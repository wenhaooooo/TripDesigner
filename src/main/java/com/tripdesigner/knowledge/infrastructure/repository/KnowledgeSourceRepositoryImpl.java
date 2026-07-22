package com.tripdesigner.knowledge.infrastructure.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tripdesigner.knowledge.domain.KnowledgeSource;
import com.tripdesigner.knowledge.domain.repository.KnowledgeSourceRepository;
import com.tripdesigner.knowledge.infrastructure.mapper.KnowledgeSourceMapper;
import com.tripdesigner.knowledge.infrastructure.po.KnowledgeSourcePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 知识数据源仓储实现。
 * 使用 MyBatis Plus 实现持久化。无 JSONB 字段，无需 ObjectMapper 转换。
 */
@Repository
@RequiredArgsConstructor
public class KnowledgeSourceRepositoryImpl implements KnowledgeSourceRepository {

    private final KnowledgeSourceMapper mapper;

    @Override
    public Optional<KnowledgeSource> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public Optional<KnowledgeSource> findBySourceTypeAndSourceId(String sourceType, String sourceId) {
        return Optional.ofNullable(mapper.selectOne(
                        Wrappers.<KnowledgeSourcePO>lambdaQuery()
                                .eq(KnowledgeSourcePO::getSourceType, sourceType)
                                .eq(KnowledgeSourcePO::getSourceId, sourceId)))
                .map(this::fromPO);
    }

    @Override
    public KnowledgeSource save(KnowledgeSource source) {
        KnowledgeSourcePO po = toPO(source);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public List<KnowledgeSource> findPending(int limit) {
        return mapper.selectList(
                        Wrappers.<KnowledgeSourcePO>lambdaQuery()
                                .eq(KnowledgeSourcePO::getStatus, "PENDING")
                                .orderByAsc(KnowledgeSourcePO::getCreatedAt)
                                .last("LIMIT " + limit))
                .stream().map(this::fromPO).toList();
    }

    private KnowledgeSourcePO toPO(KnowledgeSource s) {
        KnowledgeSourcePO po = new KnowledgeSourcePO();
        po.setId(s.getId());
        po.setSourceType(s.getSourceType());
        po.setSourceUrl(s.getSourceUrl());
        po.setSourceId(s.getSourceId());
        po.setEntityType(s.getEntityType());
        po.setEntityId(s.getEntityId());
        po.setRawContent(s.getRawContent());
        po.setContentHash(s.getContentHash());
        po.setFetchedAt(s.getFetchedAt());
        po.setEtag(s.getEtag());
        po.setLastModified(s.getLastModified());
        po.setStatus(s.getStatus());
        po.setErrorMessage(s.getErrorMessage());
        po.setRetryCount(s.getRetryCount());
        po.setVersion(s.getVersion());
        return po;
    }

    private KnowledgeSource fromPO(KnowledgeSourcePO po) {
        if (po == null) return null;
        return KnowledgeSource.builder()
                .id(po.getId())
                .sourceType(po.getSourceType())
                .sourceUrl(po.getSourceUrl())
                .sourceId(po.getSourceId())
                .entityType(po.getEntityType())
                .entityId(po.getEntityId())
                .rawContent(po.getRawContent())
                .contentHash(po.getContentHash())
                .fetchedAt(po.getFetchedAt())
                .etag(po.getEtag())
                .lastModified(po.getLastModified())
                .status(po.getStatus())
                .errorMessage(po.getErrorMessage())
                .retryCount(po.getRetryCount())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .version(po.getVersion())
                .build();
    }
}
