package com.tripdesigner.trip.application;
/**
 * 目的地应用服务。
 * 处理目的地的业务逻辑，支持按国家和分类查询。
 */

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.trip.api.dto.CreateDestinationRequest;
import com.tripdesigner.trip.api.dto.UpdateDestinationRequest;
import com.tripdesigner.trip.api.vo.DestinationVo;
import com.tripdesigner.trip.domain.Destination;
import com.tripdesigner.trip.domain.DestinationRepository;
import com.tripdesigner.trip.infrastructure.DestinationMapper;
import com.tripdesigner.trip.infrastructure.DestinationPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DestinationAppService {
    private final DestinationRepository destRepo;
    private final DestinationMapper destMapper;

    @Transactional(readOnly = true)
    public List<DestinationVo> list() {
        return destMapper.selectList(
                Wrappers.<DestinationPO>lambdaQuery().orderByAsc(DestinationPO::getName))
                .stream()
                .map(this::fromPO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DestinationVo> listByCountry(String country) {
        return destRepo.findByCountry(country).stream()
                .map(DestinationVo::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public DestinationVo create(CreateDestinationRequest req) {
        Destination dest = Destination.create(req.getName(), req.getCountry(), req.getRegion(),
                req.getCategory(), req.getDescription());
        return DestinationVo.from(destRepo.save(dest));
    }

    @Transactional(readOnly = true)
    public DestinationVo get(Long destId) {
        Destination dest = destRepo.findById(destId)
                .orElseThrow(() -> new BizException(ResultCode.DESTINATION_NOT_FOUND));
        return DestinationVo.from(dest);
    }

    @Transactional
    public DestinationVo update(Long destId, UpdateDestinationRequest req) {
        Destination dest = destRepo.findById(destId)
                .orElseThrow(() -> new BizException(ResultCode.DESTINATION_NOT_FOUND));
        Destination updated = dest.withUpdatedFields(req.getName(), req.getCountry(),
                req.getRegion(), req.getCategory(), req.getDescription());
        return DestinationVo.from(destRepo.save(updated));
    }

    @Transactional
    public void delete(Long destId) {
        destRepo.findById(destId)
                .orElseThrow(() -> new BizException(ResultCode.DESTINATION_NOT_FOUND));
        destRepo.deleteById(destId);
    }

    private DestinationVo fromPO(DestinationPO po) {
        return DestinationVo.builder()
                .id(po.getId())
                .name(po.getName())
                .country(po.getCountry())
                .region(po.getRegion())
                .category(po.getCategory())
                .description(po.getDescription())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
