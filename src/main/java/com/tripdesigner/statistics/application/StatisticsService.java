package com.tripdesigner.statistics.application;

import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.statistics.api.vo.TripStatisticsVo;
import com.tripdesigner.trip.api.vo.TripActivityVo;
import com.tripdesigner.trip.api.vo.TripDayVo;
import com.tripdesigner.trip.api.vo.TripDetailVo;
import com.tripdesigner.trip.api.vo.TripVo;
import com.tripdesigner.trip.application.TripAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 旅行统计服务。
 *
 * 聚合用户的行程、活动、预算、目的地等数据，
 * 生成用于仪表盘可视化的统计数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final TripAppService tripAppService;

    @Transactional(readOnly = true)
    public TripStatisticsVo getStatistics() {
        UserContext ctx = requireAuth();
        List<TripVo> trips = tripAppService.listForUser(ctx.userId());

        long total = trips.size();
        long completed = trips.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
        long planning = trips.stream().filter(t -> "PLANNING".equals(t.getStatus())).count();
        long draft = trips.stream().filter(t -> "DRAFT".equals(t.getStatus())).count();

        int totalBudget = trips.stream()
                .filter(t -> t.getBudget() != null)
                .mapToInt(TripVo::getBudget)
                .sum();
        int avgBudget = total > 0 ? totalBudget / (int) total : 0;

        // 目的地统计
        Map<String, Long> destinationCount = trips.stream()
                .collect(Collectors.groupingBy(TripVo::getDestinationName, Collectors.counting()));
        List<Map<String, Object>> topDestinations = destinationCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("destination", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .toList();

        // 活动类别分布：遍历每个行程的每日活动
        Map<String, Integer> categoryCounts = new HashMap<>();
        Map<String, Integer> monthlyCounts = new TreeMap<>();
        for (TripVo tripVo : trips) {
            try {
                TripDetailVo detail = tripAppService.getDetailForUser(tripVo.getId(), ctx.userId());
                for (TripDayVo day : detail.getDays()) {
                    for (TripActivityVo activity : day.getActivities()) {
                        String category = activity.getCategory() != null && !activity.getCategory().isBlank()
                                ? activity.getCategory() : "其他";
                        categoryCounts.merge(category, 1, Integer::sum);
                    }
                }
            } catch (Exception e) {
                log.warn("[Statistics] Failed to load trip detail {}: {}", tripVo.getId(), e.getMessage());
            }

            // 月度统计
            if (tripVo.getStartDate() != null) {
                String month = tripVo.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                monthlyCounts.merge(month, 1, Integer::sum);
            }
        }

        List<Map<String, Object>> categoryDist = categoryCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("category", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .toList();

        // 行程时长分布
        Map<String, Integer> durationBuckets = new LinkedHashMap<>();
        durationBuckets.put("1-2天", 0);
        durationBuckets.put("3-5天", 0);
        durationBuckets.put("6-10天", 0);
        durationBuckets.put("10+天", 0);
        for (TripVo t : trips) {
            try {
                long days = ChronoUnit.DAYS.between(
                        t.getStartDate(),
                        t.getEndDate()) + 1;
                if (days <= 2) durationBuckets.merge("1-2天", 1, Integer::sum);
                else if (days <= 5) durationBuckets.merge("3-5天", 1, Integer::sum);
                else if (days <= 10) durationBuckets.merge("6-10天", 1, Integer::sum);
                else durationBuckets.merge("10+天", 1, Integer::sum);
            } catch (Exception ignored) {
            }
        }
        List<Map<String, Object>> durationDist = durationBuckets.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("bucket", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .toList();

        // 月度统计列表
        List<Map<String, Object>> monthlyStats = monthlyCounts.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("month", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .toList();

        // 成就徽章
        List<String> achievements = new ArrayList<>();
        if (total >= 1) achievements.add("首次出行 🚀");
        if (total >= 5) achievements.add("旅行爱好者 🌍");
        if (total >= 10) achievements.add("旅行达人 ✈️");
        if (total >= 20) achievements.add("环球旅行家 🗺️");
        if (destinationCount.size() >= 5) achievements.add("多目的地探索 🧭");
        if (destinationCount.size() >= 10) achievements.add("百城打卡 📍");
        if (completed >= 10) achievements.add("行动派 🎯");

        return TripStatisticsVo.builder()
                .userId(ctx.userId())
                .totalTrips(total)
                .completedTrips(completed)
                .planningTrips(planning)
                .draftTrips(draft)
                .totalBudget(totalBudget)
                .averageBudget(avgBudget)
                .topDestinations(topDestinations)
                .activityCategoryDistribution(categoryDist)
                .tripDurationDistribution(durationDist)
                .monthlyStats(monthlyStats)
                .achievements(achievements)
                .build();
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }
}
