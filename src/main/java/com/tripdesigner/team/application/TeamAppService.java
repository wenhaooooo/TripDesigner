package com.tripdesigner.team.application;

import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.PageResult;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.common.security.UserContext;
import com.tripdesigner.common.security.UserContextHolder;
import com.tripdesigner.team.api.dto.ApplyTeamRequest;
import com.tripdesigner.team.api.dto.CreateTeamRequest;
import com.tripdesigner.team.api.vo.TeamApplicationVo;
import com.tripdesigner.team.api.vo.TravelTeamVo;
import com.tripdesigner.team.domain.*;
import com.tripdesigner.user.domain.User;
import com.tripdesigner.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 旅行组队应用服务。
 *
 * 用例：
 * 1. 创建队伍（自动添加创建者为成员）
 * 2. 申请加入队伍
 * 3. 批准/拒绝申请
 * 4. 退出队伍
 * 5. 匹配推荐（按目的地+日期重叠）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamAppService {

    private final TravelTeamRepository teamRepository;
    private final TeamApplicationRepository applicationRepository;
    private final TeamMemberRepository memberRepository;
    private final UserRepository userRepository;

    @Transactional
    public TravelTeamVo createTeam(CreateTeamRequest req) {
        UserContext ctx = requireAuth();
        if (req.getStartDate() != null && req.getEndDate() != null
                && req.getStartDate().isAfter(req.getEndDate())) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, "开始日期不能晚于结束日期");
        }

        TeamType teamType = parseEnum(TeamType.class, req.getTeamType(), "invalid teamType");
        TravelTeam team = TravelTeam.create(
                ctx.userId(), req.getTitle(), req.getDescription(), req.getDestination(),
                req.getStartDate(), req.getEndDate(), teamType, req.getInterests(),
                req.getMaxMembers(), req.getGenderRequirement(), req.getMinAge(),
                req.getMaxAge(), req.getContact());

        TravelTeam saved = teamRepository.save(team);

        // 添加创建者为 CREATOR 成员
        memberRepository.save(TeamMember.creator(saved.getId(), ctx.userId()));

        return toTeamVo(saved, ctx.userId());
    }

    @Transactional(readOnly = true)
    public PageResult<TravelTeamVo> listOpenTeams(int page, int size) {
        Long currentUserId = currentUserIdOrNull();
        List<TravelTeam> teams = teamRepository.findOpen(page, size);
        List<TravelTeamVo> vos = teams.stream()
                .map(t -> toTeamVo(t, currentUserId))
                .toList();
        return new PageResult<>(page, size, vos.size(), vos);
    }

    @Transactional(readOnly = true)
    public List<TravelTeamVo> listMyCreatedTeams() {
        UserContext ctx = requireAuth();
        return teamRepository.findByCreator(ctx.userId()).stream()
                .map(t -> toTeamVo(t, ctx.userId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TravelTeamVo> listMyJoinedTeams() {
        UserContext ctx = requireAuth();
        return memberRepository.findByUserId(ctx.userId()).stream()
                .map(m -> teamRepository.findById(m.getTeamId()).orElse(null))
                .filter(t -> t != null)
                .map(t -> toTeamVo(t, ctx.userId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TravelTeamVo getTeam(Long id) {
        Long currentUserId = currentUserIdOrNull();
        TravelTeam team = loadTeam(id);
        return toTeamVo(team, currentUserId);
    }

    @Transactional
    public void closeTeam(Long id) {
        UserContext ctx = requireAuth();
        TravelTeam team = loadTeam(id);
        verifyCreator(team, ctx.userId());
        teamRepository.save(team.withStatus(TeamStatus.CLOSED));
    }

    @Transactional
    public void deleteTeam(Long id) {
        UserContext ctx = requireAuth();
        TravelTeam team = loadTeam(id);
        verifyCreator(team, ctx.userId());
        teamRepository.deleteById(id);
    }

    @Transactional
    public TeamApplicationVo applyToJoin(Long teamId, ApplyTeamRequest req) {
        UserContext ctx = requireAuth();
        TravelTeam team = loadTeam(teamId);

        if (team.getCreatorId().equals(ctx.userId())) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, "不能申请加入自己创建的队伍");
        }
        if (!team.isOpenForApplication()) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST,
                    "队伍已满或已关闭，无法申请");
        }
        if (memberRepository.findByTeamAndUser(teamId, ctx.userId()).isPresent()) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, "你已是该队伍成员");
        }
        if (applicationRepository.findByTeamAndApplicant(teamId, ctx.userId()).isPresent()) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, "你已申请过该队伍，请等待审核");
        }

        TeamApplication application = TeamApplication.create(teamId, ctx.userId(),
                req != null ? req.getMessage() : null);
        TeamApplication saved = applicationRepository.save(application);

        String email = userEmail(ctx.userId()).orElse("");
        return TeamApplicationVo.from(saved, email);
    }

    @Transactional
    public TeamApplicationVo approveApplication(Long applicationId) {
        UserContext ctx = requireAuth();
        TeamApplication application = loadApplication(applicationId);
        TravelTeam team = loadTeam(application.getTeamId());
        verifyCreator(team, ctx.userId());

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, "申请已处理");
        }
        if (team.isFull()) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, "队伍已满，无法加入");
        }

        TeamApplication approved = applicationRepository.save(application.approve(ctx.userId()));
        memberRepository.save(TeamMember.member(team.getId(), application.getApplicantId()));
        teamRepository.save(team.withMemberCount(1));

        // 队伍满员时自动切换状态
        TravelTeam refreshed = teamRepository.findById(team.getId()).orElse(team);
        if (refreshed.isFull()) {
            teamRepository.save(refreshed.withStatus(TeamStatus.FULL));
        }

        String email = userEmail(approved.getApplicantId()).orElse("");
        return TeamApplicationVo.from(approved, email);
    }

    @Transactional
    public TeamApplicationVo rejectApplication(Long applicationId) {
        UserContext ctx = requireAuth();
        TeamApplication application = loadApplication(applicationId);
        TravelTeam team = loadTeam(application.getTeamId());
        verifyCreator(team, ctx.userId());

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, "申请已处理");
        }

        TeamApplication rejected = applicationRepository.save(application.reject(ctx.userId()));
        String email = userEmail(rejected.getApplicantId()).orElse("");
        return TeamApplicationVo.from(rejected, email);
    }

    @Transactional
    public void cancelApplication(Long applicationId) {
        UserContext ctx = requireAuth();
        TeamApplication application = loadApplication(applicationId);
        if (!application.getApplicantId().equals(ctx.userId())) {
            throw new BizException(ResultCode.PERMISSION_DENIED, "只能取消自己的申请");
        }
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, "申请已处理，无法取消");
        }
        applicationRepository.save(application.cancel());
    }

    @Transactional(readOnly = true)
    public List<TeamApplicationVo> listApplicationsForTeam(Long teamId) {
        UserContext ctx = requireAuth();
        TravelTeam team = loadTeam(teamId);
        verifyCreator(team, ctx.userId());
        return applicationRepository.findByTeamId(teamId).stream()
                .map(a -> TeamApplicationVo.from(a, userEmail(a.getApplicantId()).orElse("")))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamApplicationVo> listMyApplications() {
        UserContext ctx = requireAuth();
        return applicationRepository.findByApplicant(ctx.userId()).stream()
                .map(a -> TeamApplicationVo.from(a, userEmail(a.getApplicantId()).orElse("")))
                .toList();
    }

    @Transactional
    public void leaveTeam(Long teamId) {
        UserContext ctx = requireAuth();
        TravelTeam team = loadTeam(teamId);
        if (team.getCreatorId().equals(ctx.userId())) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, "创建者不能退出，请使用关闭/删除操作");
        }
        if (memberRepository.findByTeamAndUser(teamId, ctx.userId()).isEmpty()) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, "你不是该队伍成员");
        }
        memberRepository.deleteByTeamIdAndUserId(teamId, ctx.userId());
        teamRepository.save(team.withMemberCount(-1));
    }

    /** 匹配推荐：按目的地+日期重叠查找可加入的队伍 */
    @Transactional(readOnly = true)
    public List<TravelTeamVo> findMatches(String destination, LocalDate startDate, LocalDate endDate) {
        Long currentUserId = currentUserIdOrNull();
        return teamRepository.findMatches(destination, startDate, endDate).stream()
                .filter(t -> currentUserId == null || !t.getCreatorId().equals(currentUserId))
                .map(t -> toTeamVo(t, currentUserId))
                .toList();
    }

    // ========== 私有方法 ==========

    private TravelTeamVo toTeamVo(TravelTeam team, Long currentUserId) {
        String creatorEmail = userEmail(team.getCreatorId()).orElse("");
        boolean isCreator = currentUserId != null && currentUserId.equals(team.getCreatorId());
        boolean isMember = currentUserId != null
                && memberRepository.findByTeamAndUser(team.getId(), currentUserId).isPresent();
        boolean hasApplied = currentUserId != null
                && applicationRepository.findByTeamAndApplicant(team.getId(), currentUserId).isPresent();
        return TravelTeamVo.from(team, creatorEmail, isCreator, isMember, hasApplied);
    }

    private TravelTeam loadTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.COMMON_NOT_FOUND, "team not found"));
    }

    private TeamApplication loadApplication(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.COMMON_NOT_FOUND, "application not found"));
    }

    private void verifyCreator(TravelTeam team, Long currentUserId) {
        if (!team.getCreatorId().equals(currentUserId)) {
            throw new BizException(ResultCode.PERMISSION_DENIED, "only team creator can perform this action");
        }
    }

    private Optional<String> userEmail(Long userId) {
        if (userId == null) return Optional.empty();
        return userRepository.findById(userId).map(User::getEmail);
    }

    private Long currentUserIdOrNull() {
        UserContext ctx = UserContextHolder.get();
        return ctx != null ? ctx.userId() : null;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String errorMessage) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.COMMON_BAD_REQUEST, errorMessage);
        }
    }

    private UserContext requireAuth() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID, "authentication required");
        }
        return ctx;
    }
}
