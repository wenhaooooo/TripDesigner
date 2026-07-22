package com.tripdesigner.community.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tripdesigner.community.domain.CommunityFavorite;
import com.tripdesigner.community.domain.CommunityFavoriteRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CommunityFavoriteRepositoryImpl implements CommunityFavoriteRepository {

    private final CommunityFavoriteMapper mapper;

    public CommunityFavoriteRepositoryImpl(CommunityFavoriteMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public CommunityFavorite save(CommunityFavorite favorite) {
        CommunityFavoritePO po = toPO(favorite);
        mapper.insert(po);
        return fromPO(po);
    }

    @Override
    public Optional<CommunityFavorite> findByUserAndPost(Long userId, Long postId) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<CommunityFavoritePO>lambdaQuery()
                        .eq(CommunityFavoritePO::getUserId, userId)
                        .eq(CommunityFavoritePO::getPostId, postId)))
                .map(this::fromPO);
    }

    @Override
    public List<CommunityFavorite> findByUserId(Long userId) {
        return mapper.selectList(Wrappers.<CommunityFavoritePO>lambdaQuery()
                        .eq(CommunityFavoritePO::getUserId, userId)
                        .orderByDesc(CommunityFavoritePO::getCreatedAt))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public void delete(Long userId, Long postId) {
        mapper.delete(Wrappers.<CommunityFavoritePO>lambdaQuery()
                .eq(CommunityFavoritePO::getUserId, userId)
                .eq(CommunityFavoritePO::getPostId, postId));
    }

    @Override
    public boolean exists(Long userId, Long postId) {
        return mapper.exists(Wrappers.<CommunityFavoritePO>lambdaQuery()
                .eq(CommunityFavoritePO::getUserId, userId)
                .eq(CommunityFavoritePO::getPostId, postId));
    }

    private CommunityFavoritePO toPO(CommunityFavorite f) {
        CommunityFavoritePO po = new CommunityFavoritePO();
        po.setId(f.getId());
        po.setUserId(f.getUserId());
        po.setPostId(f.getPostId());
        po.setCreatedAt(f.getCreatedAt());
        return po;
    }

    private CommunityFavorite fromPO(CommunityFavoritePO po) {
        if (po == null) return null;
        return CommunityFavorite.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .postId(po.getPostId())
                .createdAt(po.getCreatedAt())
                .build();
    }
}
