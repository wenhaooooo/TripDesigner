package com.tripdesigner.xiaohongshu.api;

import com.tripdesigner.common.response.Result;
import com.tripdesigner.xiaohongshu.api.dto.XiaohongshuSearchRequest;
import com.tripdesigner.xiaohongshu.api.vo.XiaohongshuSearchResponse;
import com.tripdesigner.xiaohongshu.application.XiaohongshuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/xiaohongshu")
@RequiredArgsConstructor
public class XiaohongshuController {
    private final XiaohongshuService xiaohongshuService;

    @PostMapping("/search")
    public Result<XiaohongshuSearchResponse> search(@Valid @RequestBody XiaohongshuSearchRequest request) {
        return Result.success();
//        return Result.success(xiaohongshuService.search(request));
    }

    @GetMapping("/search")
    public Result<XiaohongshuSearchResponse> searchGet(@RequestParam String keyword,
                                                       @RequestParam(defaultValue = "5") Integer limit) {
        return Result.success();
//        return Result.success(xiaohongshuService.search(XiaohongshuSearchRequest.builder()
//                .keyword(keyword)
//                .limit(limit)
//                .build()));
    }
}