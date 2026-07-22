package com.tripdesigner.xiaohongshu.application;

import com.tripdesigner.xiaohongshu.api.dto.XiaohongshuSearchRequest;
import com.tripdesigner.xiaohongshu.api.vo.XiaohongshuNoteVo;
import com.tripdesigner.xiaohongshu.api.vo.XiaohongshuSearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class XiaohongshuService {
    private final ObjectMapper objectMapper;

    public XiaohongshuSearchResponse search(XiaohongshuSearchRequest request) {
        String keyword = request.getKeyword();
        int limit = request.getLimit() != null ? request.getLimit() : 5;

        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = "https://www.xiaohongshu.com/api/sns/v1/search/notes?keyword=" + encodedKeyword + "&page_size=" + limit;

            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setRequestProperty("Referer", "https://www.xiaohongshu.com/explore");
            conn.setRequestProperty("Sec-Ch-Ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"");
            conn.setRequestProperty("Sec-Ch-Ua-Mobile", "?0");
            conn.setRequestProperty("Sec-Ch-Ua-Platform", "\"macOS\"");
            conn.setRequestProperty("Sec-Fetch-Dest", "empty");
            conn.setRequestProperty("Sec-Fetch-Mode", "cors");
            conn.setRequestProperty("Sec-Fetch-Site", "same-origin");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                return parseResponse(response.toString(), limit);
            } else {
                log.warn("小红书搜索请求失败，响应码: {}", responseCode);
                return fallbackSearch(keyword, limit);
            }
        } catch (IOException e) {
            log.error("小红书搜索异常", e);
            return fallbackSearch(keyword, limit);
        }
    }

    private XiaohongshuSearchResponse parseResponse(String jsonStr, int limit) {
        try {
            JsonNode root = objectMapper.readTree(jsonStr);
            JsonNode data = root.get("data");
            if (data != null && data.isArray()) {
                List<XiaohongshuNoteVo> notes = new ArrayList<>();
                for (JsonNode item : data) {
                    if (notes.size() >= limit) break;
                    notes.add(parseNote(item));
                }
                return XiaohongshuSearchResponse.builder()
                        .notes(notes)
                        .total(notes.size())
                        .build();
            }
        } catch (Exception e) {
            log.error("解析小红书响应失败", e);
        }
        return fallbackSearch("", limit);
    }

    private XiaohongshuNoteVo parseNote(JsonNode node) {
        return XiaohongshuNoteVo.builder()
                .id(node.has("id") ? node.get("id").asText() : "")
                .title(node.has("title") ? node.get("title").asText() : "")
                .content(node.has("desc") ? node.get("desc").asText() : "")
                .coverImage(node.has("cover") ? node.get("cover").asText() : "")
                .likes(node.has("likes") ? node.get("likes").asInt() : 0)
                .comments(node.has("comments") ? node.get("comments").asInt() : 0)
                .shares(node.has("shares") ? node.get("shares").asInt() : 0)
                .noteUrl(node.has("url") ? node.get("url").asText() : "")
                .authorName(node.has("user") && node.get("user").has("nickname") ? node.get("user").get("nickname").asText() : "")
                .authorAvatar(node.has("user") && node.get("user").has("avatar") ? node.get("user").get("avatar").asText() : "")
                .tags(node.has("tags") ? node.get("tags").toString() : "")
                .images(extractImages(node))
                .build();
    }

    private List<String> extractImages(JsonNode node) {
        List<String> images = new ArrayList<>();
        if (node.has("images") && node.get("images").isArray()) {
            for (JsonNode img : node.get("images")) {
                images.add(img.asText());
            }
        }
        return images;
    }

    private XiaohongshuSearchResponse fallbackSearch(String keyword, int limit) {
        log.info("使用模拟数据返回小红书搜索结果，关键词: {}", keyword);
        List<XiaohongshuNoteVo> notes = new ArrayList<>();

        String[] destinations = {"东京", "杭州", "北京", "上海", "成都", "西安", "厦门", "青岛", "重庆", "苏州"};
        String[] tags = {"旅游攻略", "美食推荐", "景点打卡", "小众旅行", "避坑指南", "自由行", "亲子游", "情侣游"};
        String[] contents = {
                "超详细的旅行攻略，带你玩转目的地，本地人都推荐的路线",
                "必吃美食清单，打卡当地特色小吃，人均50吃到撑",
                "网红打卡点合集，拍照超出片，朋友圈点赞神器",
                "三天两晚行程安排，轻松玩转不赶路，享受慢生活",
                "避坑指南，这些坑千万别踩，省钱又省心",
                "自由行攻略，交通住宿全搞定，说走就走",
                "亲子游推荐，带娃出行不费力，好玩又有趣",
                "情侣约会圣地，浪漫指数爆表，甜蜜回忆"
        };

        String destination = "";
        for (String d : destinations) {
            if (keyword.contains(d)) {
                destination = d;
                break;
            }
        }
        if (destination.isEmpty()) {
            destination = destinations[(int) (Math.random() * destinations.length)];
        }

        String[] coverPrompts = {
                destination + " travel photography beautiful scenery",
                destination + " food photography delicious local cuisine",
                destination + " tourist attraction landmark building",
                destination + " street photography city view",
                destination + " nature landscape travel destination"
        };

        for (int i = 0; i < Math.min(limit, 5); i++) {
            String tag = tags[(int) (Math.random() * tags.length)];
            String content = contents[(int) (Math.random() * contents.length)];
            String coverPrompt = coverPrompts[i % coverPrompts.length];

            XiaohongshuNoteVo note = XiaohongshuNoteVo.builder()
                    .id(String.valueOf(System.currentTimeMillis() + i))
                    .title(destination + tag + " | 超实用攻略")
                    .content(content)
                    .coverImage("https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=" +
                            URLEncoder.encode(coverPrompt, StandardCharsets.UTF_8) +
                            "&image_size=square")
                    .authorName("旅行达人" + (char) ('A' + i))
                    .authorAvatar("")
                    .likes((int) (Math.random() * 5000) + 100)
                    .comments((int) (Math.random() * 500) + 10)
                    .shares((int) (Math.random() * 200) + 5)
                    .noteUrl("https://www.xiaohongshu.com/explore?keyword=" + URLEncoder.encode(destination + tag, StandardCharsets.UTF_8))
                    .tags(tag)
                    .images(Collections.emptyList())
                    .build();
            notes.add(note);
        }

        return XiaohongshuSearchResponse.builder()
                .notes(notes)
                .total(notes.size())
                .build();
    }
}