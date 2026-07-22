package com.tripdesigner.offine.api;

import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.offine.application.OfflinePackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 离线行程包导出 REST API。
 *
 * - GET /offline/trips/{id}/package   下载 ZIP 离线包
 * - GET /offline/trips/{id}/json       下载 JSON 数据
 */
@Slf4j
@Tag(name = "Offline", description = "离线行程包导出接口")
@RestController
@RequestMapping("/offline/trips")
@RequiredArgsConstructor
public class OfflinePackageController {

    private final OfflinePackageService offlinePackageService;

    @Operation(summary = "下载离线行程包 ZIP", description = "包含 JSON 数据、HTML 离线查看页和元数据")
    @GetMapping("/{id}/package")
    public ResponseEntity<byte[]> downloadPackage(@PathVariable Long id) {
        UserContext ctx = requireAuth();
        byte[] data = offlinePackageService.buildPackage(ctx.userId(), id);
        return buildAttachmentResponse(data, "trip-" + id + "-offline.zip",
                MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    @Operation(summary = "下载行程 JSON 数据")
    @GetMapping("/{id}/json")
    public ResponseEntity<byte[]> downloadJson(@PathVariable Long id) {
        UserContext ctx = requireAuth();
        String json = offlinePackageService.buildJson(ctx.userId(), id);
        return buildAttachmentResponse(json.getBytes(StandardCharsets.UTF_8),
                "trip-" + id + ".json", MediaType.APPLICATION_JSON_VALUE);
    }

    private ResponseEntity<byte[]> buildAttachmentResponse(byte[] data, String filename, String contentType) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded);
        headers.set(HttpHeaders.CONTENT_TYPE, contentType + ";charset=UTF-8");
        headers.setContentLength(data.length);
        return new ResponseEntity<>(data, headers, org.springframework.http.HttpStatus.OK);
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }
}
