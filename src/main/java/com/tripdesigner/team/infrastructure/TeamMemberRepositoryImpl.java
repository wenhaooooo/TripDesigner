package com.tripdesigner.team.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tripdesigner.team.domain.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TeamMemberRepositoryImpl implements TeamMemberRepository {

    private final TeamMemberMapper mapper;

    public TeamMemberRepositoryImpl(TeamMemberMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TeamMember save(TeamMember member) {
        TeamMemberPO po = toPO(member);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public Optional<TeamMember> findByTeamAndUser(Long teamId, Long userId) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<TeamMemberPO>lambdaQuery()
                        .eq(TeamMemberPO::getTeamId, teamId)
                        .eq(TeamMemberPO::getUserId, userId)))
                .map(this::fromPO);
    }

    @Override
    public List<TeamMember> findByTeamId(Long teamId) {
        return mapper.selectList(Wrappers.<TeamMemberPO>lambdaQuery()
                        .eq(TeamMemberPO::getTeamId, teamId)
                        .orderByAsc(TeamMemberPO::getJoinedAt))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public List<TeamMember> findByUserId(Long userId) {
        return mapper.selectList(Wrappers.<TeamMemberPO>lambdaQuery()
                        .eq(TeamMemberPO::getUserId, userId)
                        .orderByDesc(TeamMemberPO::getJoinedAt))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public void deleteByTeamIdAndUserId(Long teamId, Long userId) {
        mapper.delete(Wrappers.<TeamMemberPO>lambdaQuery()
                .eq(TeamMemberPO::getTeamId, teamId)
                .eq(TeamMemberPO::getUserId, userId));
    }

    @Override
    public int countByTeamId(Long teamId) {
        return Math.toIntExact(mapper.selectCount(Wrappers.<TeamMemberPO>lambdaQuery()
                .eq(TeamMemberPO::getTeamId, teamId)));
    }

    private TeamMemberPO toPO(TeamMember m) {
        TeamMemberPO po = new TeamMemberPO();
        po.setId(m.getId());
        po.setTeamId(m.getTeamId());
        po.setUserId(m.getUserId());
        po.setRole(m.getRole() != null ? m.getRole().name() : MemberRole.MEMBER.name());
        po.setJoinedAt(m.getJoinedAt());
        return po;
    }

    private TeamMember fromPO(TeamMemberPO po) {
        if (po == null) return null;
        return TeamMember.builder()
                .id(po.getId())
                .teamId(po.getTeamId())
                .userId(po.getUserId())
                .role(parseRole(po.getRole()))
                .joinedAt(po.getJoinedAt())
                .build();
    }

    private MemberRole parseRole(String value) {
        if (value == null) return null;
        try {
            return MemberRole.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
