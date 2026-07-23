package com.tripdesigner.multimodal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/**
 * 多模态上传配置。
 * 上传文件存储目录、允许的文件类型、最大文件大小等。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.multimodal")
public class MultimodalProperties {
    /** 上传文件根目录 */
    private String storageDir = System.getProperty("java.io.tmpdir") + "/tripdesigner-uploads";
    /** 允许的文件类型 */
    private String[] allowedTypes = {"image/jpeg", "image/png", "image/webp", "image/gif"};
    /** 最大文件大小（字节），默认 10MB */
    private long maxFileSize = 10L * 1024 * 1024;
    /** 是否启用 AI 视觉识别（关闭时返回 fallback 结果） */
    private boolean recognitionEnabled = true;
    /** 多模态模型名称 */
    private String model = "bakllava";

    public Path resolveStoragePath(String filename) {
        return Path.of(storageDir).resolve(filename);
    }
}
