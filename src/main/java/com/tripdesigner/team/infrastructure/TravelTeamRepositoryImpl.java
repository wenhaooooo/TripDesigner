package com.tripdesigner.team.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.team.domain.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class TravelTeamRepositoryImpl implements TravelTeamRepository {

    private final TravelTeamMapper mapper;
    private final ObjectMapper objectMapper;

    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};

    public TravelTeamRepositoryImpl(TravelTeamMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public TravelTeam save(TravelTeam team) {
        TravelTeamPO po = toPO(team);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public Optional<TravelTeam> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public List<TravelTeam> findOpen(int page, int size) {
        Page<TravelTeamPO> p = new Page<>(page + 1, size);
        return mapper.selectPage(p, Wrappers.<TravelTeamPO>lambdaQuery()
                        .eq(TravelTeamPO::getStatus, TeamStatus.OPEN.name())
                        .orderByDesc(TravelTeamPO::getCreatedAt))
                .getRecords().stream().map(this::fromPO).toList();
    }

    @Override
    public List<TravelTeam> findByCreator(Long userId) {
        return mapper.selectList(Wrappers.<TravelTeamPO>lambdaQuery()
                        .eq(TravelTeamPO::getCreatorId, userId)
                        .orderByDesc(TravelTeamPO::getCreatedAt))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public List<TravelTeam> findByDestination(String destination, int page, int size) {
        Page<TravelTeamPO> p = new Page<>(page + 1, size);
        return mapper.selectPage(p, Wrappers.<TravelTeamPO>lambdaQuery()
                        .eq(TravelTeamPO::getDestination, destination)
                        .eq(TravelTeamPO::getStatus, TeamStatus.OPEN.name())
                        .orderByDesc(TravelTeamPO::getCreatedAt))
                .getRecords().stream().map(this::fromPO).toList();
    }

    @Override
    public List<TravelTeam> findMatches(String destination, LocalDate startDate, LocalDate endDate) {
        // 匹配条件：同目的地 + 日期重叠 + 状态 OPEN
        return mapper.selectList(Wrappers.<TravelTeamPO>lambdaQuery()
                        .eq(TravelTeamPO::getDestination, destination)
                        .eq(TravelTeamPO::getStatus, TeamStatus.OPEN.name())
                        .le(TravelTeamPO::getStartDate, endDate)
                        .ge(TravelTeamPO::getEndDate, startDate)
                        .orderByDesc(TravelTeamPO::getCreatedAt))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private TravelTeamPO toPO(TravelTeam t) {
        TravelTeamPO po = new TravelTeamPO();
        po.setId(t.getId());
        po.setCreatorId(t.getCreatorId());
        po.setTitle(t.getTitle());
        po.setDescription(t.getDescription());
        po.setDestination(t.getDestination());
        po.setStartDate(t.getStartDate());
        po.setEndDate(t.getEndDate());
        po.setTeamType(t.getTeamType() != null ? t.getTeamType().name() : null);
        po.setInterests(serializeList(t.getInterests()));
        po.setMaxMembers(t.getMaxMembers());
        po.setCurrentMembers(t.getCurrentMembers());
        po.setGenderRequirement(t.getGenderRequirement());
        po.setMinAge(t.getMinAge());
        po.setMaxAge(t.getMaxAge());
        po.setContact(t.getContact());
        po.setStatus(t.getStatus() != null ? t.getStatus().name() : null);
        po.setCreatedAt(t.getCreatedAt());
        po.setUpdatedAt(t.getUpdatedAt());
        po.setVersion(t.getVersion());
        return po;
    }

    private TravelTeam fromPO(TravelTeamPO po) {
        if (po == null) return null;
        return TravelTeam.builder()
                .id(po.getId())
                .creatorId(po.getCreatorId())
                .title(po.getTitle())
                .description(po.getDescription())
                .destination(po.getDestination())
                .startDate(po.getStartDate())
                .endDate(po.getEndDate())
                .teamType(parseTeamType(po.getTeamType()))
                .interests(deserializeList(po.getInterests()))
                .maxMembers(po.getMaxMembers())
                .currentMembers(po.getCurrentMembers())
                .genderRequirement(po.getGenderRequirement())
                .minAge(po.getMinAge())
                .maxAge(po.getMaxAge())
                .contact(po.getContact())
                .status(parseStatus(po.getStatus()))
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .version(po.getVersion())
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

    private List<String> deserializeList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<String> list = objectMapper.readValue(json, LIST_TYPE);
            return list != null ? list : List.of();
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private TeamType parseTeamType(String value) {
        if (value == null) return null;
        try {
            return TeamType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private TeamStatus parseStatus(String value) {
        if (value == null) return null;
        try {
            return TeamStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
