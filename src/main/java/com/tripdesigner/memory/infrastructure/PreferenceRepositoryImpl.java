package com.tripdesigner.memory.infrastructure;
/**
 * 偏好仓储实现。
 * 使用 MyBatis Plus 实现持久化。
 * data 字段通过 PreferenceConverter 进行 JSON 转换。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tripdesigner.memory.domain.PreferenceRepository;
import com.tripdesigner.memory.domain.UserPreference;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PreferenceRepositoryImpl implements PreferenceRepository {

    private final UserPreferenceMapper mapper;

    public PreferenceRepositoryImpl(UserPreferenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public UserPreference save(UserPreference preference) {
        UserPreferencePO po = PreferenceConverter.toPO(preference);
        if (preference.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return PreferenceConverter.toDomain(po);
    }

    @Override
    public Optional<UserPreference> findById(Long id) {
        UserPreferencePO po = mapper.selectById(id);
        return Optional.ofNullable(PreferenceConverter.toDomain(po));
    }

    @Override
    public List<UserPreference> findByUserId(Long userId) {
        LambdaQueryWrapper<UserPreferencePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPreferencePO::getUserId, userId)
                .orderByDesc(UserPreferencePO::getUpdatedAt);
        return mapper.selectList(wrapper).stream()
                .map(PreferenceConverter::toDomain)
                .toList();
    }

    @Override
    public List<UserPreference> findByUserIdAndCategory(Long userId, String category) {
        LambdaQueryWrapper<UserPreferencePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPreferencePO::getUserId, userId)
                .eq(UserPreferencePO::getCategory, category)
                .orderByDesc(UserPreferencePO::getUpdatedAt);
        return mapper.selectList(wrapper).stream()
                .map(PreferenceConverter::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }
}
