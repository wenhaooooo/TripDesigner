package com.tripdesigner.multimodal.domain;

import java.util.List;
import java.util.Optional;

/**
 * 多模态上传仓储接口。
 */
public interface MultimodalUploadRepository {
    MultimodalUpload save(MultimodalUpload upload);
    Optional<MultimodalUpload> findById(Long id);
    List<MultimodalUpload> findByUserId(Long userId);
    List<MultimodalUpload> findByGeneratedTripId(Long tripId);
    void deleteById(Long id);
}
