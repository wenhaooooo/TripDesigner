package com.tripdesigner.trip.infrastructure;
/**
 * 行程分享仓储实现。
 * 使用 MyBatis Plus 实现持久化，负责 TripShare/TripSharePO 转换。
 * 按行程 ID 查询时按创建时间降序排列。
 */

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tripdesigner.trip.domain.ShareStatus;
import com.tripdesigner.trip.domain.ShareType;
import com.tripdesigner.trip.domain.TripShare;
import com.tripdesigner.trip.domain.TripShareRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TripShareRepositoryImpl implements TripShareRepository {
    private final TripShareMapper mapper;

    public TripShareRepositoryImpl(TripShareMapper mapper) { this.mapper = mapper; }

    @Override
    public TripShare save(TripShare tripShare) {
        TripSharePO po = toPO(tripShare);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public Optional<TripShare> findByToken(String token) {
        return Optional.ofNullable(mapper.selectOne(
                Wrappers.<TripSharePO>lambdaQuery().eq(TripSharePO::getShareToken, token)))
                .map(this::fromPO);
    }

    @Override
    public Optional<TripShare> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public List<TripShare> findByTripId(Long tripId) {
        return mapper.selectList(
                Wrappers.<TripSharePO>lambdaQuery()
                        .eq(TripSharePO::getTripId, tripId)
                        .orderByDesc(TripSharePO::getCreatedAt))
                .stream()
                .map(this::fromPO)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private TripSharePO toPO(TripShare s) {
        TripSharePO po = new TripSharePO();
        po.setId(s.getId());
        po.setTripId(s.getTripId());
        po.setOwnerUserId(s.getOwnerUserId());
        po.setShareToken(s.getShareToken());
        po.setShareType(s.getShareType() != null ? s.getShareType().name() : null);
        po.setMaxViews(s.getMaxViews());
        po.setCurrentViews(s.getCurrentViews());
        po.setExpiresAt(s.getExpiresAt());
        po.setStatus(s.getStatus() != null ? s.getStatus().name() : null);
        po.setVersion(s.getVersion());
        return po;
    }

    private TripShare fromPO(TripSharePO po) {
        return TripShare.builder()
                .id(po.getId())
                .tripId(po.getTripId())
                .ownerUserId(po.getOwnerUserId())
                .shareToken(po.getShareToken())
                .shareType(po.getShareType() != null ? ShareType.valueOf(po.getShareType()) : null)
                .maxViews(po.getMaxViews())
                .currentViews(po.getCurrentViews())
                .expiresAt(po.getExpiresAt())
                .status(po.getStatus() != null ? ShareStatus.valueOf(po.getStatus()) : null)
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .version(po.getVersion())
                .build();
    }
}
