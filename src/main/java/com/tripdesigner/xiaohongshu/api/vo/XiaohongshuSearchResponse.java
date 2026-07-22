package com.tripdesigner.xiaohongshu.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XiaohongshuSearchResponse {
    private List<XiaohongshuNoteVo> notes;
    private Integer total;
}