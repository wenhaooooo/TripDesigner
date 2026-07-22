package com.tripdesigner.ai.trip.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tripdesigner.ai.trip.TripGenerationStatus;
import com.tripdesigner.ai.trip.TripGenerationTask;
import com.tripdesigner.ai.trip.TripGenerationTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TripGenerationTaskRepositoryImpl implements TripGenerationTaskRepository {

    private final TripGenerationTaskMapper mapper;

    @Override
    public TripGenerationTask save(TripGenerationTask task) {
        TripGenerationTaskPO po = toPO(task);
        if (task.getId() != null) {
            mapper.updateById(po);
        } else {
            mapper.insert(po);
        }
        return fromPO(po);
    }

    @Override
    public Optional<TripGenerationTask> findById(Long id) {
        TripGenerationTaskPO po = mapper.selectById(id);
        return Optional.ofNullable(po != null ? fromPO(po) : null);
    }

    @Override
    public List<TripGenerationTask> findByUserId(Long userId) {
        return mapper.selectList(Wrappers.<TripGenerationTaskPO>lambdaQuery()
                .eq(TripGenerationTaskPO::getUserId, userId)
                .orderByDesc(TripGenerationTaskPO::getCreatedAt))
                .stream()
                .map(this::fromPO)
                .toList();
    }

    @Override
    public List<TripGenerationTask> findByUserIdAndStatus(Long userId, TripGenerationStatus status) {
        return mapper.selectList(Wrappers.<TripGenerationTaskPO>lambdaQuery()
                .eq(TripGenerationTaskPO::getUserId, userId)
                .eq(TripGenerationTaskPO::getStatus, status.name())
                .orderByDesc(TripGenerationTaskPO::getCreatedAt))
                .stream()
                .map(this::fromPO)
                .toList();
    }

    @Override
    public List<TripGenerationTask> findByTripId(Long tripId) {
        return mapper.selectList(Wrappers.<TripGenerationTaskPO>lambdaQuery()
                .eq(TripGenerationTaskPO::getTripId, tripId))
                .stream()
                .map(this::fromPO)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private TripGenerationTaskPO toPO(TripGenerationTask task) {
        TripGenerationTaskPO po = new TripGenerationTaskPO();
        po.setId(task.getId());
        po.setUserId(task.getUserId());
        po.setPrompt(task.getPrompt());
        po.setStatus(task.getStatus().name());
        po.setProgress(task.getProgress());
        po.setProgressMessage(task.getProgressMessage());
        po.setConversationId(task.getConversationId());
        po.setTripId(task.getTripId());
        po.setErrorMessage(task.getErrorMessage());
        return po;
    }

    private TripGenerationTask fromPO(TripGenerationTaskPO po) {
        return TripGenerationTask.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .prompt(po.getPrompt())
                .status(TripGenerationStatus.valueOf(po.getStatus()))
                .progress(po.getProgress())
                .progressMessage(po.getProgressMessage())
                .conversationId(po.getConversationId())
                .tripId(po.getTripId())
                .errorMessage(po.getErrorMessage())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}