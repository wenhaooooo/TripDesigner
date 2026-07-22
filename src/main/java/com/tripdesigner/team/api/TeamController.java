package com.tripdesigner.team.api;

import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.PageResult;
import com.tripdesigner.common.response.Result;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.team.api.dto.ApplyTeamRequest;
import com.tripdesigner.team.api.dto.CreateTeamRequest;
import com.tripdesigner.team.api.vo.TeamApplicationVo;
import com.tripdesigner.team.api.vo.TravelTeamVo;
import com.tripdesigner.team.application.TeamAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 旅行组队 REST API。
 */
@Tag(name = "Team", description = "旅行组队：创建队伍、申请加入、匹配推荐")
@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamAppService teamAppService;

    @Operation(summary = "创建队伍")
    @PostMapping
    public Result<TravelTeamVo> createTeam(@Valid @RequestBody CreateTeamRequest req) {
        requireAuth();
        return Result.success(teamAppService.createTeam(req));
    }

    @Operation(summary = "分页列出开放队伍")
    @GetMapping
    public Result<PageResult<TravelTeamVo>> listOpenTeams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(teamAppService.listOpenTeams(page, size));
    }

    @Operation(summary = "我创建的队伍")
    @GetMapping("/mine/created")
    public Result<List<TravelTeamVo>> myCreated() {
        requireAuth();
        return Result.success(teamAppService.listMyCreatedTeams());
    }

    @Operation(summary = "我加入的队伍")
    @GetMapping("/mine/joined")
    public Result<List<TravelTeamVo>> myJoined() {
        requireAuth();
        return Result.success(teamAppService.listMyJoinedTeams());
    }

    @Operation(summary = "查看队伍详情")
    @GetMapping("/{id}")
    public Result<TravelTeamVo> getTeam(@PathVariable Long id) {
        return Result.success(teamAppService.getTeam(id));
    }

    @Operation(summary = "关闭队伍")
    @PutMapping("/{id}/close")
    public Result<Void> closeTeam(@PathVariable Long id) {
        requireAuth();
        teamAppService.closeTeam(id);
        return Result.success();
    }

    @Operation(summary = "删除队伍")
    @DeleteMapping("/{id}")
    public Result<Void> deleteTeam(@PathVariable Long id) {
        requireAuth();
        teamAppService.deleteTeam(id);
        return Result.success();
    }

    @Operation(summary = "申请加入队伍")
    @PostMapping("/{id}/applications")
    public Result<TeamApplicationVo> apply(@PathVariable Long id,
                                             @RequestBody(required = false) ApplyTeamRequest req) {
        requireAuth();
        return Result.success(teamAppService.applyToJoin(id, req));
    }

    @Operation(summary = "批准申请")
    @PutMapping("/applications/{applicationId}/approve")
    public Result<TeamApplicationVo> approve(@PathVariable Long applicationId) {
        requireAuth();
        return Result.success(teamAppService.approveApplication(applicationId));
    }

    @Operation(summary = "拒绝申请")
    @PutMapping("/applications/{applicationId}/reject")
    public Result<TeamApplicationVo> reject(@PathVariable Long applicationId) {
        requireAuth();
        return Result.success(teamAppService.rejectApplication(applicationId));
    }

    @Operation(summary = "取消申请")
    @PutMapping("/applications/{applicationId}/cancel")
    public Result<Void> cancel(@PathVariable Long applicationId) {
        requireAuth();
        teamAppService.cancelApplication(applicationId);
        return Result.success();
    }

    @Operation(summary = "列出队伍的申请（仅创建者可调用）")
    @GetMapping("/{id}/applications")
    public Result<List<TeamApplicationVo>> listApplications(@PathVariable Long id) {
        requireAuth();
        return Result.success(teamAppService.listApplicationsForTeam(id));
    }

    @Operation(summary = "我提交的申请")
    @GetMapping("/applications/mine")
    public Result<List<TeamApplicationVo>> myApplications() {
        requireAuth();
        return Result.success(teamAppService.listMyApplications());
    }

    @Operation(summary = "退出队伍")
    @DeleteMapping("/{id}/members")
    public Result<Void> leaveTeam(@PathVariable Long id) {
        requireAuth();
        teamAppService.leaveTeam(id);
        return Result.success();
    }

    @Operation(summary = "匹配推荐（按目的地+日期重叠）")
    @GetMapping("/matches")
    public Result<List<TravelTeamVo>> findMatches(
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(teamAppService.findMatches(destination, startDate, endDate));
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }
}
