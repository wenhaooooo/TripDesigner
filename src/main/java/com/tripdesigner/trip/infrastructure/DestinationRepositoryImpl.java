package com.tripdesigner.trip.infrastructure;
/**
 * 目的地仓储实现。
 * 使用 MyBatis Plus 实现持久化，支持按国家和分类查询。
 */

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tripdesigner.trip.domain.Destination;
import com.tripdesigner.trip.domain.DestinationRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DestinationRepositoryImpl implements DestinationRepository {
    private final DestinationMapper mapper;

    public DestinationRepositoryImpl(DestinationMapper mapper) { this.mapper = mapper; }

    @Override
    public Destination save(Destination destination) {
        DestinationPO po = toPO(destination);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return fromPO(po);
    }

    @Override
    public Optional<Destination> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::fromPO);
    }

    @Override
    public List<Destination> findByCountry(String country) {
        return mapper.selectList(
                Wrappers.<DestinationPO>lambdaQuery()
                        .eq(DestinationPO::getCountry, country)
                        .orderByAsc(DestinationPO::getName))
                .stream()
                .map(this::fromPO)
                .toList();
    }

    @Override
    public List<Destination> findByCategory(String category) {
        return mapper.selectList(
                Wrappers.<DestinationPO>lambdaQuery()
                        .eq(DestinationPO::getCategory, category)
                        .orderByAsc(DestinationPO::getName))
                .stream()
                .map(this::fromPO)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private DestinationPO toPO(Destination d) {
        DestinationPO po = new DestinationPO();
        po.setId(d.getId());
        po.setName(d.getName());
        po.setCountry(d.getCountry());
        po.setRegion(d.getRegion());
        po.setCategory(d.getCategory());
        po.setDescription(d.getDescription());
        po.setVersion(d.getVersion());
        return po;
    }

    private Destination fromPO(DestinationPO po) {
        return Destination.builder()
                .id(po.getId())
                .name(po.getName())
                .country(po.getCountry())
                .region(po.getRegion())
                .category(po.getCategory())
                .description(po.getDescription())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .version(po.getVersion())
                .build();
    }
}
