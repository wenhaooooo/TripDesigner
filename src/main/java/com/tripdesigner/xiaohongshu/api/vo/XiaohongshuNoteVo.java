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
public class XiaohongshuNoteVo {
    private String id;
    private String title;
    private String content;
    private String coverImage;
    private List<String> images;
    private String authorName;
    private String authorAvatar;
    private Integer likes;
    private Integer comments;
    private Integer shares;
    private String noteUrl;
    private String tags;
}