package com.tripdesigner.knowledge.domain.repository;

import com.tripdesigner.knowledge.domain.Poi;

import java.util.List;
import java.util.Optional;

/**
 * 景点（POI）仓储接口。
 *
 * 定义景点知识实体的持久化操作。
 * 实现在 infrastructure 层（PoiRepositoryImpl），使用 MyBatis Plus。
 */
public interface PoiRepository {

    /** 按主键查询 */
    Optional<Poi> findById(Long id);

    /** 按城市 ID 查询景点列表 */
    List<Poi> findByCityId(Long cityId);

    /** 按分类查询景点列表 */
    List<Poi> findByCategory(String category);

    /** 保存（新增或更新） */
    Poi save(Poi poi);

    /** 按数据来源与源 ID 查询 */
    Optional<Poi> findBySource(String source, String sourceId);
}
