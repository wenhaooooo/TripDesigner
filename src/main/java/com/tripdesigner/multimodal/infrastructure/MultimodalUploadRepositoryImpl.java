package com.tripdesigner.multimodal.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.multimodal.domain.MultimodalUpload;
import com.tripdesigner.multimodal.domain.MultimodalUploadRepository;
import com.tripdesigner.multimodal.domain.UploadStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 多模态上传仓储实现。
 * 使用 MyBatis Plus 持久化，recognitionResult 通过 ObjectMapper 序列化为 JSON 字符串与 JSONB 交互。
 */
@Repository
public class MultimodalUploadRepositoryImpl implements MultimodalUploadRepository {

    private final MultimodalUploadMapper mapper;
    private final ObjectMapper objectMapper;

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    public MultimodalUploadRepositoryImpl(MultimodalUploadMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public MultimodalUpload save(MultimodalUpload upload) {
        MultimodalUploadPO po = toPO(upload);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public Optional<MultimodalUpload> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public List<MultimodalUpload> findByUserId(Long userId) {
        return mapper.selectList(
                        Wrappers.<MultimodalUploadPO>lambdaQuery()
                                .eq(MultimodalUploadPO::getUserId, userId)
                                .orderByDesc(MultimodalUploadPO::getCreatedAt))
                .stream()
                .map(this::fromPO)
                .toList();
    }

    @Override
    public List<MultimodalUpload> findByGeneratedTripId(Long tripId) {
        return mapper.selectList(
                        Wrappers.<MultimodalUploadPO>lambdaQuery()
                                .eq(MultimodalUploadPO::getGeneratedTripId, tripId))
                .stream()
                .map(this::fromPO)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private MultimodalUploadPO toPO(MultimodalUpload u) {
        MultimodalUploadPO po = new MultimodalUploadPO();
        po.setId(u.getId());
        po.setUserId(u.getUserId());
        po.setOriginalFilename(u.getOriginalFilename());
        po.setStoredFilename(u.getStoredFilename());
        po.setContentType(u.getContentType());
        po.setFileSize(u.getFileSize());
        po.setStoragePath(u.getStoragePath());
        po.setRecognitionResult(serializeMap(u.getRecognitionResult()));
        po.setGeneratedTripId(u.getGeneratedTripId());
        po.setStatus(u.getStatus() != null ? u.getStatus().name() : UploadStatus.PENDING.name());
        po.setCreatedAt(u.getCreatedAt());
        po.setUpdatedAt(u.getUpdatedAt());
        po.setVersion(u.getVersion());
        return po;
    }

    private MultimodalUpload fromPO(MultimodalUploadPO po) {
        if (po == null) return null;
        return MultimodalUpload.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .originalFilename(po.getOriginalFilename())
                .storedFilename(po.getStoredFilename())
                .contentType(po.getContentType())
                .fileSize(po.getFileSize())
                .storagePath(po.getStoragePath())
                .recognitionResult(deserializeMap(po.getRecognitionResult()))
                .generatedTripId(po.getGeneratedTripId())
                .status(parseStatus(po.getStatus()))
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .version(po.getVersion())
                .build();
    }

    private String serializeMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Map<String, Object> deserializeMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            Map<String, Object> map = objectMapper.readValue(json, MAP_TYPE);
            return map != null ? map : Map.of();
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private UploadStatus parseStatus(String value) {
        if (value == null) return null;
        try {
            return UploadStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
