package com.tripdesigner.user.application;
/**
 * 用户应用服务。
 * 处理用户信息的查询和更新逻辑。
 */

import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.user.api.dto.UserUpdateRequest;
import com.tripdesigner.user.api.vo.UserVo;
import com.tripdesigner.user.domain.User;
import com.tripdesigner.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAppService {
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public UserVo me() {
        UserContext ctx = UserContextHolder.get();
        return userRepo.findById(ctx.userId())
                .map(UserVo::from)
                .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
    }

    @Transactional
    public UserVo update(UserUpdateRequest req) {
        UserContext ctx = UserContextHolder.get();
        User user = userRepo.findById(ctx.userId())
                .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        User updated = user.withNickname(req.getNickname());
        return UserVo.from(userRepo.save(updated));
    }
}