package com.tripdesigner.offine.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.trip.api.vo.TripActivityVo;
import com.tripdesigner.trip.api.vo.TripDayVo;
import com.tripdesigner.trip.api.vo.TripDetailVo;
import com.tripdesigner.trip.application.TripAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 离线行程包服务。
 *
 * 将行程数据打包为 ZIP（包含 JSON 数据 + HTML 预览页 + 简易地图占位），
 * 用户在无网络环境下也可查看行程详情。
 *
 * 包结构：
 * - trip.json         行程完整数据
 * - index.html        离线可查看的 HTML 页面
 * - manifest.json     包元数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfflinePackageService {

    private final TripAppService tripAppService;
    private final ObjectMapper objectMapper;

    /** 生成离线行程包字节流 */
    public byte[] buildPackage(Long userId, Long tripId) {
        TripDetailVo detail = tripAppService.getDetailForUser(tripId, userId);
        try {
            return buildZip(detail);
        } catch (IOException e) {
            log.error("[OfflinePackage] Failed to build package for trip {}: {}", tripId, e.getMessage(), e);
            throw new BizException(ResultCode.COMMON_INTERNAL_ERROR, "生成离线包失败");
        }
    }

    /** 仅生成 JSON 数据（前端可直接下载） */
    public String buildJson(Long userId, Long tripId) {
        TripDetailVo detail = tripAppService.getDetailForUser(tripId, userId);
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(detail);
        } catch (IOException e) {
            log.error("[OfflinePackage] Failed to build JSON for trip {}: {}", tripId, e.getMessage(), e);
            throw new BizException(ResultCode.COMMON_INTERNAL_ERROR, "生成 JSON 失败");
        }
    }

    private byte[] buildZip(TripDetailVo detail) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // trip.json
            addEntry(zos, "trip.json", objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(detail));

            // manifest.json
            Map<String, Object> manifest = new HashMap<>();
            manifest.put("tripId", detail.getId());
            manifest.put("title", detail.getTitle());
            manifest.put("destination", detail.getDestinationName());
            manifest.put("exportedAt", java.time.Instant.now().toString());
            manifest.put("version", "1.0");
            manifest.put("dayCount", detail.getDays().size());
            manifest.put("activityCount", detail.getDays().stream()
                    .mapToInt(d -> d.getActivities().size()).sum());
            addEntry(zos, "manifest.json", objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(manifest));

            // index.html（离线可查看的精简 HTML）
            addEntry(zos, "index.html", buildHtml(detail));
        }
        return baos.toByteArray();
    }

    private void addEntry(ZipOutputStream zos, String name, String content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private String buildHtml(TripDetailVo detail) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        sb.append("<title>").append(escapeHtml(detail.getTitle())).append(" - 离线行程</title>");
        sb.append("<style>");
        sb.append("body{font-family:-apple-system,BlinkMacSystemFont,'PingFang SC',sans-serif;");
        sb.append("max-width:800px;margin:0 auto;padding:20px;color:#333;background:#f5f5f5;}");
        sb.append("h1{color:#1890ff;border-bottom:2px solid #1890ff;padding-bottom:8px;}");
        sb.append("h2{color:#1a1a1a;margin-top:24px;border-left:4px solid #1890ff;padding-left:10px;}");
        sb.append(".day{background:#fff;border-radius:8px;padding:16px;margin:12px 0;box-shadow:0 1px 3px rgba(0,0,0,0.06);}");
        sb.append(".activity{padding:8px 0;border-bottom:1px dashed #eee;}");
        sb.append(".activity:last-child{border-bottom:none;}");
        sb.append(".time{color:#1890ff;font-weight:600;font-size:14px;}");
        sb.append(".place{color:#666;font-size:13px;}");
        sb.append(".tag{display:inline-block;background:#e8f4ff;color:#1890ff;padding:2px 8px;border-radius:4px;font-size:12px;margin-left:6px;}");
        sb.append(".meta{color:#999;font-size:13px;margin:4px 0;}");
        sb.append("</style></head><body>");
        sb.append("<h1>").append(escapeHtml(detail.getTitle())).append("</h1>");
        sb.append("<div class=\"meta\">目的地：").append(escapeHtml(detail.getDestinationName())).append("</div>");
        sb.append("<div class=\"meta\">日期：").append(detail.getStartDate())
                .append(" ~ ").append(detail.getEndDate()).append("</div>");
        if (detail.getBudget() != null && detail.getBudget() > 0) {
            sb.append("<div class=\"meta\">预算：¥").append(detail.getBudget()).append("</div>");
        }

        for (TripDayVo day : detail.getDays()) {
            sb.append("<div class=\"day\">");
            sb.append("<h2>Day ").append(day.getDayNumber()).append(" - ")
                    .append(escapeHtml(day.getTitle() != null ? day.getTitle() : day.getDate().toString()))
                    .append("</h2>");
            sb.append("<div class=\"meta\">").append(day.getDate()).append("</div>");
            if (day.getDescription() != null && !day.getDescription().isBlank()) {
                sb.append("<p>").append(escapeHtml(day.getDescription())).append("</p>");
            }
            for (TripActivityVo activity : day.getActivities()) {
                sb.append("<div class=\"activity\">");
                sb.append("<div><strong>").append(escapeHtml(activity.getName())).append("</strong>");
                if (activity.getCategory() != null && !activity.getCategory().isBlank()) {
                    sb.append("<span class=\"tag\">").append(escapeHtml(activity.getCategory())).append("</span>");
                }
                sb.append("</div>");
                if (activity.getStartTime() != null) {
                    sb.append("<div class=\"time\">")
                            .append(activity.getStartTime())
                            .append(activity.getEndTime() != null ? " - " + activity.getEndTime() : "")
                            .append("</div>");
                }
                if (activity.getPlace() != null && !activity.getPlace().isBlank()) {
                    sb.append("<div class=\"place\">📍 ").append(escapeHtml(activity.getPlace())).append("</div>");
                }
                if (activity.getDescription() != null && !activity.getDescription().isBlank()) {
                    sb.append("<div class=\"meta\">").append(escapeHtml(activity.getDescription())).append("</div>");
                }
                sb.append("</div>");
            }
            sb.append("</div>");
        }

        sb.append("<footer style=\"text-align:center;color:#999;margin-top:24px;font-size:12px;\">");
        sb.append("由 Trip Designer 离线导出 · ").append(LocalDate.now()).append("</footer>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
