package com.tripdesigner.checkin.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.checkin.domain.CheckinStatus;
import com.tripdesigner.checkin.domain.TripCheckin;
import com.tripdesigner.checkin.domain.TripCheckinRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TripCheckinRepositoryImpl implements TripCheckinRepository {

    private final TripCheckinMapper mapper;
    private final ObjectMapper objectMapper;

    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};

    public TripCheckinRepositoryImpl(TripCheckinMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public TripCheckin save(TripCheckin checkin) {
        TripCheckinPO po = toPO(checkin);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public Optional<TripCheckin> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public List<TripCheckin> findByUserId(Long userId) {
        return mapper.selectList(Wrappers.<TripCheckinPO>lambdaQuery()
                        .eq(TripCheckinPO::getUserId, userId)
                        .orderByDesc(TripCheckinPO::getCheckedInAt))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public List<TripCheckin> findByTripId(Long tripId) {
        return mapper.selectList(Wrappers.<TripCheckinPO>lambdaQuery()
                        .eq(TripCheckinPO::getTripId, tripId)
                        .orderByAsc(TripCheckinPO::getCheckedInAt))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public List<TripCheckin> findByActivityId(Long activityId) {
        return mapper.selectList(Wrappers.<TripCheckinPO>lambdaQuery()
                        .eq(TripCheckinPO::getActivityId, activityId))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public Optional<TripCheckin> findByUserAndActivity(Long userId, Long activityId) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<TripCheckinPO>lambdaQuery()
                        .eq(TripCheckinPO::getUserId, userId)
                        .eq(TripCheckinPO::getActivityId, activityId)))
                .map(this::fromPO);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private TripCheckinPO toPO(TripCheckin c) {
        TripCheckinPO po = new TripCheckinPO();
        po.setId(c.getId());
        po.setUserId(c.getUserId());
        po.setTripId(c.getTripId());
        po.setTripDayId(c.getTripDayId());
        po.setActivityId(c.getActivityId());
        po.setPlaceName(c.getPlaceName());
        po.setLatitude(c.getLatitude());
        po.setLongitude(c.getLongitude());
        po.setNotes(c.getNotes());
        po.setPhotoUrls(serializeList(c.getPhotoUrls()));
        po.setStatus(c.getStatus() != null ? c.getStatus().name() : null);
        po.setCheckedInAt(c.getCheckedInAt());
        po.setCreatedAt(c.getCreatedAt());
        po.setUpdatedAt(c.getUpdatedAt());
        po.setVersion(c.getVersion());
        return po;
    }

    private TripCheckin fromPO(TripCheckinPO po) {
        if (po == null) return null;
        return TripCheckin.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .tripId(po.getTripId())
                .tripDayId(po.getTripDayId())
                .activityId(po.getActivityId())
                .placeName(po.getPlaceName())
                .latitude(po.getLatitude())
                .longitude(po.getLongitude())
                .notes(po.getNotes())
                .photoUrls(deserializeList(po.getPhotoUrls()))
                .status(parseStatus(po.getStatus()))
                .checkedInAt(po.getCheckedInAt())
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

    private CheckinStatus parseStatus(String value) {
        if (value == null) return null;
        try {
            return CheckinStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
