package com.tripdesigner.multimodal.infrastructure;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.tripdesigner.common.typehandler.JsonbStringTypeHandler;
import lombok.Data;

import java.time.Instant;

/**
 * 多模态上传 PO。
 * recognition_result 字段使用 JSONB 类型存储识别结果 JSON。
 */
@Data
@TableName(value = "multimodal_uploads", autoResultMap = true)
public class MultimodalUploadPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String originalFilename;
    private String storedFilename;
    private String contentType;
    private Long fileSize;
    private String storagePath;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String recognitionResult;
    private Long generatedTripId;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
    @Version
    private Integer version;
}
