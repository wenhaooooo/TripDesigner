package com.tripdesigner.price.application;

import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.price.api.dto.CreateMonitorRequest;
import com.tripdesigner.price.api.vo.PriceMonitorVo;
import com.tripdesigner.price.domain.MonitorStatus;
import com.tripdesigner.price.domain.MonitorType;
import com.tripdesigner.price.domain.PriceMonitor;
import com.tripdesigner.price.domain.PriceMonitorRepository;
import com.tripdesigner.price.domain.TicketClass;
import com.tripdesigner.price.domain.TrainTicketInfo;
import com.tripdesigner.price.domain.TrainTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * 价格监测应用服务。
 *
 * 编排价格监测的创建、查询、取消、删除等业务用例，
 * 并提供定时任务调用的 checkAndNotifyPriceDrop() 方法。
 *
 * 所有面向用户的操作均要求认证且只能操作当前用户的数据。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PriceMonitorAppService {

    private final PriceMonitorRepository repository;
    private final PriceMonitorService priceService;
    private final TrainTicketRepository trainTicketRepository;

    private static final String PRICE_SOURCE = "simulated";

    @Transactional
    public PriceMonitorVo createMonitor(CreateMonitorRequest req) {
        UserContext ctx = requireAuth();
        MonitorType type = parseMonitorType(req.getMonitorType());
        TicketClass ticketClass = parseTicketClass(req.getTicketClass());

        LocalTime departureTime = req.getDepartureTime();
        LocalTime arrivalTime = req.getArrivalTime();

        if (type == MonitorType.TRAIN && req.getDeparture() != null && !req.getDeparture().isBlank()) {
            TrainTicketInfo matchedTicket = findMatchedTrainTicket(
                    req.getDeparture(), req.getDestination(), ticketClass);
            if (matchedTicket != null) {
                if (departureTime == null) {
                    departureTime = matchedTicket.getDepartureTime();
                }
                if (arrivalTime == null) {
                    arrivalTime = matchedTicket.getArrivalTime();
                }
                if (ticketClass == null) {
                    ticketClass = matchedTicket.getTicketClass();
                }
            }
        }

        PriceMonitor monitor = PriceMonitor.create(
                ctx.userId(),
                req.getTripId(),
                req.getDestination(),
                req.getDeparture(),
                type,
                ticketClass,
                departureTime,
                arrivalTime,
                req.getTargetPrice()
        );
        PriceMonitor saved = repository.save(monitor);
        return PriceMonitorVo.from(saved);
    }

    private TrainTicketInfo findMatchedTrainTicket(String departure, String destination, TicketClass ticketClass) {
        try {
            List<TrainTicketInfo> tickets;
            if (ticketClass != null) {
                tickets = trainTicketRepository.findByRouteAndClass(departure, destination, ticketClass);
            } else {
                tickets = trainTicketRepository.findByRoute(departure, destination);
            }
            if (!tickets.isEmpty()) {
                return tickets.get(0);
            }
        } catch (Exception e) {
            log.warn("[PriceMonitorAppService] Failed to find matched train ticket: {}", e.getMessage());
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<PriceMonitorVo> listMyMonitors() {
        UserContext ctx = requireAuth();
        return repository.findByUserId(ctx.userId()).stream()
                .map(PriceMonitorVo::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PriceMonitorVo getMonitor(Long id) {
        UserContext ctx = requireAuth();
        PriceMonitor monitor = loadOwned(id, ctx.userId());
        return PriceMonitorVo.from(monitor);
    }

    @Transactional
    public PriceMonitorVo cancelMonitor(Long id) {
        UserContext ctx = requireAuth();
        PriceMonitor monitor = loadOwned(id, ctx.userId());
        PriceMonitor cancelled = monitor.withStatus(MonitorStatus.CANCELLED);
        PriceMonitor saved = repository.save(cancelled);
        return PriceMonitorVo.from(saved);
    }

    @Transactional
    public void deleteMonitor(Long id) {
        UserContext ctx = requireAuth();
        loadOwned(id, ctx.userId());
        repository.deleteById(id);
    }

    /**
     * 检查所有 ACTIVE 监测的价格变化，并在达到目标价格时触发通知。
     * 由定时任务 PriceMonitorScheduler 每小时调用一次。
     *
     * @return 本次触发的监测数量
     */
    @Transactional
    public int checkAndNotifyPriceDrop() {
        List<PriceMonitor> actives = repository.findByStatus(MonitorStatus.ACTIVE);
        int triggered = 0;
        for (PriceMonitor monitor : actives) {
            try {
                BigDecimal currentPrice = priceService.fetchCurrentPrice(
                        monitor.getDeparture(), monitor.getDestination(), 
                        monitor.getMonitorType(), monitor.getTicketClass());
                PriceMonitor updated = monitor.withUpdatedPrice(currentPrice, PRICE_SOURCE);
                if (updated.shouldNotify()) {
                    updated = markNotified(updated);
                    triggered++;
                    log.info("[PriceMonitor] Triggered: monitorId={} destination={} currentPrice={} targetPrice={}",
                            updated.getId(), updated.getDestination(),
                            updated.getCurrentPrice(), updated.getTargetPrice());
                }
                repository.save(updated);
            } catch (Exception e) {
                log.warn("[PriceMonitor] Check failed for monitorId={}: {}",
                        monitor.getId(), e.getMessage());
            }
        }
        if (triggered > 0) {
            log.info("[PriceMonitor] Price drop notifications sent: count={}", triggered);
        }
        return triggered;
    }

    /**
     * 标记为已通知并切换到 TRIGGERED 状态。
     * 通过 withStatus 切换状态，并将 notificationSent 置 true；
     * 因 PriceMonitor 不可变，这里重建一个带新标志的实例。
     */
    private PriceMonitor markNotified(PriceMonitor monitor) {
        PriceMonitor triggered = monitor.withStatus(MonitorStatus.TRIGGERED);
        return PriceMonitor.builder()
                .id(triggered.getId())
                .userId(triggered.getUserId())
                .tripId(triggered.getTripId())
                .destination(triggered.getDestination())
                .departure(triggered.getDeparture())
                .ticketClass(triggered.getTicketClass())
                .departureTime(triggered.getDepartureTime())
                .arrivalTime(triggered.getArrivalTime())
                .monitorType(triggered.getMonitorType())
                .targetPrice(triggered.getTargetPrice())
                .currentPrice(triggered.getCurrentPrice())
                .lowestPrice(triggered.getLowestPrice())
                .priceHistory(triggered.getPriceHistory())
                .notificationSent(true)
                .status(triggered.getStatus())
                .createdAt(triggered.getCreatedAt())
                .updatedAt(triggered.getUpdatedAt())
                .version(triggered.getVersion())
                .build();
    }

    private PriceMonitor loadOwned(Long id, Long currentUserId) {
        PriceMonitor monitor = repository.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.COMMON_NOT_FOUND, "price monitor not found"));
        if (!monitor.getUserId().equals(currentUserId)) {
            throw new BizException(ResultCode.PERMISSION_DENIED, "price monitor does not belong to user");
        }
        return monitor;
    }

    private MonitorType parseMonitorType(String value) {
        if (value == null || value.isBlank()) return MonitorType.FLIGHT;
        try {
            return MonitorType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, "invalid monitorType: " + value);
        }
    }

    private TicketClass parseTicketClass(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return TicketClass.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, "invalid ticketClass: " + value);
        }
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }
}
