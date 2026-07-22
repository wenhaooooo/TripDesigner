package com.tripdesigner.knowledge.api;

import com.tripdesigner.common.response.PageResult;
import com.tripdesigner.common.response.Result;
import com.tripdesigner.knowledge.api.dto.CrawlRequest;
import com.tripdesigner.knowledge.api.dto.EmbedRequest;
import com.tripdesigner.knowledge.api.dto.ReindexRequest;
import com.tripdesigner.knowledge.api.vo.CrawlResultVo;
import com.tripdesigner.knowledge.api.vo.KnowledgeStatsVo;
import com.tripdesigner.knowledge.application.CrawlAppService;
import com.tripdesigner.knowledge.application.KnowledgeAppService;
import com.tripdesigner.knowledge.api.vo.CityVo;
import com.tripdesigner.knowledge.api.vo.CountryVo;
import com.tripdesigner.knowledge.api.vo.PoiVo;
import com.tripdesigner.knowledge.api.vo.SearchResultVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 全球旅行知识库 REST API 控制器。
 *
 * 提供知识库的完整管理能力，包括：
 * <ul>
 *   <li>数据抓取（Crawl）：从 OSM、Wikivoyage 等数据源抓取知识</li>
 *   <li>向量化索引（Embed/Reindex）：将文本内容向量化并存储</li>
 *   <li>语义检索（Search）：基于 RAG 的向量相似度检索</li>
 *   <li>实体查询（POI/City/Country）：按 ID 或条件查询结构化实体</li>
 *   <li>统计分析（Stats）：知识库覆盖度与数据源分布统计</li>
 *   <li>知识图谱（Relations）：实体间关联关系查询</li>
 * </ul>
 *
 * 所有接口返回统一的 {@link Result} 包装格式，分页接口返回 {@link PageResult}。
 */
@Slf4j
@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
@Tag(name = "Knowledge Base", description = "全球旅行知识库管理接口")
public class KnowledgeController {

    private final KnowledgeAppService knowledgeAppService;
    private final CrawlAppService crawlAppService;

    // ========== 数据抓取与索引 ==========

    /**
     * 触发知识库抓取任务。
     * 根据指定的数据源（OSM/WIKIVOYAGE/WIKIPEDIA/OPENTRIPMAP）抓取目的地相关数据，
     * 解析、向量化后写入知识库。
     *
     * @param request 抓取请求（包含数据源、查询关键词、数量限制等）
     * @return 抓取结果统计
     */
    @PostMapping("/crawl")
    @Operation(summary = "触发知识库抓取", description = "从指定数据源抓取旅行知识并索引到知识库")
    public Result<CrawlResultVo> crawl(@RequestBody CrawlRequest request) {
        log.info("[KnowledgeController] Crawl request: source={}, query={}", request.source(), request.query());
        return Result.success(crawlAppService.crawl(request));
    }

    /**
     * 手动对一段文本内容进行向量化并写入知识库。
     * 适用于管理员手动添加攻略、人工校对内容等场景。
     *
     * @param request 向量化请求（包含实体类型、ID、内容、块类型、语言）
     * @return 操作结果
     */
    @PostMapping("/embed")
    @Operation(summary = "手动向量化内容", description = "将一段文本内容向量化并存储到知识库")
    public Result<Void> embed(@RequestBody EmbedRequest request) {
        log.info("[KnowledgeController] Embed request: entityType={}, entityId={}", request.entityType(), request.entityId());
        crawlAppService.embed(request);
        return Result.success();
    }

    /**
     * 重建知识库索引。
     * 支持按实体类型和实体 ID 精确重建，也可全量重建。
     * force 参数为 true 时即使内容未变更也会重新分块和向量化。
     *
     * @param request 重建索引请求
     * @return 操作结果
     */
    @PostMapping("/reindex")
    @Operation(summary = "重建知识库索引", description = "对指定实体或全量知识块重新分块和向量化")
    public Result<Void> reindex(@RequestBody ReindexRequest request) {
        log.info("[KnowledgeController] Reindex request: entityType={}, entityId={}, force={}",
                request.entityType(), request.entityId(), request.force());
        crawlAppService.reindex(request);
        return Result.success();
    }

    // ========== 语义检索 ==========

    /**
     * 语义检索知识库。
     * 基于向量相似度检索最相关的知识块，支持按实体类型、城市、国家、类别等条件过滤，
     * 并支持 MMR 去重和混合检索模式。
     *
     * @param query               查询文本（必填）
     * @param entityType          实体类型过滤（可空）
     * @param cityId              城市 ID 过滤（可空）
     * @param countryId           国家 ID 过滤（可空）
     * @param category            类别过滤（可空）
     * @param language            语言（默认 "en"）
     * @param topK                返回结果数（默认 5）
     * @param similarityThreshold 相似度阈值（默认 0.5）
     * @param useMmr              是否启用 MMR 去重（默认 false）
     * @param useHybridSearch     是否启用混合检索（默认 false）
     * @return 检索结果列表
     */
    @GetMapping("/search")
    @Operation(summary = "语义检索知识库", description = "基于向量相似度检索相关的知识块")
    public Result<List<SearchResultVo>> search(
            @RequestParam String query,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Long countryId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "5") Integer topK,
            @RequestParam(defaultValue = "0.5") Double similarityThreshold,
            @RequestParam(defaultValue = "false") boolean useMmr,
            @RequestParam(defaultValue = "false") boolean useHybridSearch
    ) {
        return Result.success(knowledgeAppService.search(
                query, entityType, cityId, countryId, category, language,
                topK, similarityThreshold, useMmr, useHybridSearch));
    }

    // ========== 实体查询 ==========

    /**
     * 按 ID 查询 POI 详情。
     *
     * @param id POI ID
     * @return POI 视图对象
     */
    @GetMapping("/poi/{id}")
    @Operation(summary = "查询 POI 详情", description = "按 ID 查询兴趣点（POI）的详细信息")
    public Result<PoiVo> getPoi(@PathVariable Long id) {
        return Result.success(knowledgeAppService.getPoi(id));
    }

    /**
     * 按 ID 查询城市详情。
     *
     * @param id 城市 ID
     * @return 城市视图对象
     */
    @GetMapping("/city/{id}")
    @Operation(summary = "查询城市详情", description = "按 ID 查询城市的详细信息")
    public Result<CityVo> getCity(@PathVariable Long id) {
        return Result.success(knowledgeAppService.getCity(id));
    }

    /**
     * 按 ID 查询国家详情。
     *
     * @param id 国家 ID
     * @return 国家视图对象
     */
    @GetMapping("/country/{id}")
    @Operation(summary = "查询国家详情", description = "按 ID 查询国家的详细信息")
    public Result<CountryVo> getCountry(@PathVariable Long id) {
        return Result.success(knowledgeAppService.getCountry(id));
    }

    /**
     * 分页查询城市列表。
     * 可按国家 ID 过滤，按人口降序排列。
     *
     * @param countryId 国家 ID（可空，为空则查询全部国家）
     * @param page      页码（从 0 开始）
     * @param size      每页大小
     * @return 分页城市列表
     */
    @GetMapping("/cities")
    @Operation(summary = "分页查询城市列表", description = "可按国家 ID 过滤，支持分页")
    public Result<PageResult<CityVo>> listCities(
            @RequestParam(required = false) Long countryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return Result.success(knowledgeAppService.listCities(countryId, page, size));
    }

    /**
     * 分页查询 POI 列表。
     * 可按城市 ID 和类别过滤，按评分降序排列。
     *
     * @param cityId   城市 ID（可空）
     * @param category 类别（可空）
     * @param page     页码（从 0 开始）
     * @param size     每页大小
     * @return 分页 POI 列表
     */
    @GetMapping("/pois")
    @Operation(summary = "分页查询 POI 列表", description = "可按城市 ID 和类别过滤，支持分页")
    public Result<PageResult<PoiVo>> listPois(
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return Result.success(knowledgeAppService.listPois(cityId, category, page, size));
    }

    // ========== 统计与知识图谱 ==========

    /**
     * 获取知识库统计信息。
     * 返回各实体类型的总数及按数据源分布的数量。
     *
     * @return 知识库统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "获取知识库统计信息", description = "返回各实体类型的总数和数据源分布")
    public Result<KnowledgeStatsVo> getStats() {
        return Result.success(knowledgeAppService.getStats());
    }

    /**
     * 查询知识图谱中某个实体的关联关系。
     * 返回该实体与其他实体（如城市-国家、POI-城市、POI-餐厅等）之间的关联。
     *
     * @param entityType 实体类型：COUNTRY, CITY, POI, RESTAURANT, HOTEL, TRAVEL_GUIDE, ROUTE
     * @param entityId   实体 ID
     * @return 关联关系列表（每项包含关系类型、目标实体类型与 ID 等）
     */
    @GetMapping("/relations/{entityType}/{entityId}")
    @Operation(summary = "查询知识图谱关系", description = "获取指定实体在知识图谱中的关联关系")
    public Result<List<Map<String, Object>>> getRelations(
            @PathVariable String entityType,
            @PathVariable Long entityId
    ) {
        return Result.success(knowledgeAppService.getRelations(entityType, entityId));
    }
}
