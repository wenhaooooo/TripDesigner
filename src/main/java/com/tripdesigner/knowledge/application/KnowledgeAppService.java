package com.tripdesigner.knowledge.application;

import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.PageResult;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.knowledge.api.vo.CityVo;
import com.tripdesigner.knowledge.api.vo.CountryVo;
import com.tripdesigner.knowledge.api.vo.KnowledgeStatsVo;
import com.tripdesigner.knowledge.api.vo.PoiVo;
import com.tripdesigner.knowledge.api.vo.SearchResultVo;
import com.tripdesigner.knowledge.domain.City;
import com.tripdesigner.knowledge.domain.Country;
import com.tripdesigner.knowledge.domain.KnowledgeRelation;
import com.tripdesigner.knowledge.domain.Poi;
import com.tripdesigner.knowledge.domain.repository.CityRepository;
import com.tripdesigner.knowledge.domain.repository.CountryRepository;
import com.tripdesigner.knowledge.domain.repository.KnowledgeChunkRepository;
import com.tripdesigner.knowledge.domain.repository.KnowledgeRelationRepository;
import com.tripdesigner.knowledge.domain.repository.PoiRepository;
import com.tripdesigner.knowledge.infrastructure.mapper.HotelMapper;
import com.tripdesigner.knowledge.infrastructure.mapper.RestaurantMapper;
import com.tripdesigner.knowledge.infrastructure.mapper.RouteMapper;
import com.tripdesigner.knowledge.infrastructure.mapper.TravelGuideMapper;
import com.tripdesigner.knowledge.infrastructure.po.HotelPO;
import com.tripdesigner.knowledge.infrastructure.po.RestaurantPO;
import com.tripdesigner.knowledge.infrastructure.po.RoutePO;
import com.tripdesigner.knowledge.infrastructure.po.TravelGuidePO;
import com.tripdesigner.knowledge.rag.RagRetrievalService;
import com.tripdesigner.knowledge.rag.RagSearchResult;
import com.tripdesigner.knowledge.rag.SearchFilters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识库查询应用服务。
 *
 * 提供知识库的查询能力，包括：
 * <ul>
 *   <li>语义检索（search）：委托给 {@link RagRetrievalService} 执行向量相似度检索</li>
 *   <li>实体查询（getPoi/getCity/getCountry）：按 ID 查询结构化实体</li>
 *   <li>列表查询（listCities/listPois）：分页查询实体列表，支持条件过滤</li>
 *   <li>统计分析（getStats）：统计各实体类型的数量和数据源分布</li>
 *   <li>知识图谱（getRelations）：查询实体间的关联关系</li>
 * </ul>
 *
 * 所有查询方法均为只读事务，不修改数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeAppService {

    private final RagRetrievalService ragRetrievalService;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final PoiRepository poiRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeRelationRepository knowledgeRelationRepository;

    // 以下实体暂无 Repository 接口，直接使用 MyBatis Plus Mapper
    private final RestaurantMapper restaurantMapper;
    private final HotelMapper hotelMapper;
    private final RouteMapper routeMapper;
    private final TravelGuideMapper travelGuideMapper;

    /**
     * 语义检索知识库。
     * 委托给 {@link RagRetrievalService} 执行向量相似度检索，根据 useMmr 和 useHybridSearch
     * 选择合适的检索策略，将 {@link RagSearchResult} 转换为 {@link SearchResultVo} 返回。
     *
     * @param query               查询文本
     * @param entityType          实体类型过滤（可空）
     * @param cityId              城市 ID 过滤（可空）
     * @param countryId           国家 ID 过滤（可空）
     * @param category            类别过滤（可空）
     * @param language            语言
     * @param topK                返回结果数
     * @param similarityThreshold 相似度阈值
     * @param useMmr              是否启用 MMR 去重
     * @param useHybridSearch     是否启用混合检索
     * @return 检索结果列表
     */
    @Transactional(readOnly = true)
    public List<SearchResultVo> search(String query, String entityType, Long cityId, Long countryId,
                                       String category, String language, Integer topK,
                                       Double similarityThreshold, boolean useMmr, boolean useHybridSearch) {
        log.debug("[KnowledgeAppService] search: query='{}', entityType={}, topK={}", query, entityType, topK);

        SearchFilters filters = SearchFilters.builder()
                .entityType(entityType)
                .cityId(cityId)
                .countryId(countryId)
                .category(category)
                .language(language)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .useMmr(useMmr)
                .build();

        List<RagSearchResult> results;
        if (useHybridSearch) {
            results = ragRetrievalService.hybridSearch(query, filters);
        } else if (useMmr) {
            results = ragRetrievalService.mmrSearch(query, filters);
        } else {
            results = ragRetrievalService.search(query, filters);
        }

        return results.stream()
                .map(this::toSearchResultVo)
                .collect(Collectors.toList());
    }

    /**
     * 按 ID 查询 POI 详情。
     *
     * @param id POI ID
     * @return POI 视图对象
     * @throws BizException 当 POI 不存在时抛出 COMMON_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public PoiVo getPoi(Long id) {
        Poi poi = poiRepository.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.COMMON_NOT_FOUND, "POI not found: " + id));
        return toPoiVo(poi);
    }

    /**
     * 按 ID 查询城市详情。
     *
     * @param id 城市 ID
     * @return 城市视图对象
     * @throws BizException 当城市不存在时抛出 COMMON_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public CityVo getCity(Long id) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.COMMON_NOT_FOUND, "City not found: " + id));
        return toCityVo(city);
    }

    /**
     * 按 ID 查询国家详情。
     *
     * @param id 国家 ID
     * @return 国家视图对象
     * @throws BizException 当国家不存在时抛出 COMMON_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public CountryVo getCountry(Long id) {
        Country country = countryRepository.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.COMMON_NOT_FOUND, "Country not found: " + id));
        return toCountryVo(country);
    }

    /**
     * 分页查询城市列表。
     * 可按国家 ID 过滤。由于仓储接口不支持分页，在此进行内存分页。
     *
     * @param countryId 国家 ID（可空，为空则查询全部）
     * @param page      页码（从 0 开始）
     * @param size      每页大小
     * @return 分页城市列表
     */
    @Transactional(readOnly = true)
    public PageResult<CityVo> listCities(Long countryId, int page, int size) {
        List<City> allCities = (countryId != null)
                ? cityRepository.findByCountryId(countryId)
                : List.of();

        long total = allCities.size();
        List<CityVo> records = allCities.stream()
                .skip((long) page * size)
                .limit(size)
                .map(this::toCityVo)
                .collect(Collectors.toList());
        return new PageResult<>(page, size, total, records);
    }

    /**
     * 分页查询 POI 列表。
     * 可按城市 ID 和类别过滤。由于仓储接口不支持分页，在此进行内存分页。
     *
     * @param cityId   城市 ID（可空）
     * @param category 类别（可空）
     * @param page     页码（从 0 开始）
     * @param size     每页大小
     * @return 分页 POI 列表
     */
    @Transactional(readOnly = true)
    public PageResult<PoiVo> listPois(Long cityId, String category, int page, int size) {
        List<Poi> allPois;
        if (cityId != null) {
            allPois = poiRepository.findByCityId(cityId);
            if (category != null) {
                allPois = allPois.stream()
                        .filter(p -> category.equals(p.getCategory()))
                        .collect(Collectors.toList());
            }
        } else if (category != null) {
            allPois = poiRepository.findByCategory(category);
        } else {
            allPois = List.of();
        }

        long total = allPois.size();
        List<PoiVo> records = allPois.stream()
                .skip((long) page * size)
                .limit(size)
                .map(this::toPoiVo)
                .collect(Collectors.toList());
        return new PageResult<>(page, size, total, records);
    }

    /**
     * 获取知识库统计信息。
     * 统计各实体类型的总数，以及按数据源分布的知识块数量。
     *
     * @return 知识库统计信息
     */
    @Transactional(readOnly = true)
    public KnowledgeStatsVo getStats() {
        long totalCountries = countryRepository.findAll().size();
        long totalCities = 0;
        long totalPois = 0;
        long totalRestaurants = restaurantMapper.selectCount(null);
        long totalHotels = hotelMapper.selectCount(null);
        long totalGuides = travelGuideMapper.selectCount(null);
        long totalRoutes = routeMapper.selectCount(null);

        // 城市：遍历所有国家累加（仓储不支持 countAll）
        for (Country country : countryRepository.findAll()) {
            totalCities += cityRepository.findByCountryId(country.getId()).size();
        }

        // POI：遍历所有城市累加
        for (Country country : countryRepository.findAll()) {
            for (City city : cityRepository.findByCountryId(country.getId())) {
                totalPois += poiRepository.findByCityId(city.getId()).size();
            }
        }

        long totalChunks = knowledgeChunkRepository.vectorSearchAll(new float[0], 0).size();

        return new KnowledgeStatsVo(
                totalCountries, totalCities, totalPois, totalRestaurants,
                totalHotels, totalGuides, totalChunks, totalRoutes, Map.of());
    }

    /**
     * 查询知识图谱中某个实体的关联关系。
     * 返回该实体作为起始或目标的所有关联关系。
     *
     * @param entityType 实体类型
     * @param entityId   实体 ID
     * @return 关联关系列表，每项为一个 Map
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRelations(String entityType, Long entityId) {
        List<KnowledgeRelation> fromRelations = knowledgeRelationRepository.findByFromEntity(entityType, entityId);
        List<KnowledgeRelation> toRelations = knowledgeRelationRepository.findByToEntity(entityType, entityId);

        List<KnowledgeRelation> all = new ArrayList<>(fromRelations);
        all.addAll(toRelations);

        return all.stream()
                .map(this::toRelationMap)
                .collect(Collectors.toList());
    }

    // ========== 实体转 VO ==========

    /**
     * 将 {@link RagSearchResult} 转换为 {@link SearchResultVo}。
     * chunkType 从 metadata 中提取（RagSearchResult 本身不含 chunkType 字段）。
     */
    private SearchResultVo toSearchResultVo(RagSearchResult result) {
        String chunkType = result.metadata() != null
                ? (String) result.metadata().get("chunk_type")
                : null;
        return new SearchResultVo(
                result.chunkId(),
                result.entityType(),
                result.entityId(),
                result.title(),
                result.content(),
                result.score(),
                chunkType,
                result.metadata()
        );
    }

    /**
     * 将 {@link Country} 转换为 {@link CountryVo}。
     */
    private CountryVo toCountryVo(Country c) {
        return new CountryVo(
                c.getId(), c.getName(), c.getIsoCode2(), c.getIsoCode3(),
                c.getContinent(), c.getCapital(), c.getCurrencyCode(),
                c.getLanguages(), c.getSource(), c.getLastSyncedAt());
    }

    /**
     * 将 {@link City} 转换为 {@link CityVo}。
     * countryName 通过 countryRepository 查询填充。
     */
    private CityVo toCityVo(City c) {
        String countryName = c.getCountryId() != null
                ? countryRepository.findById(c.getCountryId())
                    .map(Country::getName)
                    .orElse(null)
                : null;
        return new CityVo(
                c.getId(), c.getCountryId(), countryName, c.getName(), c.getNameLocal(),
                c.getTimezone(), c.getPopulation(), c.getLatitude(), c.getLongitude(),
                c.getSource(), c.getLastSyncedAt());
    }

    /**
     * 将 {@link Poi} 转换为 {@link PoiVo}。
     * cityName 通过 cityRepository 查询填充。
     */
    private PoiVo toPoiVo(Poi p) {
        String cityName = p.getCityId() != null
                ? cityRepository.findById(p.getCityId())
                    .map(City::getName)
                    .orElse(null)
                : null;
        return new PoiVo(
                p.getId(), p.getCityId(), cityName, p.getName(), p.getNameLocal(),
                p.getCategory(), p.getSubcategory(), p.getDescription(),
                p.getLatitude(), p.getLongitude(), p.getAddress(),
                p.getRating(), p.getReviewCount(), p.getSource(), p.getLastSyncedAt());
    }

    /**
     * 将 {@link KnowledgeRelation} 转换为 Map 形式。
     */
    private Map<String, Object> toRelationMap(KnowledgeRelation r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("relationType", r.getRelationType());
        map.put("fromEntityType", r.getFromEntityType());
        map.put("fromEntityId", r.getFromEntityId());
        map.put("toEntityType", r.getToEntityType());
        map.put("toEntityId", r.getToEntityId());
        map.put("weight", r.getWeight());
        map.put("metadata", r.getMetadata());
        return map;
    }
}
