package com.tripdesigner.knowledge.resolver;

/**
 * 实体解析器接口。
 *
 * 将爬虫获取的文本名称（城市名、国家名、POI 名）解析为系统内部的实体 ID。
 * 解析仅做查找，不做创建——实体创建由 AppService 层负责。
 *
 * <p>解析流程：
 * <ol>
 *   <li>{@link #resolveCountry} — 国家名 → 国家 ID</li>
 *   <li>{@link #resolveCity} — 城市名 + 国家名 → 城市 ID</li>
 *   <li>{@link #resolvePoi} — POI 名 + 城市 ID → POI ID</li>
 * </ol>
 *
 * <p>未找到时返回 {@code null}，调用方可据此决定是否创建新实体。
 */
public interface EntityResolver {

    /**
     * 解析城市名称为城市实体 ID。
     *
     * @param cityName     城市名称
     * @param countryName  国家名称（用于消歧义）
     * @return 城市 ID；未找到时返回 null
     */
    Long resolveCity(String cityName, String countryName);

    /**
     * 解析国家名称为国家实体 ID。
     *
     * @param countryName 国家名称
     * @return 国家 ID；未找到时返回 null
     */
    Long resolveCountry(String countryName);

    /**
     * 解析 POI（兴趣点）名称为 POI 实体 ID。
     *
     * @param poiName POI 名称
     * @param cityId  所属城市 ID
     * @return POI ID；未找到时返回 null
     */
    Long resolvePoi(String poiName, Long cityId);
}
