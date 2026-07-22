package com.tripdesigner.price.infrastructure;

/**
 * 价格监测仓储实现。
 * 使用 MyBatis Plus 实现持久化，负责 PriceMonitor/PriceMonitorPO 转换。
 * priceHistory 字段通过 ObjectMapper 序列化/反序列化为 JSON 字符串与 JSONB 列交互。
 * 按用户 ID 查询时按创建时间降序排列。
 */

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.price.domain.MonitorStatus;
import com.tripdesigner.price.domain.MonitorType;
import com.tripdesigner.price.domain.PriceMonitor;
import com.tripdesigner.price.domain.PriceMonitorRepository;
import com.tripdesigner.price.domain.PricePoint;
import com.tripdesigner.price.domain.TicketClass;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PriceMonitorRepositoryImpl implements PriceMonitorRepository {

    private final PriceMonitorMapper mapper;
    private final ObjectMapper objectMapper;

    private static final TypeReference<List<PricePoint>> PRICE_POINT_LIST_TYPE = new TypeReference<>() {};

    public PriceMonitorRepositoryImpl(PriceMonitorMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public PriceMonitor save(PriceMonitor monitor) {
        PriceMonitorPO po = toPO(monitor);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public Optional<PriceMonitor> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public List<PriceMonitor> findByUserId(Long userId) {
        return mapper.selectList(
                        Wrappers.<PriceMonitorPO>lambdaQuery()
                                .eq(PriceMonitorPO::getUserId, userId)
                                .orderByDesc(PriceMonitorPO::getCreatedAt))
                .stream()
                .map(this::fromPO)
                .toList();
    }

    @Override
    public List<PriceMonitor> findByStatus(MonitorStatus status) {
        return mapper.selectList(
                        Wrappers.<PriceMonitorPO>lambdaQuery()
                                .eq(PriceMonitorPO::getStatus, status.name()))
                .stream()
                .map(this::fromPO)
                .toList();
    }

    @Override
    public List<PriceMonitor> findByTripId(Long tripId) {
        return mapper.selectList(
                        Wrappers.<PriceMonitorPO>lambdaQuery()
                                .eq(PriceMonitorPO::getTripId, tripId))
                .stream()
                .map(this::fromPO)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private PriceMonitorPO toPO(PriceMonitor m) {
        PriceMonitorPO po = new PriceMonitorPO();
        po.setId(m.getId());
        po.setUserId(m.getUserId());
        po.setTripId(m.getTripId());
        po.setDestination(m.getDestination());
        po.setDeparture(m.getDeparture());
        po.setTicketClass(m.getTicketClass() != null ? m.getTicketClass().name() : null);
        po.setDepartureTime(m.getDepartureTime());
        po.setArrivalTime(m.getArrivalTime());
        po.setMonitorType(m.getMonitorType() != null ? m.getMonitorType().name() : null);
        po.setTargetPrice(m.getTargetPrice());
        po.setCurrentPrice(m.getCurrentPrice());
        po.setLowestPrice(m.getLowestPrice());
        po.setPriceHistory(serializeHistory(m.getPriceHistory()));
        po.setNotificationSent(m.isNotificationSent());
        po.setStatus(m.getStatus() != null ? m.getStatus().name() : null);
        po.setCreatedAt(m.getCreatedAt());
        po.setUpdatedAt(m.getUpdatedAt());
        po.setVersion(m.getVersion());
        return po;
    }

    private PriceMonitor fromPO(PriceMonitorPO po) {
        if (po == null) return null;
        return PriceMonitor.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .tripId(po.getTripId())
                .destination(po.getDestination())
                .departure(po.getDeparture())
                .ticketClass(parseTicketClass(po.getTicketClass()))
                .departureTime(po.getDepartureTime())
                .arrivalTime(po.getArrivalTime())
                .monitorType(parseMonitorType(po.getMonitorType()))
                .targetPrice(po.getTargetPrice())
                .currentPrice(po.getCurrentPrice())
                .lowestPrice(po.getLowestPrice())
                .priceHistory(deserializeHistory(po.getPriceHistory()))
                .notificationSent(po.getNotificationSent() != null && po.getNotificationSent())
                .status(parseMonitorStatus(po.getStatus()))
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .version(po.getVersion())
                .build();
    }

    private String serializeHistory(List<PricePoint> history) {
        if (history == null || history.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(history);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<PricePoint> deserializeHistory(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<PricePoint> list = objectMapper.readValue(json, PRICE_POINT_LIST_TYPE);
            return list != null ? list : List.of();
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private MonitorType parseMonitorType(String value) {
        if (value == null) return null;
        try {
            return MonitorType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private MonitorStatus parseMonitorStatus(String value) {
        if (value == null) return null;
        try {
            return MonitorStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private TicketClass parseTicketClass(String value) {
        if (value == null) return null;
        try {
            return TicketClass.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
