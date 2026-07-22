package com.tripdesigner.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tripdesigner.knowledge.infrastructure.po.KnowledgeRelationPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识关系 MyBatis Mapper 接口，对应 kb_knowledge_relations 表。
 */
@Mapper
public interface KnowledgeRelationMapper extends BaseMapper<KnowledgeRelationPO> {
}
