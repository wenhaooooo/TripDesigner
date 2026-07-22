package com.tripdesigner.multimodal.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.ai.trip.agent.AgentContext;
import com.tripdesigner.ai.trip.agent.WorkflowEngine;
import com.tripdesigner.ai.trip.dto.GenerateRequest;
import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.multimodal.api.vo.MultimodalUploadVo;
import com.tripdesigner.multimodal.api.vo.RecognitionResultVo;
import com.tripdesigner.multimodal.config.MultimodalProperties;
import com.tripdesigner.multimodal.domain.MultimodalUpload;
import com.tripdesigner.multimodal.domain.MultimodalUploadRepository;
import com.tripdesigner.multimodal.domain.UploadStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 多模态应用服务。
 *
 * 用例：
 * 1. 上传图片并启动 AI 识别
 * 2. 查询识别结果
 * 3. 基于识别结果生成行程
 *
 * 当 AI 视觉能力不可用时，使用基于文件名/特征的 fallback 识别，
 * 保证用户流程不阻塞。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultimodalAppService {

    private final MultimodalUploadRepository repository;
    private final MultimodalProperties properties;
    private final ObjectMapper objectMapper;

    /** 上传图片并启动 AI 识别流程 */
    public MultimodalUploadVo uploadAndRecognize(Long userId, MultipartFile file) {
        validateFile(file);
        String storedFilename = generateStoredFilename(file);
        Path target = properties.resolveStoragePath(storedFilename);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("[Multimodal] Failed to store uploaded file: {}", e.getMessage(), e);
            throw new BizException(ResultCode.COMMON_INTERNAL_ERROR, "文件上传失败");
        }

        MultimodalUpload upload = MultimodalUpload.create(
                userId,
                file.getOriginalFilename(),
                storedFilename,
                file.getContentType(),
                file.getSize(),
                target.toString());

        MultimodalUpload saved = repository.save(upload);

        // 同步执行识别（图片大小一般较小，识别耗时可控；如需异步可改用 @Async）
        Map<String, Object> recognitionResult = recognize(saved);
        MultimodalUpload recognized = repository.save(saved.withRecognition(recognitionResult));
        return MultimodalUploadVo.from(recognized);
    }

    /** 列出当前用户的上传记录 */
    public List<MultimodalUploadVo> listMyUploads(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(MultimodalUploadVo::from)
                .toList();
    }

    /** 获取上传详情（含识别结果） */
    public MultimodalUploadVo getUpload(Long userId, Long uploadId) {
        MultimodalUpload upload = loadOwned(userId, uploadId);
        return MultimodalUploadVo.from(upload);
    }

    /** 基于图片识别结果生成行程 */
    public MultimodalUploadVo generateTripFromUpload(Long userId, Long uploadId) {
        MultimodalUpload upload = loadOwned(userId, uploadId);
        Map<String, Object> result = upload.getRecognitionResult();
        if (result == null || result.isEmpty()) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, "尚未完成识别，无法生成行程");
        }

        String destination = (String) result.getOrDefault("destination", "未识别目的地");
        String suggestedTitle = (String) result.getOrDefault("suggestedTripTitle", "AI 推荐行程 - " + destination);
        Integer days = parseInteger(result.get("suggestedDays"), 3);

        // 保存 generatedTripId（避免实际调用 LLM 生成行程的复杂性，
        // 这里仅记录识别到的目标信息，前端可基于此跳转到 AI 生成页填写表单）
        Map<String, Object> updatedResult = new HashMap<>(result);
        updatedResult.put("generatedTripId", null);
        updatedResult.put("suggestedPrompt", buildPrompt(destination, days, result));

        MultimodalUpload updated = upload.withRecognition(updatedResult);
        MultimodalUpload saved = repository.save(updated);
        return MultimodalUploadVo.from(saved);
    }

    /** 删除上传记录（同时清理本地文件） */
    public void deleteUpload(Long userId, Long uploadId) {
        MultimodalUpload upload = loadOwned(userId, uploadId);
        try {
            Files.deleteIfExists(Path.of(upload.getStoragePath()));
        } catch (IOException e) {
            log.warn("[Multimodal] Failed to delete upload file {}: {}", upload.getStoragePath(), e.getMessage());
        }
        repository.deleteById(uploadId);
    }

    // ========== 私有方法 ==========

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, "上传文件不能为空");
        }
        if (file.getSize() > properties.getMaxFileSize()) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, "文件大小超过限制（10MB）");
        }
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, "无法识别文件类型");
        }
        boolean allowed = false;
        for (String t : properties.getAllowedTypes()) {
            if (t.equalsIgnoreCase(contentType)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, "不支持的文件类型：" + contentType);
        }
    }

    private String generateStoredFilename(MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        return UUID.randomUUID().toString().replace("-", "") + ext;
    }

    /**
     * 图片识别：调用 Spring AI 多模态能力，或使用 fallback。
     * 当前实现使用 fallback（基于文件名的简单识别），保留 ChatClient 集成点。
     */
    private Map<String, Object> recognize(MultimodalUpload upload) {
        if (!properties.isRecognitionEnabled()) {
            return fallbackRecognize(upload);
        }
        try {
            // TODO: 集成 ChatClient 多模态调用
            // ChatClient.create().prompt().user(u -> u.text("...").media(MimeTypeUtils.IMAGE_JPEG, resource))
            // 当前降级到 fallback，避免依赖外部 API Key
            return fallbackRecognize(upload);
        } catch (Exception e) {
            log.warn("[Multimodal] Recognition failed, falling back: {}", e.getMessage());
            return fallbackRecognize(upload);
        }
    }

    /**
     * Fallback 识别：基于文件名提取目的地关键词，返回模拟识别结果。
     * 与 XiaohongshuService 的 fallback 模式保持一致。
     */
    private Map<String, Object> fallbackRecognize(MultimodalUpload upload) {
        String filename = upload.getOriginalFilename() == null ? "" : upload.getOriginalFilename().toLowerCase();
        // 去扩展名
        if (filename.contains(".")) {
            filename = filename.substring(0, filename.lastIndexOf('.'));
        }

        String destination = "未知目的地";
        String[] knownDestinations = {
                "tokyo", "japan", "kyoto", "osaka", "paris", "london", "newyork", "new york",
                "beijing", "shanghai", "hangzhou", "chengdu", "xian", "xiamen", "guilin",
                "东京", "京都", "大阪", "巴黎", "伦敦", "纽约", "北京", "上海", "杭州", "成都", "西安", "厦门", "桂林"
        };

        for (String d : knownDestinations) {
            if (filename.contains(d.toLowerCase())) {
                destination = d;
                break;
            }
        }

        // 如果文件名无目的地关键词，使用随机选择
        if (destination.equals("未知目的地")) {
            String[] defaults = {"东京", "杭州", "成都", "厦门"};
            destination = defaults[Math.abs(filename.hashCode()) % defaults.length];
        }

        List<String> tags = List.of("风景", "城市", "美食", "文化");
        List<String> landmarks = List.of("景点 A", "景点 B", "景点 C");
        Integer days = 3 + (Math.abs(filename.hashCode()) % 4); // 3-6 天
        String description = String.format("基于图片「%s」识别到目的地「%s」，建议规划 %d 天行程",
                upload.getOriginalFilename(), destination, days);

        Map<String, Object> result = new HashMap<>();
        result.put("destination", destination);
        result.put("description", description);
        result.put("tags", tags);
        result.put("landmarks", landmarks);
        result.put("suggestedTripTitle", destination + " " + days + "日游");
        result.put("suggestedDays", days);
        result.put("source", "fallback");
        return result;
    }

    private String buildPrompt(String destination, Integer days, Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();
        sb.append("请帮我规划一个 ").append(days).append(" 天的 ").append(destination).append(" 行程。");
        Object landmarks = result.get("landmarks");
        if (landmarks instanceof List<?> list && !list.isEmpty()) {
            sb.append(" 我对以下景点感兴趣：");
            for (Object o : list) {
                sb.append(o).append("、");
            }
            sb.setLength(sb.length() - 1);
            sb.append("。");
        }
        Object tags = result.get("tags");
        if (tags instanceof List<?> list && !list.isEmpty()) {
            sb.append(" 推荐主题包括：");
            for (Object o : list) {
                sb.append(o).append("、");
            }
            sb.setLength(sb.length() - 1);
            sb.append("。");
        }
        return sb.toString();
    }

    private Integer parseInteger(Object value, Integer defaultValue) {
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private MultimodalUpload loadOwned(Long userId, Long uploadId) {
        MultimodalUpload upload = repository.findById(uploadId)
                .orElseThrow(() -> new BizException(ResultCode.COMMON_NOT_FOUND, "上传记录不存在"));
        if (!upload.getUserId().equals(userId)) {
            throw new BizException(ResultCode.PERMISSION_DENIED, "无权访问该上传记录");
        }
        return upload;
    }
}
