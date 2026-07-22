package com.tripdesigner.booking.application;

import com.tripdesigner.booking.api.vo.BookingLinkVo;
import com.tripdesigner.trip.application.TripAppService;
import com.tripdesigner.trip.api.vo.TripActivityVo;
import com.tripdesigner.trip.api.vo.TripDayVo;
import com.tripdesigner.trip.api.vo.TripDetailVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 在线预订集成服务。
 *
 * 不调用第三方预订 API（这需要平台授权），
 * 而是基于行程活动信息（地点、类别、目的地）生成预订平台的搜索/列表 URL，
 * 前端可直接打开新标签页跳转到第三方平台。
 *
 * 支持平台：
 * - 携程（机票/酒店/景点门票）
 * - 飞猪（机票/酒店）
 * - 美团（餐厅/门票）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final TripAppService tripAppService;

    /** 生成单个活动的预订链接列表 */
    public List<BookingLinkVo> generateLinksForActivity(Long userId, Long tripId, Long activityId) {
        TripDetailVo detail = tripAppService.getDetailForUser(tripId, userId);
        TripActivityVo activity = detail.getDays().stream()
                .flatMap(d -> d.getActivities().stream())
                .filter(a -> a.getId().equals(activityId))
                .findFirst()
                .orElse(null);
        if (activity == null) {
            return List.of();
        }
        String destination = detail.getDestinationName();
        return buildLinks(destination, activity);
    }

    /** 生成整个行程的预订建议 */
    public Map<String, List<BookingLinkVo.Suggestion>> generateSuggestionsForTrip(Long userId, Long tripId) {
        TripDetailVo detail = tripAppService.getDetailForUser(tripId, userId);
        String destination = detail.getDestinationName();

        // 按天分组生成建议
        return detail.getDays().stream()
                .collect(Collectors.toMap(
                        d -> "Day " + d.getDayNumber(),
                        d -> d.getActivities().stream()
                                .map(a -> new BookingLinkVo.Suggestion(
                                        a.getName(),
                                        a.getCategory(),
                                        buildLinks(destination, a)))
                                .filter(s -> !s.links().isEmpty())
                                .toList()));
    }

    private List<BookingLinkVo> buildLinks(String destination, TripActivityVo activity) {
        List<BookingLinkVo> links = new ArrayList<>();
        String category = activity.getCategory() == null ? "" : activity.getCategory().toLowerCase();
        String place = activity.getPlace() != null && !activity.getPlace().isBlank()
                ? activity.getPlace() : activity.getName();

        // 按类别决定推荐平台
        if (category.contains("酒店") || category.contains("hotel") || category.contains("住宿")) {
            links.add(buildCtripLink("hotel", destination, place));
            links.add(buildFliggyLink("hotel", destination, place));
        } else if (category.contains("机票") || category.contains("flight") || category.contains("交通")) {
            links.add(buildCtripLink("flight", destination, place));
            links.add(buildFliggyLink("flight", destination, place));
        } else if (category.contains("餐厅") || category.contains("美食") || category.contains("dining")) {
            links.add(buildMeituanLink(destination, place));
        } else if (category.contains("景点") || category.contains("attraction") || category.contains("sight")) {
            links.add(buildCtripLink("ticket", destination, place));
            links.add(buildMeituanLink(destination, place));
        } else {
            // 默认推荐所有平台
            links.add(buildCtripLink("search", destination, place));
            links.add(buildMeituanLink(destination, place));
        }
        return links;
    }

    private BookingLinkVo buildCtripLink(String type, String destination, String keyword) {
        String url;
        String description;
        switch (type) {
            case "hotel":
                url = "https://hotels.ctrip.com/hotels/list?countryId=1&city=" + encode(destination);
                description = "查看携程" + destination + "酒店列表";
                break;
            case "flight":
                url = "https://flights.ctrip.com/online/list/oneway-" + encode(destination);
                description = "查询飞往" + destination + "的机票";
                break;
            case "ticket":
                url = "https://piao.ctrip.com/piaoweb/search?keyword=" + encode(keyword) + "&city=" + encode(destination);
                description = "查询" + destination + "景点门票：" + keyword;
                break;
            default:
                url = "https://www.ctrip.com/?allianceid=4897&sid=784026&keyword=" + encode(keyword);
                description = "在携程搜索：" + keyword;
        }
        return BookingLinkVo.of("ctrip", "携程", url, description);
    }

    private BookingLinkVo buildFliggyLink(String type, String destination, String keyword) {
        String url;
        String description;
        if ("hotel".equals(type)) {
            url = "https://hotel.fliggy.com/list.htm?city=" + encode(destination);
            description = "查看飞猪" + destination + "酒店";
        } else if ("flight".equals(type)) {
            url = "https://www.fliggy.com/flight/domestic-search?depCity=&arrCity=" + encode(destination);
            description = "查询飞猪飞往" + destination + "的机票";
        } else {
            url = "https://www.fliggy.com/?q=" + encode(keyword);
            description = "在飞猪搜索：" + keyword;
        }
        return BookingLinkVo.of("fliggy", "飞猪", url, description);
    }

    private BookingLinkVo buildMeituanLink(String destination, String keyword) {
        String url = "https://www.meituan.com/s/" + encode(destination + " " + keyword);
        return BookingLinkVo.of("meituan", "美团", url, "在美团搜索：" + destination + " " + keyword);
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
