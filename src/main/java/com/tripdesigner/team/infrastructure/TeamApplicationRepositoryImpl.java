package com.tripdesigner.team.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tripdesigner.team.domain.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TeamApplicationRepositoryImpl implements TeamApplicationRepository {

    private final TeamApplicationMapper mapper;

    public TeamApplicationRepositoryImpl(TeamApplicationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TeamApplication save(TeamApplication application) {
        TeamApplicationPO po = toPO(application);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public Optional<TeamApplication> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public List<TeamApplication> findByTeamId(Long teamId) {
        return mapper.selectList(Wrappers.<TeamApplicationPO>lambdaQuery()
                        .eq(TeamApplicationPO::getTeamId, teamId)
                        .orderByDesc(TeamApplicationPO::getCreatedAt))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public List<TeamApplication> findByApplicant(Long applicantId) {
        return mapper.selectList(Wrappers.<TeamApplicationPO>lambdaQuery()
                        .eq(TeamApplicationPO::getApplicantId, applicantId)
                        .orderByDesc(TeamApplicationPO::getCreatedAt))
                .stream().map(this::fromPO).toList();
    }

    @Override
    public Optional<TeamApplication> findByTeamAndApplicant(Long teamId, Long applicantId) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<TeamApplicationPO>lambdaQuery()
                        .eq(TeamApplicationPO::getTeamId, teamId)
                        .eq(TeamApplicationPO::getApplicantId, applicantId)))
                .map(this::fromPO);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private TeamApplicationPO toPO(TeamApplication a) {
        TeamApplicationPO po = new TeamApplicationPO();
        po.setId(a.getId());
        po.setTeamId(a.getTeamId());
        po.setApplicantId(a.getApplicantId());
        po.setMessage(a.getMessage());
        po.setStatus(a.getStatus() != null ? a.getStatus().name() : null);
        po.setProcessedAt(a.getProcessedAt());
        po.setProcessedBy(a.getProcessedBy());
        po.setCreatedAt(a.getCreatedAt());
        po.setUpdatedAt(a.getUpdatedAt());
        po.setVersion(a.getVersion());
        return po;
    }

    private TeamApplication fromPO(TeamApplicationPO po) {
        if (po == null) return null;
        return TeamApplication.builder()
                .id(po.getId())
                .teamId(po.getTeamId())
                .applicantId(po.getApplicantId())
                .message(po.getMessage())
                .status(parseStatus(po.getStatus()))
                .processedAt(po.getProcessedAt())
                .processedBy(po.getProcessedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .version(po.getVersion())
                .build();
    }

    private ApplicationStatus parseStatus(String value) {
        if (value == null) return null;
        try {
            return ApplicationStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
