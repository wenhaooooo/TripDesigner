package com.tripdesigner.ai.trip;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class SystemTools {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Tool(description = "Get current system date and time. Use this before creating any trip to get accurate dates. " +
            "Returns current date (yyyy-MM-dd), day of week, month, year, and current time.")
    public String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        
        Map<String, String> result = new HashMap<>();
        result.put("currentDate", today.format(DATE_FORMATTER));
        result.put("currentTime", now.format(DATETIME_FORMATTER));
        result.put("year", String.valueOf(today.getYear()));
        result.put("month", String.valueOf(today.getMonthValue()));
        result.put("day", String.valueOf(today.getDayOfMonth()));
        result.put("dayOfWeek", today.getDayOfWeek().name());
        result.put("weekOfYear", String.valueOf(today.getDayOfWeek().getValue()));
        
        String entries = result.entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\": \"" + e.getValue() + "\"")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        
        String json = "{" + entries + "}";
        
        log.info("[SystemTools] getCurrentDateTime: {}", json);
        return json;
    }

    @Tool(description = "Get current date only. Returns current date in yyyy-MM-dd format.")
    public String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    @Tool(description = "Get current year. Returns the current year as integer.")
    public Integer getCurrentYear() {
        return LocalDate.now().getYear();
    }
}