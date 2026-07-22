package com.tripdesigner.community.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tripdesigner.community.domain.CommunityComment;
import com.tripdesigner.community.domain.CommunityCommentRepository;
import com.tripdesigner.community.domain.PostStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CommunityCommentRepositoryImpl implements CommunityCommentRepository {

    private final CommunityCommentMapper mapper;

    public CommunityCommentRepositoryImpl(CommunityCommentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public CommunityComment save(CommunityComment comment) {
        CommunityCommentPO po = toPO(comment);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public Optional<CommunityComment> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public List<CommunityComment> findByPostId(Long postId) {
        return mapper.selectList(Wrappers.<CommunityCommentPO>lambdaQuery()
                        .eq(CommunityCommentPO::getPostId, postId)
                        .eq(CommunityCommentPO::getStatus, PostStatus.PUBLISHED.name())
                        .orderByAsc(CommunityCommentPO::getCreatedAt))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private CommunityCommentPO toPO(CommunityComment c) {
        CommunityCommentPO po = new CommunityCommentPO();
        po.setId(c.getId());
        po.setPostId(c.getPostId());
        po.setUserId(c.getUserId());
        po.setParentId(c.getParentId());
        po.setContent(c.getContent());
        po.setLikeCount(c.getLikeCount());
        po.setStatus(c.getStatus() != null ? c.getStatus().name() : PostStatus.PUBLISHED.name());
        po.setCreatedAt(c.getCreatedAt());
        po.setUpdatedAt(c.getUpdatedAt());
        po.setVersion(c.getVersion());
        return po;
    }

    private CommunityComment fromPO(CommunityCommentPO po) {
        if (po == null) return null;
        return CommunityComment.builder()
                .id(po.getId())
                .postId(po.getPostId())
                .userId(po.getUserId())
                .parentId(po.getParentId())
                .content(po.getContent())
                .likeCount(po.getLikeCount())
                .status(parseStatus(po.getStatus()))
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .version(po.getVersion())
                .build();
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
