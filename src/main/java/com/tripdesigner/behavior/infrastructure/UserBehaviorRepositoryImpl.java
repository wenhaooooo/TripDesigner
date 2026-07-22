package com.tripdesigner.behavior.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.behavior.domain.BehaviorType;
import com.tripdesigner.behavior.domain.TargetType;
import com.tripdesigner.behavior.domain.UserBehavior;
import com.tripdesigner.behavior.domain.UserBehaviorRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserBehaviorRepositoryImpl implements UserBehaviorRepository {

    private final UserBehaviorMapper mapper;
    private final ObjectMapper objectMapper;

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    public UserBehaviorRepositoryImpl(UserBehaviorMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public UserBehavior save(UserBehavior behavior) {
        UserBehaviorPO po = toPO(behavior);
        mapper.insert(po);
        return fromPO(po);
    }

    @Override
    public Optional<UserBehavior> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public List<UserBehavior> findByUserId(Long userId, int limit) {
        return mapper.selectList(Wrappers.<UserBehaviorPO>lambdaQuery()
                        .eq(UserBehaviorPO::getUserId, userId)
                        .orderByDesc(UserBehaviorPO::getCreatedAt)
                        .last("LIMIT " + Math.max(1, limit)))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public List<UserBehavior> findByUserIdSince(Long userId, Instant since) {
        return mapper.selectList(Wrappers.<UserBehaviorPO>lambdaQuery()
                        .eq(UserBehaviorPO::getUserId, userId)
                        .ge(UserBehaviorPO::getCreatedAt, since)
                        .orderByDesc(UserBehaviorPO::getCreatedAt))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public List<UserBehavior> findByUserIdAndType(Long userId, BehaviorType type, int limit) {
        return mapper.selectList(Wrappers.<UserBehaviorPO>lambdaQuery()
                        .eq(UserBehaviorPO::getUserId, userId)
                        .eq(UserBehaviorPO::getBehaviorType, type.name())
                        .orderByDesc(UserBehaviorPO::getCreatedAt)
                        .last("LIMIT " + Math.max(1, limit)))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public long countByUserId(Long userId) {
        return mapper.selectCount(Wrappers.<UserBehaviorPO>lambdaQuery()
                .eq(UserBehaviorPO::getUserId, userId));
    }

    private UserBehaviorPO toPO(UserBehavior b) {
        UserBehaviorPO po = new UserBehaviorPO();
        po.setId(b.getId());
        po.setUserId(b.getUserId());
        po.setBehaviorType(b.getBehaviorType() != null ? b.getBehaviorType().name() : null);
        po.setTargetType(b.getTargetType() != null ? b.getTargetType().name() : null);
        po.setTargetId(b.getTargetId());
        po.setContext(serializeMap(b.getContext()));
        po.setWeight(b.getWeight());
        po.setCreatedAt(b.getCreatedAt());
        return po;
    }

    private UserBehavior fromPO(UserBehaviorPO po) {
        if (po == null) return null;
        return UserBehavior.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .behaviorType(parseBehaviorType(po.getBehaviorType()))
                .targetType(parseTargetType(po.getTargetType()))
                .targetId(po.getTargetId())
                .context(deserializeMap(po.getContext()))
                .weight(po.getWeight())
                .createdAt(po.getCreatedAt())
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

    private BehaviorType parseBehaviorType(String value) {
        if (value == null) return null;
        try {
            return BehaviorType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private TargetType parseTargetType(String value) {
        if (value == null) return null;
        try {
            return TargetType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
