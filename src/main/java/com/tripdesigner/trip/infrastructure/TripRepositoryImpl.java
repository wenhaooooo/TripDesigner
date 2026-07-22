package com.tripdesigner.trip.infrastructure;
/**
 * 行程仓储实现。
 * 使用 MyBatis Plus 实现持久化，负责 Trip/TripPO 转换。
 * 按用户 ID 查询时按创建时间降序排列。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tripdesigner.trip.domain.Trip;
import com.tripdesigner.trip.domain.TripRepository;
import com.tripdesigner.trip.domain.TripStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TripRepositoryImpl implements TripRepository {
    private final TripMapper mapper;

    public TripRepositoryImpl(TripMapper mapper) { this.mapper = mapper; }

    @Override
    public Trip save(Trip trip) {
        TripPO po = toPO(trip);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public Optional<Trip> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public List<Trip> findByUserId(Long userId) {
        return mapper.selectList(
                Wrappers.<TripPO>lambdaQuery()
                        .eq(TripPO::getUserId, userId)
                        .orderByDesc(TripPO::getCreatedAt))
                .stream()
                .map(this::fromPO)
                .toList();
    }

    @Override
    public List<Trip> findByUserId(Long userId, int page, int size) {
        Page<TripPO> pageReq = new Page<>(page + 1, size);
        LambdaQueryWrapper<TripPO> wrapper = Wrappers.<TripPO>lambdaQuery()
                .eq(TripPO::getUserId, userId)
                .orderByDesc(TripPO::getCreatedAt);
        return mapper.selectPage(pageReq, wrapper).getRecords()
                .stream()
                .map(this::fromPO)
                .toList();
    }

    @Override
    public long countByUserId(Long userId) {
        return mapper.selectCount(Wrappers.<TripPO>lambdaQuery().eq(TripPO::getUserId, userId));
    }

    @Override
    public List<Trip> searchByUserId(Long userId, String keyword, int page, int size) {
        Page<TripPO> pageReq = new Page<>(page + 1, size);
        LambdaQueryWrapper<TripPO> wrapper = Wrappers.<TripPO>lambdaQuery()
                .eq(TripPO::getUserId, userId)
                .and(w -> w.like(TripPO::getTitle, keyword)
                        .or().like(TripPO::getDestinationName, keyword))
                .orderByDesc(TripPO::getCreatedAt);
        return mapper.selectPage(pageReq, wrapper).getRecords()
                .stream()
                .map(this::fromPO)
                .toList();
    }

    @Override
    public long countByUserIdAndKeyword(Long userId, String keyword) {
        LambdaQueryWrapper<TripPO> wrapper = Wrappers.<TripPO>lambdaQuery()
                .eq(TripPO::getUserId, userId)
                .and(w -> w.like(TripPO::getTitle, keyword)
                        .or().like(TripPO::getDestinationName, keyword));
        return mapper.selectCount(wrapper);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private TripPO toPO(Trip t) {
        TripPO po = new TripPO();
        po.setId(t.getId());
        po.setUserId(t.getUserId());
        po.setTitle(t.getTitle());
        po.setDescription(t.getDescription());
        po.setDestinationName(t.getDestinationName());
        po.setStatus(t.getStatus() != null ? t.getStatus().getCode() : null);
        po.setStartDate(t.getStartDate());
        po.setEndDate(t.getEndDate());
        po.setBudget(t.getBudget());
        po.setVersion(t.getVersion());
        return po;
    }

    private Trip fromPO(TripPO po) {
        return Trip.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .title(po.getTitle())
                .description(po.getDescription())
                .destinationName(po.getDestinationName())
                .status(po.getStatus() != null ? TripStatus.of(po.getStatus()) : null)
                .startDate(po.getStartDate())
                .endDate(po.getEndDate())
                .budget(po.getBudget())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .version(po.getVersion())
                .build();
    }
}
