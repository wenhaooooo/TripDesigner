package com.tripdesigner.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tripdesigner.knowledge.infrastructure.po.ImagePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 图片 MyBatis Mapper 接口，对应 kb_images 表。
 */
@Mapper
public interface ImageMapper extends BaseMapper<ImagePO> {
}
