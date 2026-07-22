package com.tripdesigner.price.application;

import com.tripdesigner.price.domain.MonitorType;
import com.tripdesigner.price.domain.TicketClass;
import com.tripdesigner.price.domain.TrainTicketInfo;
import com.tripdesigner.price.domain.TrainTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceMonitorService {

    private final StringRedisTemplate redisTemplate;
    private final TrainTicketRepository trainTicketRepository;

    private static final String PRICE_CACHE_KEY_PREFIX = "price:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private static final BigDecimal FLIGHT_BASE = new BigDecimal("1200.00");
    private static final BigDecimal HOTEL_BASE = new BigDecimal("500.00");
    private static final BigDecimal TRAIN_BASE = new BigDecimal("300.00");

    private static final double FLUCTUATION_RATIO = 0.2;

    public BigDecimal fetchCurrentPrice(String departure, String destination, MonitorType type, TicketClass ticketClass) {
        String cacheKey = buildCacheKey(departure, destination, type, ticketClass);
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return new BigDecimal(cached);
            }
        } catch (Exception e) {
            log.warn("[PriceMonitorService] Cache read failed for {}: {}", cacheKey, e.getMessage());
        }

        BigDecimal price = calculatePrice(type, ticketClass, departure, destination);
        try {
            redisTemplate.opsForValue().set(cacheKey, price.toPlainString(), CACHE_TTL);
        } catch (Exception e) {
            log.warn("[PriceMonitorService] Cache write failed for {}: {}", cacheKey, e.getMessage());
        }
        return price;
    }

    public BigDecimal fetchCurrentPrice(String departure, String destination, MonitorType type) {
        return fetchCurrentPrice(departure, destination, type, null);
    }

    public BigDecimal fetchCurrentPrice(String destination, MonitorType type) {
        return fetchCurrentPrice(null, destination, type);
    }

    public List<TrainTicketInfo> findTrainTickets(String departure, String destination) {
        return trainTicketRepository.findByRoute(departure, destination);
    }

    public List<TrainTicketInfo> findTrainTickets(String departure, String destination, TicketClass ticketClass) {
        if (ticketClass != null) {
            return trainTicketRepository.findByRouteAndClass(departure, destination, ticketClass);
        }
        return trainTicketRepository.findByRoute(departure, destination);
    }

    private BigDecimal calculatePrice(MonitorType type, TicketClass ticketClass, String departure, String destination) {
        if (type == MonitorType.TRAIN && departure != null && !departure.isBlank()) {
            BigDecimal realPrice = findRealTrainPrice(departure, destination, ticketClass);
            if (realPrice != null) {
                return applyFluctuation(realPrice);
            }
        }
        return simulatePrice(type, ticketClass, departure, destination);
    }

    private BigDecimal findRealTrainPrice(String departure, String destination, TicketClass ticketClass) {
        try {
            List<TrainTicketInfo> tickets;
            if (ticketClass != null) {
                tickets = trainTicketRepository.findByRouteAndClass(departure, destination, ticketClass);
            } else {
                tickets = trainTicketRepository.findByRoute(departure, destination);
            }
            if (!tickets.isEmpty()) {
                TrainTicketInfo first = tickets.get(0);
                log.debug("[PriceMonitorService] Found real train price for {}->{} {}: {}", 
                        departure, destination, ticketClass, first.getPrice());
                return first.getPrice();
            }
        } catch (Exception e) {
            log.warn("[PriceMonitorService] Failed to query real train prices: {}", e.getMessage());
        }
        return null;
    }

    private BigDecimal applyFluctuation(BigDecimal base) {
        double fluctuation = ThreadLocalRandom.current().nextDouble(-FLUCTUATION_RATIO, FLUCTUATION_RATIO);
        BigDecimal fluctuationAmount = base.multiply(BigDecimal.valueOf(fluctuation))
                .setScale(2, RoundingMode.HALF_UP);
        return base.add(fluctuationAmount).max(BigDecimal.ONE)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal simulatePrice(MonitorType type, TicketClass ticketClass, String departure, String destination) {
        BigDecimal base = switch (type) {
            case FLIGHT -> FLIGHT_BASE;
            case HOTEL -> HOTEL_BASE;
            case TRAIN -> TRAIN_BASE;
        };

        BigDecimal classMultiplier = calculateClassMultiplier(type, ticketClass);
        base = base.multiply(classMultiplier);

        int depLen = departure == null ? 0 : departure.length();
        int destLen = destination == null ? 0 : destination.length();
        BigDecimal routeOffset = BigDecimal.valueOf((depLen + destLen) * 5L);
        return applyFluctuation(base.add(routeOffset));
    }

    private BigDecimal calculateClassMultiplier(MonitorType type, TicketClass ticketClass) {
        if (ticketClass == null) {
            return BigDecimal.ONE;
        }
        return switch (ticketClass) {
            case STANDING -> new BigDecimal("0.7");
            case HARD_SEAT -> new BigDecimal("0.8");
            case HARD_BERTH -> new BigDecimal("1.0");
            case SOFT_BERTH -> new BigDecimal("1.5");
            case SECOND_CLASS -> new BigDecimal("1.0");
            case FIRST_CLASS -> new BigDecimal("1.6");
            case BUSINESS_CLASS -> new BigDecimal("3.0");
            case ECONOMY -> new BigDecimal("1.0");
            case BUSINESS -> new BigDecimal("2.5");
            case FIRST -> new BigDecimal("4.0");
        };
    }

    private String buildCacheKey(String departure, String destination, MonitorType type, TicketClass ticketClass) {
        String safeDep = departure == null ? "" : departure.trim().toLowerCase();
        String safeDest = destination == null ? "unknown" : destination.trim().toLowerCase();
        String route = safeDep.isEmpty() ? safeDest : (safeDep + "-" + safeDest);
        String classPart = ticketClass != null ? ":" + ticketClass.name() : "";
        return PRICE_CACHE_KEY_PREFIX + route + ":" + (type != null ? type.name() : "UNKNOWN") + classPart;
    }
}