package com.tripdesigner.knowledge.agent;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tripdesigner.knowledge.api.vo.PoiVo;
import com.tripdesigner.knowledge.domain.City;
import com.tripdesigner.knowledge.domain.Country;
import com.tripdesigner.knowledge.domain.Poi;
import com.tripdesigner.knowledge.domain.repository.CityRepository;
import com.tripdesigner.knowledge.domain.repository.CountryRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库 Agent 集成服务。
 *
 * 这是知识库与 AI Agent 之间的主要集成点，为 {@code PlannerAgent}、{@code SightseeingAgent}、
 * {@code DiningAgent}、{@code AccommodationAgent}、{@code TransportAgent} 等 Agent 提供统一的
 * 知识检索接口。
 *
 * <p>每个方法遵循统一的检索策略：
 * <ol>
 *   <li>优先通过 RAG（{@link RagRetrievalService}）进行语义检索，获取最相关的知识块</li>
 *   <li>若 RAG 无结果，回退到数据库直接查询（DB fallback），确保结果可用性</li>
 *   <li>将结果格式化为 Agent 可直接使用的形式（VO 列表或上下文字符串）</li>
 * </ol>
 *
 * <p>这种 RAG-first + DB-fallback 的策略既保证了检索的精准性（RAG），
 * 又保证了结果的覆盖度（DB），是 Agent 知识获取的推荐模式。
 *
 * <p>注意：Restaurant / Hotel / Route / TravelGuide 实体暂无 Repository 接口，
 * 此处直接使用 MyBatis Plus Mapper（{@link RestaurantMapper} 等）进行数据库查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeAgentService {

    private final RagRetrievalService ragRetrievalService;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final PoiRepository poiRepository;

    // 以下实体暂无 Repository 接口，直接使用 MyBatis Plus Mapper
    private final RestaurantMapper restaurantMapper;
    private final HotelMapper hotelMapper;
    private final RouteMapper routeMapper;
    private final TravelGuideMapper travelGuideMapper;

    /** Agent 检索默认的 topK 值 */
    private static final int DEFAULT_AGENT_TOP_K = 5;
    /** Agent 检索默认的相似度阈值 */
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.5;
    /** 上下文字符串中单条知识的最大长度 */
    private static final int MAX_CONTEXT_ITEM_LENGTH = 500;

    /**
     * 为指定目的地构建知识上下文（供 PlannerAgent 使用）。
     *
     * 检索策略：
     * 1. 通过 RAG 检索与 userRequest 相关的目的地知识
     * 2. 若 RAG 无结果，从数据库查询目的地（城市/国家）的基本信息
     * 3. 将结果格式化为可注入 Agent 提示词的上下文字符串
     *
     * @param destination 目的地名称（如 "Paris"、"France"）
     * @param userRequest 用户的旅行需求描述
     * @return 格式化的知识上下文字符串，若无可用知识则返回 null
     */
    @Transactional(readOnly = true)
    public String buildDestinationContext(String destination, String userRequest) {
        if (destination == null || destination.isBlank()) {
            return null;
        }

        log.debug("[KnowledgeAgentService] buildDestinationContext: dest='{}', requestLen={}",
                destination, userRequest != null ? userRequest.length() : 0);

        // 1. RAG 检索
        String ragContext = buildRagContext(userRequest, null, null, DEFAULT_AGENT_TOP_K);
        if (ragContext != null) {
            log.info("[KnowledgeAgentService] Built destination context from RAG: dest='{}'", destination);
            return ragContext;
        }

        // 2. DB 回退：尝试按城市名查询
        String dbContext = buildDbContextFromDestination(destination);
        if (dbContext != null) {
            log.info("[KnowledgeAgentService] Built destination context from DB: dest='{}'", destination);
            return dbContext;
        }

        log.warn("[KnowledgeAgentService] No knowledge found for destination: {}", destination);
        return null;
    }

    /**
     * 获取 POI 推荐（供 SightseeingAgent 使用）。
     *
     * 检索策略：
     * 1. 通过 RAG 检索与城市和类别相关的 POI 知识块
     * 2. 从 RAG 结果中提取 POI ID，从数据库加载完整 POI 信息
     * 3. 若 RAG 无结果，直接从数据库按城市和类别查询，按评分降序排列
     *
     * @param cityId   城市 ID
     * @param category POI 类别（可空，为空则不限类别）
     * @param limit    返回数量上限
     * @return POI 视图对象列表
     */
    @Transactional(readOnly = true)
    public List<PoiVo> recommendPois(Long cityId, String category, int limit) {
        log.debug("[KnowledgeAgentService] recommendPois: cityId={}, category={}, limit={}", cityId, category, limit);

        // 1. RAG 检索
        String query = buildPoiQuery(cityId, category);
        SearchFilters filters = buildFilters("POI", cityId, category, null, limit);
        List<RagSearchResult> ragResults = ragRetrievalService.search(query, filters);

        if (ragResults != null && !ragResults.isEmpty()) {
            List<PoiVo> pois = loadPoisFromRagResults(ragResults, limit);
            if (!pois.isEmpty()) {
                log.info("[KnowledgeAgentService] recommendPois from RAG: cityId={}, count={}", cityId, pois.size());
                return pois;
            }
        }

        // 2. DB 回退
        List<PoiVo> dbPois = loadPoisFromDb(cityId, category, limit);
        log.info("[KnowledgeAgentService] recommendPois from DB: cityId={}, count={}", cityId, dbPois.size());
        return dbPois;
    }

    /**
     * 获取餐厅推荐（供 DiningAgent 使用）。
     *
     * 检索策略：
     * 1. 通过 RAG 检索与城市和菜系相关的餐厅知识块
     * 2. 从 RAG 结果中提取餐厅 ID，从数据库加载完整餐厅信息
     * 3. 若 RAG 无结果，直接从数据库按城市和菜系查询
     *
     * @param cityId      城市 ID
     * @param cuisineType 菜系类型（可空，为空则不限菜系）
     * @param limit       返回数量上限
     * @return 餐厅持久化对象列表
     */
    @Transactional(readOnly = true)
    public List<RestaurantPO> recommendRestaurants(Long cityId, String cuisineType, int limit) {
        log.debug("[KnowledgeAgentService] recommendRestaurants: cityId={}, cuisine={}, limit={}",
                cityId, cuisineType, limit);

        // 1. RAG 检索
        String query = buildRestaurantQuery(cityId, cuisineType);
        SearchFilters filters = buildFilters("RESTAURANT", cityId, cuisineType, null, limit);
        List<RagSearchResult> ragResults = ragRetrievalService.search(query, filters);

        if (ragResults != null && !ragResults.isEmpty()) {
            List<RestaurantPO> restaurants = loadRestaurantsFromRagResults(ragResults, limit);
            if (!restaurants.isEmpty()) {
                log.info("[KnowledgeAgentService] recommendRestaurants from RAG: cityId={}, count={}",
                        cityId, restaurants.size());
                return restaurants;
            }
        }

        // 2. DB 回退
        List<RestaurantPO> all = restaurantMapper.selectList(
                Wrappers.<RestaurantPO>lambdaQuery()
                        .eq(RestaurantPO::getCityId, cityId)
                        .eq(cuisineType != null, RestaurantPO::getCuisineType, cuisineType));
        List<RestaurantPO> dbRestaurants = all.stream()
                .limit(limit)
                .collect(Collectors.toList());
        log.info("[KnowledgeAgentService] recommendRestaurants from DB: cityId={}, count={}",
                cityId, dbRestaurants.size());
        return dbRestaurants;
    }

    /**
     * 获取酒店推荐（供 AccommodationAgent 使用）。
     *
     * 检索策略：
     * 1. 通过 RAG 检索与城市和类别相关的酒店知识块
     * 2. 从 RAG 结果中提取酒店 ID，从数据库加载完整酒店信息
     * 3. 若 RAG 无结果，直接从数据库按城市和类别查询
     *
     * @param cityId   城市 ID
     * @param category 酒店类别（可空，如 "luxury", "budget"）
     * @param limit    返回数量上限
     * @return 酒店持久化对象列表
     */
    @Transactional(readOnly = true)
    public List<HotelPO> recommendHotels(Long cityId, String category, int limit) {
        log.debug("[KnowledgeAgentService] recommendHotels: cityId={}, category={}, limit={}",
                cityId, category, limit);

        // 1. RAG 检索
        String query = buildHotelQuery(cityId, category);
        SearchFilters filters = buildFilters("HOTEL", cityId, category, null, limit);
        List<RagSearchResult> ragResults = ragRetrievalService.search(query, filters);

        if (ragResults != null && !ragResults.isEmpty()) {
            List<HotelPO> hotels = loadHotelsFromRagResults(ragResults, limit);
            if (!hotels.isEmpty()) {
                log.info("[KnowledgeAgentService] recommendHotels from RAG: cityId={}, count={}",
                        cityId, hotels.size());
                return hotels;
            }
        }

        // 2. DB 回退
        List<HotelPO> all = hotelMapper.selectList(
                Wrappers.<HotelPO>lambdaQuery()
                        .eq(HotelPO::getCityId, cityId)
                        .eq(category != null, HotelPO::getCategory, category));
        List<HotelPO> dbHotels = all.stream()
                .limit(limit)
                .collect(Collectors.toList());
        log.info("[KnowledgeAgentService] recommendHotels from DB: cityId={}, count={}",
                cityId, dbHotels.size());
        return dbHotels;
    }

    /**
     * 获取两城市间的交通路线信息（供 TransportAgent 使用）。
     *
     * 直接从数据库查询路线信息，因为路线数据通常是结构化的（不依赖 RAG 检索）。
     *
     * @param fromCityId 出发城市 ID
     * @param toCityId   到达城市 ID
     * @return 路线持久化对象列表
     */
    @Transactional(readOnly = true)
    public List<RoutePO> getRoutes(Long fromCityId, Long toCityId) {
        log.debug("[KnowledgeAgentService] getRoutes: from={}, to={}", fromCityId, toCityId);

        List<RoutePO> routes = routeMapper.selectList(
                Wrappers.<RoutePO>lambdaQuery()
                        .eq(RoutePO::getFromCityId, fromCityId)
                        .eq(RoutePO::getToCityId, toCityId));
        log.info("[KnowledgeAgentService] getRoutes: from={}, to={}, count={}",
                fromCityId, toCityId, routes.size());
        return routes;
    }

    /**
     * 获取城市的旅行攻略（供 PlannerAgent / SightseeingAgent 使用）。
     *
     * 检索策略：
     * 1. 通过 RAG 检索与城市相关的旅行攻略知识块
     * 2. 若 RAG 无结果，从数据库查询城市的旅行攻略
     * 3. 将结果格式化为可注入 Agent 提示词的上下文字符串
     *
     * @param cityId   城市 ID
     * @param language 语言代码（如 "en", "zh"）
     * @return 格式化的旅行攻略字符串，若无则返回 null
     */
    @Transactional(readOnly = true)
    public String getTravelGuide(Long cityId, String language) {
        log.debug("[KnowledgeAgentService] getTravelGuide: cityId={}, language={}", cityId, language);

        String cityName = cityRepository.findById(cityId)
                .map(City::getName)
                .orElse("city_" + cityId);

        // 1. RAG 检索
        String query = "Travel guide for " + cityName;
        SearchFilters filters = buildFilters("TRAVEL_GUIDE", cityId, null, language, DEFAULT_AGENT_TOP_K);
        List<RagSearchResult> ragResults = ragRetrievalService.search(query, filters);

        if (ragResults != null && !ragResults.isEmpty()) {
            String context = formatRagResultsAsContext(ragResults, "Travel Guide for " + cityName);
            log.info("[KnowledgeAgentService] getTravelGuide from RAG: cityId={}, count={}",
                    cityId, ragResults.size());
            return context;
        }

        // 2. DB 回退
        List<TravelGuidePO> guides = travelGuideMapper.selectList(
                Wrappers.<TravelGuidePO>lambdaQuery()
                        .eq(TravelGuidePO::getCityId, cityId)
                        .eq(language != null, TravelGuidePO::getLanguage, language));
        String guide = guides.stream()
                .findFirst()
                .map(TravelGuidePO::getContent)
                .orElse(null);
        if (guide != null) {
            log.info("[KnowledgeAgentService] getTravelGuide from DB: cityId={}", cityId);
            return guide;
        }

        log.warn("[KnowledgeAgentService] No travel guide found: cityId={}, language={}", cityId, language);
        return null;
    }

    /**
     * 通用知识库检索（供任意 Agent 使用）。
     *
     * 直接委托给 {@link RagRetrievalService} 进行语义检索，
     * 返回原始的 {@link RagSearchResult} 列表，由调用方自行解析。
     *
     * @param query      查询文本
     * @param entityType 实体类型过滤（可空）
     * @param topK       返回结果数
     * @return RAG 检索结果列表
     */
    @Transactional(readOnly = true)
    public List<RagSearchResult> searchForAgent(String query, String entityType, int topK) {
        log.debug("[KnowledgeAgentService] searchForAgent: query='{}', entityType={}, topK={}",
                query, entityType, topK);

        SearchFilters filters = buildFilters(entityType, null, null, null, topK);
        List<RagSearchResult> results = ragRetrievalService.search(query, filters);

        if (results == null || results.isEmpty()) {
            log.debug("[KnowledgeAgentService] searchForAgent: no results for query='{}'", query);
            return Collections.emptyList();
        }

        log.info("[KnowledgeAgentService] searchForAgent: query='{}', count={}", query, results.size());
        return results;
    }

    // ========== RAG 上下文构建 ==========

    /**
     * 通过 RAG 检索结果构建上下文字符串。
     */
    private String buildRagContext(String query, String entityType, Long cityId, int topK) {
        SearchFilters filters = buildFilters(entityType, cityId, null, null, topK);
        List<RagSearchResult> results = ragRetrievalService.search(query, filters);

        if (results == null || results.isEmpty()) {
            return null;
        }

        return formatRagResultsAsContext(results, "Destination Knowledge (RAG retrieved)");
    }

    /**
     * 从数据库构建目的地上下文（RAG 无结果时的回退方案）。
     * 尝试按城市名查询，若无则按国家名查询。
     */
    private String buildDbContextFromDestination(String destination) {
        // 尝试按城市名查询
        List<City> cities = cityRepository.findByName(destination);
        if (!cities.isEmpty()) {
            return formatCitiesAsContext(cities);
        }

        // 尝试按国家名查询
        List<Country> countries = countryRepository.findByName(destination);
        if (!countries.isEmpty()) {
            return formatCountriesAsContext(countries);
        }

        return null;
    }

    // ========== RAG 结果加载实体 ==========

    /**
     * 从 RAG 检索结果中提取 POI ID 并加载完整的 POI 信息。
     */
    private List<PoiVo> loadPoisFromRagResults(List<RagSearchResult> results, int limit) {
        List<PoiVo> pois = new ArrayList<>();
        for (RagSearchResult r : results) {
            if (r.entityId() == null) continue;
            poiRepository.findById(r.entityId()).ifPresent(poi -> {
                pois.add(toPoiVo(poi));
            });
            if (pois.size() >= limit) break;
        }
        return pois;
    }

    /**
     * 从 RAG 检索结果中提取餐厅 ID 并加载完整的餐厅信息。
     */
    private List<RestaurantPO> loadRestaurantsFromRagResults(List<RagSearchResult> results, int limit) {
        List<RestaurantPO> restaurants = new ArrayList<>();
        for (RagSearchResult r : results) {
            if (r.entityId() == null) continue;
            RestaurantPO restaurant = restaurantMapper.selectById(r.entityId());
            if (restaurant != null) {
                restaurants.add(restaurant);
            }
            if (restaurants.size() >= limit) break;
        }
        return restaurants;
    }

    /**
     * 从 RAG 检索结果中提取酒店 ID 并加载完整的酒店信息。
     */
    private List<HotelPO> loadHotelsFromRagResults(List<RagSearchResult> results, int limit) {
        List<HotelPO> hotels = new ArrayList<>();
        for (RagSearchResult r : results) {
            if (r.entityId() == null) continue;
            HotelPO hotel = hotelMapper.selectById(r.entityId());
            if (hotel != null) {
                hotels.add(hotel);
            }
            if (hotels.size() >= limit) break;
        }
        return hotels;
    }

    // ========== DB 回退加载 ==========

    /**
     * 从数据库直接加载 POI（按城市和类别过滤）。
     * 仓储接口不支持分页，在此进行内存分页。
     */
    private List<PoiVo> loadPoisFromDb(Long cityId, String category, int limit) {
        List<Poi> pois;
        if (cityId != null) {
            pois = poiRepository.findByCityId(cityId);
            if (category != null) {
                pois = pois.stream()
                        .filter(p -> category.equals(p.getCategory()))
                        .collect(Collectors.toList());
            }
        } else if (category != null) {
            pois = poiRepository.findByCategory(category);
        } else {
            pois = List.of();
        }
        return pois.stream()
                .limit(limit)
                .map(this::toPoiVo)
                .collect(Collectors.toList());
    }

    // ========== 格式化方法 ==========

    /**
     * 将 RAG 检索结果格式化为上下文字符串。
     */
    private String formatRagResultsAsContext(List<RagSearchResult> results, String header) {
        StringBuilder sb = new StringBuilder();
        sb.append(header).append(":\n");
        for (int i = 0; i < results.size(); i++) {
            RagSearchResult r = results.get(i);
            sb.append(i + 1).append(". ");
            if (r.title() != null && !r.title().isBlank()) {
                sb.append(r.title()).append(": ");
            }
            String content = r.content();
            if (content != null && content.length() > MAX_CONTEXT_ITEM_LENGTH) {
                content = content.substring(0, MAX_CONTEXT_ITEM_LENGTH) + "...";
            }
            sb.append(content).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * 将城市列表格式化为上下文字符串。
     */
    private String formatCitiesAsContext(List<City> cities) {
        StringBuilder sb = new StringBuilder();
        sb.append("Destination Knowledge (from database):\n");
        for (City city : cities) {
            sb.append("- City: ").append(city.getName());
            if (city.getNameLocal() != null && !city.getNameLocal().equals(city.getName())) {
                sb.append(" (").append(city.getNameLocal()).append(")");
            }
            if (city.getTimezone() != null) sb.append(", Timezone: ").append(city.getTimezone());
            if (city.getPopulation() != null) sb.append(", Population: ").append(city.getPopulation());
            if (city.getLatitude() != null && city.getLongitude() != null) {
                sb.append(", Coordinates: ").append(city.getLatitude()).append(",").append(city.getLongitude());
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * 将国家列表格式化为上下文字符串。
     */
    private String formatCountriesAsContext(List<Country> countries) {
        StringBuilder sb = new StringBuilder();
        sb.append("Destination Knowledge (from database):\n");
        for (Country country : countries) {
            sb.append("- Country: ").append(country.getName());
            if (country.getIsoCode2() != null) sb.append(" (").append(country.getIsoCode2()).append(")");
            if (country.getContinent() != null) sb.append(", Continent: ").append(country.getContinent());
            if (country.getCapital() != null) sb.append(", Capital: ").append(country.getCapital());
            if (country.getCurrencyCode() != null) sb.append(", Currency: ").append(country.getCurrencyCode());
            if (country.getLanguages() != null && !country.getLanguages().isEmpty()) {
                sb.append(", Languages: ").append(String.join(", ", country.getLanguages()));
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    // ========== 查询构建 ==========

    private String buildPoiQuery(Long cityId, String category) {
        String cityName = cityId != null
                ? cityRepository.findById(cityId).map(City::getName).orElse("city_" + cityId)
                : "";
        String categoryPart = category != null ? category + " " : "";
        return "Recommend " + categoryPart + "attractions in " + cityName;
    }

    private String buildRestaurantQuery(Long cityId, String cuisineType) {
        String cityName = cityId != null
                ? cityRepository.findById(cityId).map(City::getName).orElse("city_" + cityId)
                : "";
        String cuisinePart = cuisineType != null ? cuisineType + " " : "";
        return "Best " + cuisinePart + "restaurants in " + cityName;
    }

    private String buildHotelQuery(Long cityId, String category) {
        String cityName = cityId != null
                ? cityRepository.findById(cityId).map(City::getName).orElse("city_" + cityId)
                : "";
        String categoryPart = category != null ? category + " " : "";
        return categoryPart + "hotels in " + cityName;
    }

    // ========== 辅助方法 ==========

    /**
     * 构建 {@link SearchFilters} 对象，统一设置默认相似度阈值。
     *
     * @param entityType 实体类型（可空）
     * @param cityId     城市 ID（可空）
     * @param category   类别（可空）
     * @param language   语言（可空）
     * @param topK       返回结果数
     * @return 构建好的 SearchFilters
     */
    private SearchFilters buildFilters(String entityType, Long cityId, String category,
                                       String language, int topK) {
        return SearchFilters.builder()
                .entityType(entityType)
                .cityId(cityId)
                .category(category)
                .language(language)
                .topK(topK)
                .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD)
                .build();
    }

    /**
     * 将 POI 实体转换为 VO（Agent 内部使用，cityName 从城市仓储查询）。
     */
    private PoiVo toPoiVo(Poi p) {
        String cityName = p.getCityId() != null
                ? cityRepository.findById(p.getCityId()).map(City::getName).orElse(null)
                : null;
        return new PoiVo(
                p.getId(), p.getCityId(), cityName, p.getName(), p.getNameLocal(),
                p.getCategory(), p.getSubcategory(), p.getDescription(),
                p.getLatitude(), p.getLongitude(), p.getAddress(),
                p.getRating(), p.getReviewCount(), p.getSource(), p.getLastSyncedAt());
    }
}
