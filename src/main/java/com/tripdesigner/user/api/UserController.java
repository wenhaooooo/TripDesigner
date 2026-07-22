package com.tripdesigner.user.api;
/**
 * 用户 REST API 控制器。
 * 提供当前用户信息查询和更新功能。
 */

import com.tripdesigner.common.response.Result;
import com.tripdesigner.user.api.dto.UserUpdateRequest;
import com.tripdesigner.user.api.vo.UserVo;
import com.tripdesigner.user.application.UserAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserAppService userAppService;

    @GetMapping("/me")
    public Result<UserVo> me() {
        return Result.success(userAppService.me());
    }

    @PutMapping("/me")
    public Result<UserVo> update(@Valid @RequestBody UserUpdateRequest req) {
        return Result.success(userAppService.update(req));
    }
}