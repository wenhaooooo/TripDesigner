package com.tripdesigner.community.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.community.domain.CommunityPost;
import com.tripdesigner.community.domain.CommunityPostRepository;
import com.tripdesigner.community.domain.PostStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CommunityPostRepositoryImpl implements CommunityPostRepository {

    private final CommunityPostMapper mapper;
    private final ObjectMapper objectMapper;

    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};

    public CommunityPostRepositoryImpl(CommunityPostMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public CommunityPost save(CommunityPost post) {
        CommunityPostPO po = toPO(post);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public Optional<CommunityPost> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public List<CommunityPost> findAll(int page, int size) {
        Page<CommunityPostPO> p = new Page<>(page + 1, size);
        return mapper.selectPage(p, Wrappers.<CommunityPostPO>lambdaQuery()
                        .eq(CommunityPostPO::getStatus, PostStatus.PUBLISHED.name())
                        .orderByDesc(CommunityPostPO::getCreatedAt))
                .getRecords().stream().map(this::fromPO).toList();
    }

    @Override
    public List<CommunityPost> findByUserId(Long userId) {
        return mapper.selectList(Wrappers.<CommunityPostPO>lambdaQuery()
                        .eq(CommunityPostPO::getUserId, userId)
                        .orderByDesc(CommunityPostPO::getCreatedAt))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public List<CommunityPost> findByDestination(String destination, int page, int size) {
        Page<CommunityPostPO> p = new Page<>(page + 1, size);
        return mapper.selectPage(p, Wrappers.<CommunityPostPO>lambdaQuery()
                        .eq(CommunityPostPO::getStatus, PostStatus.PUBLISHED.name())
                        .eq(CommunityPostPO::getDestination, destination)
                        .orderByDesc(CommunityPostPO::getCreatedAt))
                .getRecords().stream().map(this::fromPO).toList();
    }

    @Override
    public List<CommunityPost> findHot(int limit) {
        Page<CommunityPostPO> p = new Page<>(1, limit);
        return mapper.selectPage(p, Wrappers.<CommunityPostPO>lambdaQuery()
                        .eq(CommunityPostPO::getStatus, PostStatus.PUBLISHED.name())
                        .orderByDesc(CommunityPostPO::getLikeCount))
                .getRecords().stream().map(this::fromPO).toList();
    }

    @Override
    public long count() {
        return mapper.selectCount(Wrappers.<CommunityPostPO>lambdaQuery()
                .eq(CommunityPostPO::getStatus, PostStatus.PUBLISHED.name()));
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private CommunityPostPO toPO(CommunityPost p) {
        CommunityPostPO po = new CommunityPostPO();
        po.setId(p.getId());
        po.setUserId(p.getUserId());
        po.setTitle(p.getTitle());
        po.setContent(p.getContent());
        po.setDestination(p.getDestination());
        po.setTags(serializeList(p.getTags()));
        po.setMediaUrls(serializeList(p.getMediaUrls()));
        po.setViewCount(p.getViewCount());
        po.setLikeCount(p.getLikeCount());
        po.setCommentCount(p.getCommentCount());
        po.setFavoriteCount(p.getFavoriteCount());
        po.setStatus(p.getStatus() != null ? p.getStatus().name() : PostStatus.PUBLISHED.name());
        po.setCreatedAt(p.getCreatedAt());
        po.setUpdatedAt(p.getUpdatedAt());
        po.setVersion(p.getVersion());
        return po;
    }

    private CommunityPost fromPO(CommunityPostPO po) {
        if (po == null) return null;
        return CommunityPost.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .title(po.getTitle())
                .content(po.getContent())
                .destination(po.getDestination())
                .tags(deserializeList(po.getTags()))
                .mediaUrls(deserializeList(po.getMediaUrls()))
                .viewCount(po.getViewCount())
                .likeCount(po.getLikeCount())
                .commentCount(po.getCommentCount())
                .favoriteCount(po.getFavoriteCount())
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

    private PostStatus parseStatus(String value) {
        if (value == null) return null;
        try {
            return PostStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
