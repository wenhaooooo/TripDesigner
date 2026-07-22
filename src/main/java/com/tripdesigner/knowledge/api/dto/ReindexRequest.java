package com.tripdesigner.knowledge.api.dto;

/**
 * 知识库重建索引请求 DTO。
 *
 * 用于触发知识块的重新分块和向量化。支持按实体类型和实体 ID 精确重建，
 * 也可不指定参数对全量数据进行重建。force 参数用于强制重建（即使内容未变更）。
 *
 * @param entityType 实体类型：COUNTRY, CITY, POI, RESTAURANT, HOTEL, TRAVEL_GUIDE, ROUTE（可空，为空则重建全部类型）
 * @param entityId   实体 ID（可空，为空则重建指定类型的全部实体）
 * @param force      是否强制重建（即使内容未变更也重新分块和向量化）
 */
public record ReindexRequest(
        String entityType,
        Long entityId,
        boolean force
) {
}
