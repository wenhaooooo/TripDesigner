package com.tripdesigner.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tripdesigner.knowledge.infrastructure.po.KnowledgeChunkPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识分块 MyBatis Mapper 接口，对应 kb_knowledge_chunks 表。
 * <p>
 * 除标准 CRUD 外，提供基于 pgvector 的向量相似度检索方法。
 * embedding 字段不在此 PO 中声明，通过自定义 SQL 的 CAST(#{queryVector} AS vector) 进行查询。
 */
@Mapper
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunkPO> {

    /**
     * 按实体类型过滤的向量相似度检索（余弦距离）。
     *
     * @param queryVector 查询向量，pgvector 字符串格式如 "[0.1,0.2,...]"
     * @param entityType  实体类型
     * @param topK        返回条数
     * @return 按距离升序排列的分块列表
     */
    @Select("SELECT *, embedding <=> CAST(#{queryVector} AS vector) AS distance FROM kb_knowledge_chunks " +
            "WHERE entity_type = #{entityType} " +
            "ORDER BY embedding <=> CAST(#{queryVector} AS vector) LIMIT #{topK}")
    List<KnowledgeChunkPO> vectorSearch(@Param("queryVector") String queryVector,
                                        @Param("entityType") String entityType,
                                        @Param("topK") int topK);

    /**
     * 全库向量相似度检索（余弦距离），不限定实体类型。
     *
     * @param queryVector 查询向量，pgvector 字符串格式
     * @param topK        返回条数
     * @return 按距离升序排列的分块列表
     */
    @Select("SELECT *, embedding <=> CAST(#{queryVector} AS vector) AS distance FROM kb_knowledge_chunks " +
            "ORDER BY embedding <=> CAST(#{queryVector} AS vector) LIMIT #{topK}")
    List<KnowledgeChunkPO> vectorSearchAll(@Param("queryVector") String queryVector,
                                           @Param("topK") int topK);
}
