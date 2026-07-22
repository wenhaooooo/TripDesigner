package com.tripdesigner.ai.trip;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Component
public class LanguageDetector {

    private static final Set<Character> CHINESE_CHARACTERS = new HashSet<>();
    private static final Set<Character> JAPANESE_CHARACTERS = new HashSet<>();
    private static final Set<Character> KOREAN_CHARACTERS = new HashSet<>();
    
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");
    private static final Pattern JAPANESE_PATTERN = Pattern.compile("[\\u3040-\\u30ff\\u3400-\\u4dbf\\u4e00-\\u9fa5\\uff66-\\uff9f]");
    private static final Pattern KOREAN_PATTERN = Pattern.compile("[\\uac00-\\ud7af]");
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]");

    static {
        for (char c = '\u4e00'; c <= '\u9fa5'; c++) {
            CHINESE_CHARACTERS.add(c);
        }
        for (char c = '\u3040'; c <= '\u30ff'; c++) {
            JAPANESE_CHARACTERS.add(c);
        }
        for (char c = '\u3400'; c <= '\u4dbf'; c++) {
            JAPANESE_CHARACTERS.add(c);
        }
        for (char c = '\uff66'; c <= '\uff9f'; c++) {
            JAPANESE_CHARACTERS.add(c);
        }
        for (char c = '\uac00'; c <= '\ud7af'; c++) {
            KOREAN_CHARACTERS.add(c);
        }
    }

    private static final List<String> CHINESE_KEYWORDS = Arrays.asList(
            "我", "你", "他", "她", "它", "们", "的", "是", "在", "有", "和", "了", "我要", "帮我", "推荐",
            "旅行", "旅游", "行程", "攻略", "景点", "酒店", "机票", "火车票", "美食", "预算", "计划",
            "去", "来", "玩", "看", "吃", "住", "行", "买", "游", "逛", "走", "回", "出发", "到达",
            "天", "日", "周", "月", "年", "时间", "日期", "价格", "便宜", "贵", "好", "坏", "推荐",
            "日本", "东京", "京都", "大阪", "巴黎", "伦敦", "纽约", "北京", "上海", "杭州", "成都",
            "泰国", "清迈", "曼谷", "普吉", "巴厘岛", "马尔代夫", "三亚", "桂林", "厦门", "西安",
            "请", "可以", "吗", "吧", "呢", "啊", "哦", "呀", "嗯", "嗨", "嘿", "喂"
    );

    private static final List<String> ENGLISH_KEYWORDS = Arrays.asList(
            "i", "you", "he", "she", "it", "we", "they", "want", "need", "plan", "trip", "travel",
            "vacation", "holiday", "tour", "journey", "destination", "place", "city", "country",
            "hotel", "flight", "train", "food", "restaurant", "attraction", "sightseeing",
            "budget", "price", "cost", "days", "weeks", "months", "go", "visit", "see", "eat",
            "stay", "book", "reserve", "find", "search", "recommend", "suggest", "help", "please",
            "can", "could", "would", "should", "may", "might", "will", "shall", "must", "let",
            "tokyo", "kyoto", "osaka", "paris", "london", "new york", "beijing", "shanghai",
            "thailand", "chiang mai", "bali", "maldives", "sanya", "guilin", "phuket"
    );

    private static final List<String> JAPANESE_KEYWORDS = Arrays.asList(
            "私", "あなた", "彼", "彼女", "それ", "旅行", "旅", "日程", "計画", "予算", "ホテル",
            "フライト", "電車", "食べ物", "レストラン", "観光", "名所", "目的地", "都市", "国",
            "行く", "見る", "食べる", "泊まる", "予約", "探す", "推奨", "助けて", "お願い",
            "できますか", "はい", "いいえ", "東京", "京都", "大阪", "パリ", "ロンドン", "ニューヨーク"
    );

    private static final List<String> KOREAN_KEYWORDS = Arrays.asList(
            "나", "너", "그", "그녀", "것", "여행", "일정", "계획", "예산", "호텔", "비행기", "기차",
            "음식", "레스토랑", "관광", "명소", "목적지", "도시", "나라", "가다", "보다", "먹다",
            "있다", "예약", "찾다", "추천", "도와주세요", "제발", "할 수 있나요", "네", "아니요",
            "도쿄", "교토", "오사카", "파리", "런던", "뉴욕", "베이징", "상하이"
    );

    public String detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "zh";
        }

        text = text.toLowerCase().trim();

        int chineseScore = countChineseChars(text);
        int japaneseScore = countJapaneseChars(text);
        int koreanScore = countKoreanChars(text);
        int englishScore = countEnglishWords(text);

        int chineseKeywordMatches = countKeywordMatches(text, CHINESE_KEYWORDS);
        int englishKeywordMatches = countKeywordMatches(text, ENGLISH_KEYWORDS);
        int japaneseKeywordMatches = countKeywordMatches(text, JAPANESE_KEYWORDS);
        int koreanKeywordMatches = countKeywordMatches(text, KOREAN_KEYWORDS);

        int totalChinese = chineseScore * 2 + chineseKeywordMatches * 5;
        int totalEnglish = englishScore * 2 + englishKeywordMatches * 5;
        int totalJapanese = japaneseScore * 2 + japaneseKeywordMatches * 5;
        int totalKorean = koreanScore * 2 + koreanKeywordMatches * 5;

        int maxScore = Math.max(Math.max(totalChinese, totalEnglish), Math.max(totalJapanese, totalKorean));

        String detected;
        if (maxScore == totalChinese) {
            detected = "zh";
        } else if (maxScore == totalEnglish) {
            detected = "en";
        } else if (maxScore == totalJapanese) {
            detected = "ja";
        } else if (maxScore == totalKorean) {
            detected = "ko";
        } else {
            detected = "zh";
        }

        log.info("[LanguageDetector] Text: \"{}\", Chinese:{}, English:{}, Japanese:{}, Korean:{}, Detected: {}",
                truncate(text, 100), totalChinese, totalEnglish, totalJapanese, totalKorean, detected);

        return detected;
    }

    public String getLanguageName(String code) {
        return switch (code) {
            case "zh" -> "Chinese";
            case "en" -> "English";
            case "ja" -> "Japanese";
            case "ko" -> "Korean";
            default -> "English";
        };
    }

    public String getLanguagePrompt(String code) {
        return switch (code) {
            case "zh" -> "Chinese (中文)";
            case "en" -> "English";
            case "ja" -> "Japanese (日本語)";
            case "ko" -> "Korean (한국어)";
            default -> "English";
        };
    }

    private int countChineseChars(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (CHINESE_CHARACTERS.contains(c)) {
                count++;
            }
        }
        return count;
    }

    private int countJapaneseChars(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (JAPANESE_CHARACTERS.contains(c)) {
                count++;
            }
        }
        return count;
    }

    private int countKoreanChars(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (KOREAN_CHARACTERS.contains(c)) {
                count++;
            }
        }
        return count;
    }

    private int countEnglishWords(String text) {
        String[] words = text.split("[^a-zA-Z]+");
        int count = 0;
        for (String word : words) {
            if (word.length() >= 2) {
                count++;
            }
        }
        return count;
    }

    private int countKeywordMatches(String text, List<String> keywords) {
        int count = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                count++;
            }
        }
        return count;
    }

    private String truncate(String s, int max) {
        return (s == null || s.length() <= max) ? s : s.substring(0, max) + "...";
    }
}