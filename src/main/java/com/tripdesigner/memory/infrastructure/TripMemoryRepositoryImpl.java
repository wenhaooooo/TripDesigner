package com.tripdesigner.memory.infrastructure;
/**
 * 旅行记忆仓储实现。
 * 使用 MyBatis Plus 实现持久化。
 * 支持按用户 ID 查询，按类型筛选。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.memory.domain.TripMemory;
import com.tripdesigner.memory.domain.TripMemoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TripMemoryRepositoryImpl implements TripMemoryRepository {

    private final TripMemoryMapper mapper;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public TripMemoryRepositoryImpl(TripMemoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TripMemory save(TripMemory memory) {
        TripMemoryPO po = new TripMemoryPO();
        po.setUserId(memory.getUserId());
        po.setTripId(memory.getTripId());
        po.setMemoryType(memory.getMemoryType());
        po.setContent(memory.getContent());
        po.setTags(serializeList(memory.getTags()));
        mapper.insert(po);
        return TripMemory.builder()
                .id(po.getId())
                .userId(memory.getUserId())
                .tripId(memory.getTripId())
                .memoryType(memory.getMemoryType())
                .content(memory.getContent())
                .tags(memory.getTags())
                .createdAt(po.getCreatedAt())
                .build();
    }

    @Override
    public Optional<TripMemory> findById(Long id) {
        TripMemoryPO po = mapper.selectById(id);
        if (po == null) return Optional.empty();
        return Optional.of(toDomain(po));
    }

    @Override
    public List<TripMemory> findByUserId(Long userId) {
        LambdaQueryWrapper<TripMemoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TripMemoryPO::getUserId, userId)
                .orderByDesc(TripMemoryPO::getCreatedAt);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<TripMemory> findByUserIdAndType(Long userId, String memoryType) {
        LambdaQueryWrapper<TripMemoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TripMemoryPO::getUserId, userId)
                .eq(TripMemoryPO::getMemoryType, memoryType)
                .orderByDesc(TripMemoryPO::getCreatedAt);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private TripMemory toDomain(TripMemoryPO po) {
        return TripMemory.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .tripId(po.getTripId())
                .memoryType(po.getMemoryType())
                .content(po.getContent())
                .tags(parseList(po.getTags()))
                .createdAt(po.getCreatedAt())
                .build();
    }

    private String serializeList(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
