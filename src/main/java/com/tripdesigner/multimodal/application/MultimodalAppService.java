package com.tripdesigner.multimodal.application;

import com.fasterxml.jackson.databind.JsonNode;
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
import com.tripdesigner.trip.application.TripAppService;
import com.tripdesigner.trip.api.vo.TripVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
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
    private final ChatClient chatClient;
    private final TripAppService tripAppService;

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

        // 根据识别结果创建行程
        LocalDate startDate = LocalDate.now().plusDays(7);
        LocalDate endDate = startDate.plusDays(days - 1);
        String title = suggestedTitle;
        String description = (String) result.getOrDefault("description", "");
        Integer budget = days * 1000;

        TripVo trip = tripAppService.createForUser(
                userId, title, description, destination, startDate, endDate, budget);

        Long generatedTripId = trip.getId();

        // 为每一天创建空 Day
        for (int d = 1; d <= days; d++) {
            tripAppService.findOrCreateDayForTrip(generatedTripId, userId, d);
        }

        Map<String, Object> updatedResult = new HashMap<>(result);
        updatedResult.put("generatedTripId", generatedTripId);
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
     */
    private Map<String, Object> recognize(MultimodalUpload upload) {
        if (!properties.isRecognitionEnabled()) {
            return fallbackRecognize(upload);
        }
        try {
            return aiRecognize(upload);
        } catch (Exception e) {
            log.warn("[Multimodal] AI recognition failed, falling back: {}", e.getMessage());
            return fallbackRecognize(upload);
        }
    }

    /**
     * 使用 AI 多模态模型进行视觉识别。
     */
    private Map<String, Object> aiRecognize(MultimodalUpload upload) {
        log.info("[Multimodal] Starting AI recognition for: {}", upload.getOriginalFilename());
        
        String systemPrompt = """
                你是一个旅行目的地识别专家。请分析这张图片，识别其中的地点或地标。
                
                请输出以下信息（JSON格式）：
                {
                    "destination": "识别到的目的地城市或国家名称，中文输出",
                    "description": "图片内容描述",
                    "tags": ["标签1", "标签2", "标签3"],
                    "landmarks": ["地标1", "地标2", "地标3"],
                    "suggestedDays": 建议的旅行天数（3-7天）
                }
                
                注意：
                - destination 必须是有效的旅行目的地城市名（中文）
                - 如果无法识别具体地点，返回最可能的城市
                - 如果完全无法识别，返回 "未知目的地"
                """;

        String userPrompt = "请分析这张图片，识别旅行目的地。";

        FileSystemResource imageResource = new FileSystemResource(upload.getStoragePath());
        
        try {
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(u -> u.text(userPrompt).media(MimeTypeUtils.IMAGE_JPEG, imageResource))
                    .call()
                    .content();

            log.info("[Multimodal] AI recognition response: {}", response);

            Map<String, Object> result = parseAiResponse(response);
            
            if (result == null || result.isEmpty()) {
                throw new RuntimeException("AI response parsing failed");
            }

            String destination = (String) result.getOrDefault("destination", "未知目的地");
            if ("未知目的地".equals(destination) || destination.isBlank()) {
                return fallbackRecognize(upload);
            }

            result.put("source", "ai");
            result.put("suggestedTripTitle", destination + " " + 
                    result.getOrDefault("suggestedDays", 3) + "日游");
            
            return result;
        } catch (Exception e) {
            log.warn("[Multimodal] AI recognition error: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 解析 AI 返回的 JSON 响应。
     */
    private Map<String, Object> parseAiResponse(String response) {
        try {
            String jsonStr = response.trim();
            if (jsonStr.contains("```")) {
                int start = jsonStr.indexOf("```") + 3;
                int end = jsonStr.lastIndexOf("```");
                if (end > start) {
                    jsonStr = jsonStr.substring(start, end).trim();
                }
            }
            // 剥离 ```json 中的 "json" 语言标识
            if (jsonStr.startsWith("json")) {
                jsonStr = jsonStr.substring(4).trim();
            }
            
            JsonNode node = objectMapper.readTree(jsonStr);
            Map<String, Object> result = new HashMap<>();
            
            result.put("destination", node.path("destination").asText("未知目的地"));
            result.put("description", node.path("description").asText(""));
            
            List<String> tags = new ArrayList<>();
            JsonNode tagsNode = node.path("tags");
            if (tagsNode.isArray()) {
                for (JsonNode tag : tagsNode) {
                    tags.add(tag.asText());
                }
            }
            result.put("tags", tags.isEmpty() ? List.of("风景", "城市") : tags);
            
            List<String> landmarks = new ArrayList<>();
            JsonNode landmarksNode = node.path("landmarks");
            if (landmarksNode.isArray()) {
                for (JsonNode landmark : landmarksNode) {
                    landmarks.add(landmark.asText());
                }
            }
            result.put("landmarks", landmarks.isEmpty() ? List.of("景点 A", "景点 B") : landmarks);
            
            result.put("suggestedDays", node.path("suggestedDays").asInt(3));
            
            return result;
        } catch (Exception e) {
            log.warn("[Multimodal] Failed to parse AI response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fallback 识别：基于文件名提取目的地关键词，返回模拟识别结果。
     * 与 XiaohongshuService 的 fallback 模式保持一致。
     */
    private Map<String, Object> fallbackRecognize(MultimodalUpload upload) {
        String filename = upload.getOriginalFilename() == null ? "" : upload.getOriginalFilename().toLowerCase();
        if (filename.contains(".")) {
            filename = filename.substring(0, filename.lastIndexOf('.'));
        }

        String destination = "未知目的地";
        
        Map<String, String> landmarkToDestination = new HashMap<>();
        landmarkToDestination.put("大本钟", "伦敦");
        landmarkToDestination.put("bigben", "伦敦");
        landmarkToDestination.put("big ben", "伦敦");
        landmarkToDestination.put("埃菲尔", "巴黎");
        landmarkToDestination.put("eiffel", "巴黎");
        landmarkToDestination.put("自由女神", "纽约");
        landmarkToDestination.put("statue of liberty", "纽约");
        landmarkToDestination.put("泰姬陵", "新德里");
        landmarkToDestination.put("taj mahal", "新德里");
        landmarkToDestination.put("富士山", "东京");
        landmarkToDestination.put("fuji", "东京");
        landmarkToDestination.put("长城", "北京");
        landmarkToDestination.put("great wall", "北京");
        landmarkToDestination.put("兵马俑", "西安");
        landmarkToDestination.put("terracotta", "西安");
        landmarkToDestination.put("西湖", "杭州");
        landmarkToDestination.put("west lake", "杭州");
        landmarkToDestination.put("黄鹤楼", "武汉");
        landmarkToDestination.put("东方明珠", "上海");
        landmarkToDestination.put("东方明珠塔", "上海");
        landmarkToDestination.put("bund", "上海");
        landmarkToDestination.put("外滩", "上海");
        landmarkToDestination.put("故宫", "北京");
        landmarkToDestination.put("forbidden city", "北京");
        landmarkToDestination.put("天坛", "北京");
        landmarkToDestination.put("temple of heaven", "北京");

        for (Map.Entry<String, String> entry : landmarkToDestination.entrySet()) {
            if (filename.contains(entry.getKey().toLowerCase())) {
                destination = entry.getValue();
                break;
            }
        }

        if (destination.equals("未知目的地")) {
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
        }

        if (destination.equals("未知目的地")) {
            String[] defaults = {"东京", "杭州", "成都", "厦门"};
            destination = defaults[Math.abs(filename.hashCode()) % defaults.length];
        }

        Map<String, String> destinationLandmarks = new HashMap<>();
        destinationLandmarks.put("伦敦", "大本钟、伦敦塔、大英博物馆");
        destinationLandmarks.put("巴黎", "埃菲尔铁塔、卢浮宫、凯旋门");
        destinationLandmarks.put("纽约", "自由女神像、时代广场、帝国大厦");
        destinationLandmarks.put("东京", "富士山、东京塔、浅草寺");
        destinationLandmarks.put("北京", "故宫、长城、颐和园");
        destinationLandmarks.put("上海", "外滩、东方明珠塔、豫园");
        destinationLandmarks.put("杭州", "西湖、灵隐寺、雷峰塔");
        destinationLandmarks.put("西安", "兵马俑、大雁塔、城墙");
        destinationLandmarks.put("新德里", "泰姬陵、红堡、莲花寺");
        destinationLandmarks.put("武汉", "黄鹤楼、东湖、长江大桥");

        String landmarksStr = destinationLandmarks.getOrDefault(destination, "景点 A、景点 B、景点 C");
        List<String> landmarks = List.of(landmarksStr.split("、"));
        
        Map<String, String> destinationTags = new HashMap<>();
        destinationTags.put("伦敦", "文化、历史、博物馆");
        destinationTags.put("巴黎", "浪漫、艺术、美食");
        destinationTags.put("纽约", "现代、购物、夜景");
        destinationTags.put("东京", "传统、现代、美食");
        destinationTags.put("北京", "历史、文化、古迹");
        destinationTags.put("上海", "现代、购物、美食");
        destinationTags.put("杭州", "风景、休闲、文化");
        destinationTags.put("西安", "历史、古迹、文化");
        destinationTags.put("新德里", "历史、宗教、文化");
        destinationTags.put("武汉", "历史、风景、美食");

        String tagsStr = destinationTags.getOrDefault(destination, "风景、城市、美食、文化");
        List<String> tags = List.of(tagsStr.split("、"));

        Integer days = 3 + (Math.abs(filename.hashCode()) % 4);
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
