package com.tripdesigner.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tripdesigner.knowledge.infrastructure.po.KnowledgeSourcePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识数据源 MyBatis Mapper 接口，对应 kb_knowledge_sources 表。
 */
@Mapper
public interface KnowledgeSourceMapper extends BaseMapper<KnowledgeSourcePO> {
}
