package com.tripdesigner.experience.infrastructure;
/**
 * 体验仓储实现。
 * 使用 MyBatis Plus 实现持久化。
 * 使用 TripExperienceConverter 处理 JSON 字段转换。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tripdesigner.experience.domain.ExperienceRepository;
import com.tripdesigner.experience.domain.ExperienceStatus;
import com.tripdesigner.experience.domain.TripExperience;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ExperienceRepositoryImpl implements ExperienceRepository {

    private final TripExperienceMapper mapper;

    public ExperienceRepositoryImpl(TripExperienceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TripExperience save(TripExperience experience) {
        TripExperiencePO po = TripExperienceConverter.toPO(experience);
        if (experience.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return TripExperienceConverter.toDomain(po);
    }

    @Override
    public Optional<TripExperience> findById(Long id) {
        TripExperiencePO po = mapper.selectById(id);
        return Optional.ofNullable(TripExperienceConverter.toDomain(po));
    }

    @Override
    public List<TripExperience> findByUserId(Long userId) {
        LambdaQueryWrapper<TripExperiencePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TripExperiencePO::getUserId, userId)
                .eq(TripExperiencePO::getStatus, ExperienceStatus.PUBLISHED.name())
                .orderByDesc(TripExperiencePO::getCreatedAt);
        return mapper.selectList(wrapper).stream()
                .map(TripExperienceConverter::toDomain)
                .toList();
    }

    @Override
    public List<TripExperience> findByTripId(Long tripId) {
        LambdaQueryWrapper<TripExperiencePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TripExperiencePO::getTripId, tripId)
                .eq(TripExperiencePO::getStatus, ExperienceStatus.PUBLISHED.name())
                .orderByDesc(TripExperiencePO::getCreatedAt);
        return mapper.selectList(wrapper).stream()
                .map(TripExperienceConverter::toDomain)
                .toList();
    }

    @Override
    public List<TripExperience> findByTripDayId(Long tripDayId) {
        LambdaQueryWrapper<TripExperiencePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TripExperiencePO::getTripDayId, tripDayId)
                .eq(TripExperiencePO::getStatus, ExperienceStatus.PUBLISHED.name())
                .orderByDesc(TripExperiencePO::getCreatedAt);
        return mapper.selectList(wrapper).stream()
                .map(TripExperienceConverter::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }
}
