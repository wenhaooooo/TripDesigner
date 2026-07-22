package com.tripdesigner.trip.infrastructure;
/**
 * 行程日仓储实现。
 * 使用 MyBatis Plus 实现持久化。
 */

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tripdesigner.trip.domain.TripDay;
import com.tripdesigner.trip.domain.TripDayRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TripDayRepositoryImpl implements TripDayRepository {
    private final TripDayMapper mapper;

    public TripDayRepositoryImpl(TripDayMapper mapper) { this.mapper = mapper; }

    @Override
    public TripDay save(TripDay tripDay) {
        TripDayPO po = toPO(tripDay);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public Optional<TripDay> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public List<TripDay> findByTripId(Long tripId) {
        return mapper.selectList(
                Wrappers.<TripDayPO>lambdaQuery()
                        .eq(TripDayPO::getTripId, tripId)
                        .orderByAsc(TripDayPO::getDayNumber))
                .stream()
                .map(this::fromPO)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private TripDayPO toPO(TripDay d) {
        TripDayPO po = new TripDayPO();
        po.setId(d.getId());
        po.setTripId(d.getTripId());
        po.setDayNumber(d.getDayNumber());
        po.setDate(d.getDate());
        po.setTitle(d.getTitle());
        po.setDescription(d.getDescription());
        po.setVersion(d.getVersion());
        return po;
    }

    private TripDay fromPO(TripDayPO po) {
        return TripDay.builder()
                .id(po.getId())
                .tripId(po.getTripId())
                .dayNumber(po.getDayNumber())
                .date(po.getDate())
                .title(po.getTitle())
                .description(po.getDescription())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .version(po.getVersion())
                .build();
    }
}
