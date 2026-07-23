package com.tripdesigner.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tripdesigner.knowledge.infrastructure.po.KnowledgeChunkPO;
import com.tripdesigner.knowledge.rag.SearchFilters;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

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

    /**
     * 带多条件过滤的向量相似度检索。
     */
    @Select({
            "<script>",
            "SELECT *, embedding <![CDATA[<=>]]> CAST(#{queryVector} AS vector) AS distance FROM kb_knowledge_chunks WHERE 1=1 ",
            "<if test=\"filters.entityType != null\">AND entity_type = #{filters.entityType}</if> ",
            "<if test=\"filters.cityId != null\">AND (metadata->>'city_id')::bigint = #{filters.cityId}</if> ",
            "<if test=\"filters.countryId != null\">AND (metadata->>'country_id')::bigint = #{filters.countryId}</if> ",
            "<if test=\"filters.category != null\">AND category = #{filters.category}</if> ",
            "<if test=\"filters.language != null\">AND language = #{filters.language}</if> ",
            "ORDER BY embedding <![CDATA[<=>]]> CAST(#{queryVector} AS vector) LIMIT #{filters.topK}",
            "</script>"
    })
    List<KnowledgeChunkPO> vectorSearchWithFilters(@Param("queryVector") String queryVector,
                                                   @Param("filters") SearchFilters filters);

    /**
     * PostgreSQL 全文搜索。
     */
    @Select({
            "<script>",
            "SELECT *, ts_rank_cd(to_tsvector('english', content), plainto_tsquery('english', #{query})) AS distance ",
            "FROM kb_knowledge_chunks WHERE 1=1 ",
            "<if test=\"filters.entityType != null\">AND entity_type = #{filters.entityType}</if> ",
            "<if test=\"filters.cityId != null\">AND (metadata->>'city_id')::bigint = #{filters.cityId}</if> ",
            "<if test=\"filters.countryId != null\">AND (metadata->>'country_id')::bigint = #{filters.countryId}</if> ",
            "<if test=\"filters.category != null\">AND category = #{filters.category}</if> ",
            "AND to_tsvector('english', content) <![CDATA[@@]]> plainto_tsquery('english', #{query}) ",
            "ORDER BY distance DESC LIMIT #{filters.topK}",
            "</script>"
    })
    List<KnowledgeChunkPO> keywordSearch(@Param("query") String query,
                                         @Param("filters") SearchFilters filters);

    /**
     * 纯元数据条件检索。
     */
    @Select({
            "<script>",
            "SELECT * FROM kb_knowledge_chunks WHERE 1=1 ",
            "<if test=\"filters.entityType != null\">AND entity_type = #{filters.entityType}</if> ",
            "<if test=\"filters.cityId != null\">AND (metadata->>'city_id')::bigint = #{filters.cityId}</if> ",
            "<if test=\"filters.countryId != null\">AND (metadata->>'country_id')::bigint = #{filters.countryId}</if> ",
            "<if test=\"filters.category != null\">AND category = #{filters.category}</if> ",
            "<if test=\"filters.language != null\">AND language = #{filters.language}</if> ",
            "ORDER BY created_at DESC LIMIT #{filters.topK}",
            "</script>"
    })
    List<KnowledgeChunkPO> metadataSearch(@Param("filters") SearchFilters filters);

    /**
     * 根据分块 ID 批量获取嵌入向量。
     */
    @Select({
            "<script>",
            "SELECT id, embedding FROM kb_knowledge_chunks WHERE id IN ",
            "<foreach collection=\"chunkIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">#{id}</foreach>",
            "</script>"
    })
    List<Map<String, Object>> findEmbeddingsByIds(@Param("chunkIds") List<Long> chunkIds);
}
