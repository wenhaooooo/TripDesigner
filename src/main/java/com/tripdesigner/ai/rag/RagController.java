package com.tripdesigner.ai.rag;

import com.tripdesigner.common.response.Result;
import com.tripdesigner.crawler.CrawlerService;
import com.tripdesigner.xiaohongshu.api.dto.XiaohongshuSearchRequest;
import com.tripdesigner.xiaohongshu.api.vo.XiaohongshuNoteVo;
import com.tripdesigner.xiaohongshu.api.vo.XiaohongshuSearchResponse;
import com.tripdesigner.xiaohongshu.application.XiaohongshuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
@Tag(name = "RAG 知识库", description = "目的地知识库的索引和检索")
public class RagController {

    private final DestinationKnowledgeService knowledgeService;
    private final XiaohongshuService xiaohongshuService;
    private final CrawlerService crawlerService;

    @PostMapping("/knowledge")
    @Operation(summary = "手动添加目的地知识", description = "将一段文本作为目的地知识索引到向量库")
    public Result<Void> addKnowledge(@RequestBody AddKnowledgeRequest request) {
        knowledgeService.indexKnowledge(
                request.destination(),
                request.category(),
                "MANUAL",
                request.content()
        );
        return Result.success(null);
    }

    @PostMapping("/knowledge/xiaohongshu")
    @Operation(summary = "从小红书抓取并索引知识", description = "根据关键词搜索小红书帖子，将结果索引到目的地知识库")
    public Result<Map<String, Object>> indexFromXiaohongshu(@RequestBody IndexFromXhsRequest request) {
        XiaohongshuSearchResponse response = xiaohongshuService.search(
                XiaohongshuSearchRequest.builder()
                        .keyword(request.keyword())
                        .limit(request.limit() != null ? request.limit() : 10)
                        .build()
        );

        List<String> posts = response.getNotes().stream()
                .map(this::buildPostContent)
                .toList();

        knowledgeService.indexXiaohongshuPosts(request.destination(), posts);

        return Result.success(Map.of(
                "indexed", posts.size(),
                "destination", request.destination()
        ));
    }

    @GetMapping("/knowledge/search")
    @Operation(summary = "语义检索目的地知识", description = "根据查询文本检索相关的目的地知识")
    public Result<List<String>> searchKnowledge(
            @RequestParam String destination,
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK
    ) {
        List<String> knowledge = knowledgeService.searchKnowledge(destination, query);
        return Result.success(knowledge);
    }

    @PostMapping("/knowledge/crawl-all")
    @Operation(summary = "手动触发全站知识抓取", description = "从所有平台抓取所有热门目的地的攻略并索引到知识库")
    public Result<Map<String, Object>> crawlAllDestinations() {
        Map<String, Object> result = crawlerService.crawlAllDestinations();
        return Result.success(result);
    }

    @PostMapping("/knowledge/crawl")
    @Operation(summary = "抓取指定目的地知识", description = "从所有平台抓取指定目的地的攻略并索引到知识库")
    public Result<Map<String, Object>> crawlDestination(@RequestBody CrawlDestinationRequest request) {
        int indexed = crawlerService.crawlDestination(request.destination(), request.limit() != null ? request.limit() : 5);
        return Result.success(Map.of(
                "indexed", indexed,
                "destination", request.destination()
        ));
    }

    @GetMapping("/sources")
    @Operation(summary = "获取可用数据源", description = "获取所有可用的旅游攻略数据源")
    public Result<List<String>> getAvailableSources() {
        return Result.success(crawlerService.getAvailableSources());
    }

    @DeleteMapping("/knowledge")
    @Operation(summary = "清空目的地知识库", description = "删除所有目的地知识文档")
    public Result<Void> clearKnowledge() {
        knowledgeService.deleteDestinationKnowledge();
        return Result.success(null);
    }

    private String buildPostContent(XiaohongshuNoteVo note) {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(note.getTitle()).append("\n");
        sb.append("Content: ").append(note.getContent()).append("\n");
        if (note.getTags() != null && !note.getTags().isBlank()) {
            sb.append("Tags: ").append(note.getTags()).append("\n");
        }
        sb.append("Likes: ").append(note.getLikes());
        return sb.toString();
    }

    public record AddKnowledgeRequest(
            String destination,
            String category,
            String content
    ) {}

    public record IndexFromXhsRequest(
            String destination,
            String keyword,
            Integer limit
    ) {}

    public record CrawlDestinationRequest(
            String destination,
            Integer limit
    ) {}
}