package com.tripdesigner.trip.infrastructure;
/**
 * 活动仓储实现。
 * 使用 MyBatis Plus 实现持久化。
 */

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tripdesigner.trip.domain.TripActivity;
import com.tripdesigner.trip.domain.TripActivityRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TripActivityRepositoryImpl implements TripActivityRepository {
    private final TripActivityMapper mapper;

    public TripActivityRepositoryImpl(TripActivityMapper mapper) { this.mapper = mapper; }

    @Override
    public TripActivity save(TripActivity activity) {
        TripActivityPO po = toPO(activity);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public Optional<TripActivity> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public List<TripActivity> findByTripDayId(Long tripDayId) {
        return mapper.selectList(
                Wrappers.<TripActivityPO>lambdaQuery()
                        .eq(TripActivityPO::getTripDayId, tripDayId)
                        .orderByAsc(TripActivityPO::getSortOrder))
                .stream()
                .map(this::fromPO)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private TripActivityPO toPO(TripActivity a) {
        TripActivityPO po = new TripActivityPO();
        po.setId(a.getId());
        po.setTripDayId(a.getTripDayId());
        po.setName(a.getName());
        po.setDescription(a.getDescription());
        po.setStartTime(a.getStartTime());
        po.setEndTime(a.getEndTime());
        po.setCategory(a.getCategory());
        po.setPlace(a.getPlace());
        po.setNotes(a.getNotes());
        po.setSortOrder(a.getSortOrder());
        po.setVersion(a.getVersion());
        return po;
    }

    private TripActivity fromPO(TripActivityPO po) {
        return TripActivity.builder()
                .id(po.getId())
                .tripDayId(po.getTripDayId())
                .name(po.getName())
                .description(po.getDescription())
                .startTime(po.getStartTime())
                .endTime(po.getEndTime())
                .category(po.getCategory())
                .place(po.getPlace())
                .notes(po.getNotes())
                .sortOrder(po.getSortOrder())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .version(po.getVersion())
                .build();
    }
}
