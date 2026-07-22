package com.tripdesigner.user.infrastructure;
/**
 * 用户仓储实现。
 * 使用 MyBatis Plus 实现持久化。
 * 支持按邮箱查询和邮箱存在性检查。
 */

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tripdesigner.user.domain.User;
import com.tripdesigner.user.domain.UserRepository;
import com.tripdesigner.user.domain.UserStatus;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {
    private final UserMapper mapper;

    public UserRepositoryImpl(UserMapper mapper) { this.mapper = mapper; }

    @Override
    public User save(User user) {
        UserPO po = toPO(user);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(mapper.selectOne(
                Wrappers.<UserPO>lambdaQuery().eq(UserPO::getEmail, email))).map(this::fromPO);
    }

    @Override
    public boolean existsByEmail(String email) {
        return mapper.exists(Wrappers.<UserPO>lambdaQuery().eq(UserPO::getEmail, email));
    }

    private UserPO toPO(User u) {
        UserPO po = new UserPO();
        po.setId(u.getId());
        po.setEmail(u.getEmail());
        po.setPasswordHash(u.getPasswordHash());
        po.setNickname(u.getNickname());
        po.setStatus(u.getStatus() == null ? null : u.getStatus().getCode());
        po.setVersion(u.getVersion());
        return po;
    }

    private User fromPO(UserPO po) {
        return User.builder()
                .id(po.getId())
                .email(po.getEmail())
                .passwordHash(po.getPasswordHash())
                .nickname(po.getNickname())
                .status(UserStatus.of(po.getStatus()))
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .version(po.getVersion())
                .build();
    }
}