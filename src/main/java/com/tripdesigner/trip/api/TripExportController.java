package com.tripdesigner.trip.api;

import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.trip.application.TripExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Tag(name = "Trip Export", description = "行程导出接口")
@RestController
@RequestMapping("/trips/{id}/export")
@RequiredArgsConstructor
public class TripExportController {

    private final TripExportService tripExportService;

    @Operation(summary = "导出行程为 Excel", description = "导出行程详情为 .xlsx 格式，包含行程概览和每日行程两个工作表")
    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportExcel(
            @Parameter(description = "行程ID") @PathVariable Long id) {
        UserContext ctx = requireAuth();
        byte[] data = tripExportService.exportToExcel(ctx.userId(), id);
        return buildAttachmentResponse(data, "trip-" + id + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Operation(summary = "导出行程为 CSV", description = "导出行程详情为 CSV 格式，包含行程概览和每日行程")
    @GetMapping("/csv")
    public ResponseEntity<byte[]> exportCsv(
            @Parameter(description = "行程ID") @PathVariable Long id) {
        UserContext ctx = requireAuth();
        byte[] data = tripExportService.exportToCsv(ctx.userId(), id);
        return buildAttachmentResponse(data, "trip-" + id + ".csv",
                "text/csv");
    }

    @Operation(summary = "导出行程为 ICS", description = "导出行程为日历 ICS 格式，可导入日历应用（如日历、Google Calendar）")
    @GetMapping("/ics")
    public ResponseEntity<byte[]> exportIcs(
            @Parameter(description = "行程ID") @PathVariable Long id) {
        UserContext ctx = requireAuth();
        byte[] data = tripExportService.exportToIcs(ctx.userId(), id);
        return buildAttachmentResponse(data, "trip-" + id + ".ics",
                "text/calendar");
    }

    @Operation(summary = "导出行程为 JSON", description = "导出行程详情为 JSON 格式，包含完整数据结构")
    @GetMapping("/json")
    public ResponseEntity<byte[]> exportJson(
            @Parameter(description = "行程ID") @PathVariable Long id) {
        UserContext ctx = requireAuth();
        byte[] data = tripExportService.exportToJson(ctx.userId(), id);
        return buildAttachmentResponse(data, "trip-" + id + ".json",
                "application/json");
    }

    private ResponseEntity<byte[]> buildAttachmentResponse(byte[] data, String filename, String contentType) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded);
        headers.set(HttpHeaders.CONTENT_TYPE, contentType + ";charset=UTF-8");
        headers.setContentLength(data.length);
        return ResponseEntity.ok().headers(headers).body(data);
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }
}
