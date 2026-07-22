package com.tripdesigner.knowledge.application;

import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.knowledge.api.dto.CrawlRequest;
import com.tripdesigner.knowledge.api.dto.EmbedRequest;
import com.tripdesigner.knowledge.api.dto.ReindexRequest;
import com.tripdesigner.knowledge.api.vo.CrawlResultVo;
import com.tripdesigner.knowledge.crawler.DataCrawler;
import com.tripdesigner.knowledge.domain.City;
import com.tripdesigner.knowledge.domain.Country;
import com.tripdesigner.knowledge.domain.KnowledgeSource;
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
import com.tripdesigner.knowledge.pipeline.KnowledgePipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 知识库抓取与索引应用服务。
 *
 * 提供知识库的写入能力，包括：
 * <ul>
 *   <li>抓取（crawl）：根据数据源选择对应的 {@link DataCrawler}，执行抓取-解析-索引流水线</li>
 *   <li>手动向量化（embed）：将外部提交的文本内容向量化并写入知识库</li>
 *   <li>重建索引（reindex）：对已有实体重新分块和向量化，用于内容更新或向量模型升级</li>
 * </ul>
 *
 * 抓取流水线由 {@link KnowledgePipeline} 编排，负责数据解析、清洗、分块、向量化和存储。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlAppService {

    private final List<DataCrawler> crawlers;
    private final KnowledgePipeline knowledgePipeline;
    private final KnowledgeIndexAppService knowledgeIndexAppService;

    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final PoiRepository poiRepository;

    // 以下实体暂无 Repository 接口，直接使用 MyBatis Plus Mapper
    private final RestaurantMapper restaurantMapper;
    private final HotelMapper hotelMapper;
    private final RouteMapper routeMapper;
    private final TravelGuideMapper travelGuideMapper;

    /**
     * 触发知识库抓取任务。
     *
     * 根据请求中的 source 字段选择对应的 {@link DataCrawler}（如 OSM、WIKIVOYAGE 等），
     * 调用其 crawl 方法获取原始数据，再通过 {@link KnowledgePipeline#processCrawlResult}
     * 执行解析-清洗-分块-向量化-存储的完整流水线。
     *
     * @param request 抓取请求
     * @return 抓取结果统计
     * @throws BizException 当指定的数据源没有对应的 Crawler 时抛出 COMMON_BAD_REQUEST
     */
    @Transactional
    public CrawlResultVo crawl(CrawlRequest request) {
        String source = request.source();
        DataCrawler crawler = crawlers.stream()
                .filter(c -> c.getSourceName().equalsIgnoreCase(source))
                .findFirst()
                .orElseThrow(() -> new BizException(ResultCode.COMMON_BAD_REQUEST,
                        "No crawler found for source: " + source
                                + ", available: " + crawlers.stream().map(DataCrawler::getSourceName).toList()));

        int limit = request.limit() != null ? request.limit() : 100;
        log.info("[CrawlAppService] Starting crawl: source={}, query={}, limit={}",
                source, request.query(), limit);

        try {
            // 1. 抓取原始数据
            List<Map<String, Object>> rawData = crawler.crawl(request.query(), limit);
            int crawled = rawData != null ? rawData.size() : 0;

            if (crawled == 0) {
                log.info("[CrawlAppService] No data crawled: source={}", source);
                return new CrawlResultVo(source, 0, 0, 0, 0, 0, "No data crawled", Instant.now());
            }

            // 2. 构建 KnowledgeSource 元信息
            KnowledgeSource knowledgeSource = KnowledgeSource.builder()
                    .sourceType(source)
                    .entityType(source.toUpperCase())
                    .status("PENDING")
                    .build();

            // 3. 执行流水线（解析 → 清洗 → 分块 → 向量化 → 存储）
            int indexed = knowledgePipeline.processCrawlResult(knowledgeSource, rawData);

            log.info("[CrawlAppService] Crawl completed: source={}, crawled={}, indexed={}",
                    source, crawled, indexed);

            return new CrawlResultVo(
                    source,
                    crawled,
                    crawled,
                    indexed,
                    0,
                    0,
                    "Crawl completed successfully",
                    Instant.now());
        } catch (Exception e) {
            log.error("[CrawlAppService] Crawl failed: source={}, error={}", source, e.getMessage(), e);
            return new CrawlResultVo(
                    source, 0, 0, 0, 0, 1,
                    "Crawl failed: " + e.getMessage(),
                    Instant.now());
        }
    }

    /**
     * 手动对一段文本内容进行向量化并写入知识库。
     *
     * 将 {@link EmbedRequest} 中的 content 通过 {@link KnowledgeIndexAppService#indexEntity}
     * 进行分块、向量化并存储。适用于管理员手动添加攻略、人工校对内容等场景。
     *
     * @param request 向量化请求
     */
    @Transactional
    public void embed(EmbedRequest request) {
        log.info("[CrawlAppService] Manual embed: entityType={}, entityId={}, contentLen={}",
                request.entityType(), request.entityId(),
                request.content() != null ? request.content().length() : 0);

        if (request.content() == null || request.content().isBlank()) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, "content is required");
        }

        int indexed = knowledgeIndexAppService.indexEntity(
                request.entityType(),
                request.entityId(),
                request.content());

        log.info("[CrawlAppService] Manual embed completed: entityType={}, entityId={}, chunks={}",
                request.entityType(), request.entityId(), indexed);
    }

    /**
     * 重建知识库索引。
     *
     * 对指定实体类型（或全部类型）的实体重新进行分块和向量化。
     * 当 force 为 true 时，即使内容未变更也会强制重建。
     *
     * @param request 重建索引请求
     */
    @Transactional
    public void reindex(ReindexRequest request) {
        String entityType = request.entityType();
        Long entityId = request.entityId();

        log.info("[CrawlAppService] Starting reindex: entityType={}, entityId={}, force={}",
                entityType, entityId, request.force());

        if (entityType != null && entityId != null) {
            // 重建单个实体
            reindexSingleEntity(entityType, entityId);
        } else if (entityType != null) {
            // 重建指定类型的所有实体
            reindexByType(entityType);
        } else {
            // 重建全部类型
            reindexByType("COUNTRY");
            reindexByType("CITY");
            reindexByType("POI");
            reindexByType("RESTAURANT");
            reindexByType("HOTEL");
            reindexByType("TRAVEL_GUIDE");
            reindexByType("ROUTE");
        }

        log.info("[CrawlAppService] Reindex completed: entityType={}, entityId={}", entityType, entityId);
    }

    /**
     * 重建单个实体的索引。
     */
    private void reindexSingleEntity(String entityType, Long entityId) {
        String content = buildContentByEntity(entityType, entityId);
        if (content != null && !content.isBlank()) {
            knowledgeIndexAppService.indexEntity(entityType, entityId, content);
        }
    }

    /**
     * 按实体类型批量重建索引。
     * 遍历所有该类型的实体，逐个重建索引。
     */
    private void reindexByType(String entityType) {
        int totalIndexed = 0;
        switch (entityType.toUpperCase()) {
            case "COUNTRY" -> {
                for (Country c : countryRepository.findAll()) {
                    totalIndexed += reindexEntity("COUNTRY", c.getId(), buildCountryContent(c));
                }
            }
            case "CITY" -> {
                for (Country country : countryRepository.findAll()) {
                    for (City city : cityRepository.findByCountryId(country.getId())) {
                        totalIndexed += reindexEntity("CITY", city.getId(), buildCityContent(city));
                    }
                }
            }
            case "POI" -> {
                for (Country country : countryRepository.findAll()) {
                    for (City city : cityRepository.findByCountryId(country.getId())) {
                        for (Poi poi : poiRepository.findByCityId(city.getId())) {
                            totalIndexed += reindexEntity("POI", poi.getId(), buildPoiContent(poi));
                        }
                    }
                }
            }
            case "RESTAURANT" -> {
                List<RestaurantPO> restaurants = restaurantMapper.selectList(null);
                for (RestaurantPO r : restaurants) {
                    totalIndexed += reindexEntity("RESTAURANT", r.getId(), buildRestaurantContent(r));
                }
            }
            case "HOTEL" -> {
                List<HotelPO> hotels = hotelMapper.selectList(null);
                for (HotelPO h : hotels) {
                    totalIndexed += reindexEntity("HOTEL", h.getId(), buildHotelContent(h));
                }
            }
            case "ROUTE" -> {
                List<RoutePO> routes = routeMapper.selectList(null);
                for (RoutePO r : routes) {
                    totalIndexed += reindexEntity("ROUTE", r.getId(), buildRouteContent(r));
                }
            }
            case "TRAVEL_GUIDE" -> {
                List<TravelGuidePO> guides = travelGuideMapper.selectList(null);
                for (TravelGuidePO g : guides) {
                    String content = g.getContent() != null ? g.getContent() : "";
                    totalIndexed += reindexEntity("TRAVEL_GUIDE", g.getId(), content);
                }
            }
            default -> log.warn("[CrawlAppService] Unknown entity type for reindex: {}", entityType);
        }
        log.info("[CrawlAppService] Reindexed {} chunks for type: {}", totalIndexed, entityType);
    }

    /**
     * 重建单个实体的索引并返回索引的知识块数量。
     */
    private int reindexEntity(String entityType, Long entityId, String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        return knowledgeIndexAppService.indexEntity(entityType, entityId, content);
    }

    /**
     * 根据实体类型和 ID 构建用于索引的文本内容。
     */
    private String buildContentByEntity(String entityType, Long entityId) {
        return switch (entityType.toUpperCase()) {
            case "COUNTRY" -> countryRepository.findById(entityId)
                    .map(this::buildCountryContent)
                    .orElse(null);
            case "CITY" -> cityRepository.findById(entityId)
                    .map(this::buildCityContent)
                    .orElse(null);
            case "POI" -> poiRepository.findById(entityId)
                    .map(this::buildPoiContent)
                    .orElse(null);
            case "RESTAURANT" -> {
                RestaurantPO r = restaurantMapper.selectById(entityId);
                yield r != null ? buildRestaurantContent(r) : null;
            }
            case "HOTEL" -> {
                HotelPO h = hotelMapper.selectById(entityId);
                yield h != null ? buildHotelContent(h) : null;
            }
            case "ROUTE" -> {
                RoutePO r = routeMapper.selectById(entityId);
                yield r != null ? buildRouteContent(r) : null;
            }
            case "TRAVEL_GUIDE" -> {
                TravelGuidePO g = travelGuideMapper.selectById(entityId);
                yield g != null ? g.getContent() : null;
            }
            default -> null;
        };
    }

    private String buildCountryContent(Country c) {
        StringBuilder sb = new StringBuilder();
        sb.append("Country: ").append(c.getName()).append("\n");
        if (c.getIsoCode2() != null) sb.append("ISO Code: ").append(c.getIsoCode2()).append("\n");
        if (c.getContinent() != null) sb.append("Continent: ").append(c.getContinent()).append("\n");
        if (c.getCapital() != null) sb.append("Capital: ").append(c.getCapital()).append("\n");
        if (c.getCurrencyCode() != null) sb.append("Currency: ").append(c.getCurrencyCode()).append("\n");
        if (c.getLanguages() != null && !c.getLanguages().isEmpty()) {
            sb.append("Languages: ").append(String.join(", ", c.getLanguages())).append("\n");
        }
        return sb.toString();
    }

    private String buildCityContent(City c) {
        StringBuilder sb = new StringBuilder();
        sb.append("City: ").append(c.getName()).append("\n");
        if (c.getNameLocal() != null && !c.getNameLocal().equals(c.getName())) {
            sb.append("Local Name: ").append(c.getNameLocal()).append("\n");
        }
        if (c.getTimezone() != null) sb.append("Timezone: ").append(c.getTimezone()).append("\n");
        if (c.getPopulation() != null) sb.append("Population: ").append(c.getPopulation()).append("\n");
        if (c.getLatitude() != null && c.getLongitude() != null) {
            sb.append("Coordinates: ").append(c.getLatitude()).append(", ").append(c.getLongitude()).append("\n");
        }
        return sb.toString();
    }

    private String buildPoiContent(Poi p) {
        StringBuilder sb = new StringBuilder();
        sb.append("POI: ").append(p.getName()).append("\n");
        if (p.getCategory() != null) sb.append("Category: ").append(p.getCategory()).append("\n");
        if (p.getSubcategory() != null) sb.append("Subcategory: ").append(p.getSubcategory()).append("\n");
        if (p.getDescription() != null) sb.append("Description: ").append(p.getDescription()).append("\n");
        if (p.getAddress() != null) sb.append("Address: ").append(p.getAddress()).append("\n");
        if (p.getRating() != null) sb.append("Rating: ").append(p.getRating()).append("\n");
        return sb.toString();
    }

    private String buildRestaurantContent(RestaurantPO r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Restaurant: ").append(r.getName()).append("\n");
        if (r.getCuisineType() != null) sb.append("Cuisine: ").append(r.getCuisineType()).append("\n");
        if (r.getAddress() != null) sb.append("Address: ").append(r.getAddress()).append("\n");
        if (r.getRating() != null) sb.append("Rating: ").append(r.getRating()).append("\n");
        if (r.getPriceRange() != null) sb.append("Price Range: ").append(r.getPriceRange()).append("\n");
        return sb.toString();
    }

    private String buildHotelContent(HotelPO h) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hotel: ").append(h.getName()).append("\n");
        if (h.getCategory() != null) sb.append("Category: ").append(h.getCategory()).append("\n");
        if (h.getAddress() != null) sb.append("Address: ").append(h.getAddress()).append("\n");
        if (h.getRating() != null) sb.append("Rating: ").append(h.getRating()).append("\n");
        return sb.toString();
    }

    private String buildRouteContent(RoutePO r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Route: ").append(r.getRouteType()).append("\n");
        if (r.getDistanceKm() != null) sb.append("Distance: ").append(r.getDistanceKm()).append(" km\n");
        if (r.getDescription() != null) sb.append("Description: ").append(r.getDescription()).append("\n");
        return sb.toString();
    }
}
