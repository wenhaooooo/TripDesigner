package com.tripdesigner.community.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tripdesigner.community.domain.CommunityLike;
import com.tripdesigner.community.domain.CommunityLikeRepository;
import com.tripdesigner.community.domain.LikeTargetType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CommunityLikeRepositoryImpl implements CommunityLikeRepository {

    private final CommunityLikeMapper mapper;

    public CommunityLikeRepositoryImpl(CommunityLikeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public CommunityLike save(CommunityLike like) {
        CommunityLikePO po = toPO(like);
        mapper.insert(po);
        return fromPO(po);
    }

    @Override
    public Optional<CommunityLike> findByUserAndTarget(Long userId, Long targetId, LikeTargetType targetType) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<CommunityLikePO>lambdaQuery()
                        .eq(CommunityLikePO::getUserId, userId)
                        .eq(CommunityLikePO::getTargetId, targetId)
                        .eq(CommunityLikePO::getTargetType, targetType.name())))
                .map(this::fromPO);
    }

    @Override
    public void delete(Long userId, Long targetId, LikeTargetType targetType) {
        mapper.delete(Wrappers.<CommunityLikePO>lambdaQuery()
                .eq(CommunityLikePO::getUserId, userId)
                .eq(CommunityLikePO::getTargetId, targetId)
                .eq(CommunityLikePO::getTargetType, targetType.name()));
    }

    @Override
    public boolean exists(Long userId, Long targetId, LikeTargetType targetType) {
        return mapper.exists(Wrappers.<CommunityLikePO>lambdaQuery()
                .eq(CommunityLikePO::getUserId, userId)
                .eq(CommunityLikePO::getTargetId, targetId)
                .eq(CommunityLikePO::getTargetType, targetType.name()));
    }

    private CommunityLikePO toPO(CommunityLike l) {
        CommunityLikePO po = new CommunityLikePO();
        po.setId(l.getId());
        po.setUserId(l.getUserId());
        po.setTargetId(l.getTargetId());
        po.setTargetType(l.getTargetType() != null ? l.getTargetType().name() : null);
        po.setCreatedAt(l.getCreatedAt());
        return po;
    }

    private CommunityLike fromPO(CommunityLikePO po) {
        if (po == null) return null;
        return CommunityLike.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .targetId(po.getTargetId())
                .targetType(parseTargetType(po.getTargetType()))
                .createdAt(po.getCreatedAt())
                .build();
    }

    private LikeTargetType parseTargetType(String value) {
        if (value == null) return null;
        try {
            return LikeTargetType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
