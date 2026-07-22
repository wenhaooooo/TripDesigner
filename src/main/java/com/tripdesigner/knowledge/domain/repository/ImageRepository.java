package com.tripdesigner.knowledge.domain.repository;

import com.tripdesigner.knowledge.domain.Image;

import java.util.List;

/**
 * 图片仓储接口。
 *
 * 定义知识库关联图片的持久化操作。
 * 实现在 infrastructure 层（ImageRepositoryImpl），使用 MyBatis Plus。
 */
public interface ImageRepository {

    /** 按实体类型与实体 ID 查询关联图片列表 */
    List<Image> findByEntityTypeAndEntityId(String entityType, Long entityId);

    /** 保存（新增或更新） */
    Image save(Image image);
}
