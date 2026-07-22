package com.tripdesigner.multimodal.api;

import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.Result;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.multimodal.api.vo.MultimodalUploadVo;
import com.tripdesigner.multimodal.application.MultimodalAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 多模态上传 REST API。
 *
 * 端点：
 * - POST /multimodal/upload        上传图片并执行识别
 * - GET  /multimodal               列出我的上传
 * - GET  /multimodal/{id}          查看上传详情
 * - POST /multimodal/{id}/generate 基于图片生成行程建议
 * - DELETE /multimodal/{id}        删除上传
 */
@Slf4j
@Tag(name = "Multimodal", description = "多模态行程生成接口")
@RestController
@RequestMapping("/multimodal")
@RequiredArgsConstructor
public class MultimodalController {

    private final MultimodalAppService multimodalAppService;

    @Operation(summary = "上传图片并启动 AI 识别")
    @PostMapping("/upload")
    public Result<MultimodalUploadVo> upload(@RequestParam("file") MultipartFile file) {
        UserContext ctx = requireAuth();
        return Result.success(multimodalAppService.uploadAndRecognize(ctx.userId(), file));
    }

    @Operation(summary = "列出我的上传记录")
    @GetMapping
    public Result<List<MultimodalUploadVo>> list() {
        UserContext ctx = requireAuth();
        return Result.success(multimodalAppService.listMyUploads(ctx.userId()));
    }

    @Operation(summary = "查看上传详情（含识别结果）")
    @GetMapping("/{id}")
    public Result<MultimodalUploadVo> get(@PathVariable Long id) {
        UserContext ctx = requireAuth();
        return Result.success(multimodalAppService.getUpload(ctx.userId(), id));
    }

    @Operation(summary = "基于图片识别结果生成行程建议")
    @PostMapping("/{id}/generate")
    public Result<MultimodalUploadVo> generateTrip(@PathVariable Long id) {
        UserContext ctx = requireAuth();
        return Result.success(multimodalAppService.generateTripFromUpload(ctx.userId(), id));
    }

    @Operation(summary = "删除上传记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        UserContext ctx = requireAuth();
        multimodalAppService.deleteUpload(ctx.userId(), id);
        return Result.success();
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }
}
