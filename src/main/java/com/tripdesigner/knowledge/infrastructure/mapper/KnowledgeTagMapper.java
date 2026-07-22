package com.tripdesigner.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tripdesigner.knowledge.infrastructure.po.KnowledgeTagPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识标签 MyBatis Mapper 接口，对应 kb_knowledge_tags 表。
 */
@Mapper
public interface KnowledgeTagMapper extends BaseMapper<KnowledgeTagPO> {
}
