package com.tripdesigner.conversation.api;
/**
 * 对话 REST API 控制器。
 * 提供对话的 CRUD 和消息管理功能。
 */

import com.tripdesigner.common.response.Result;
import com.tripdesigner.conversation.api.dto.AddMessageRequest;
import com.tripdesigner.conversation.api.dto.CreateConversationRequest;
import com.tripdesigner.conversation.api.vo.ConversationMessageVo;
import com.tripdesigner.conversation.api.vo.ConversationVo;
import com.tripdesigner.conversation.application.ConversationAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationAppService convAppService;

    @GetMapping
    public Result<List<ConversationVo>> list() {
        return Result.success(convAppService.list());
    }

    @PostMapping
    public Result<ConversationVo> create(@Valid @RequestBody CreateConversationRequest req) {
        return Result.success(convAppService.create(req));
    }

    @GetMapping("/{id}")
    public Result<ConversationVo> get(@PathVariable Long id) {
        return Result.success(convAppService.get(id));
    }

    @PutMapping("/{id}")
    public Result<ConversationVo> update(@PathVariable Long id, @Valid @RequestBody CreateConversationRequest req) {
        return Result.success(convAppService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        convAppService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}/messages")
    public Result<List<ConversationMessageVo>> listMessages(@PathVariable Long id) {
        return Result.success(convAppService.listMessages(id));
    }

    @PostMapping("/{id}/messages")
    public Result<ConversationMessageVo> addMessage(@PathVariable Long id, @Valid @RequestBody AddMessageRequest req) {
        return Result.success(convAppService.addMessage(id, req));
    }
}
