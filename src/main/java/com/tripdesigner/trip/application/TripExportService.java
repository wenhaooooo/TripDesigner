package com.tripdesigner.trip.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.response.ResultCode;
import com.tripdesigner.trip.api.vo.TripActivityVo;
import com.tripdesigner.trip.api.vo.TripDayVo;
import com.tripdesigner.trip.api.vo.TripDetailVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripExportService {

    private final TripAppService tripAppService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public byte[] exportToExcel(Long userId, Long tripId) {
        TripDetailVo detail = tripAppService.getDetailForUser(tripId, userId);
        try (Workbook workbook = new XSSFWorkbook()) {
            createTripSummarySheet(workbook, detail);
            createDailyScheduleSheet(workbook, detail);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("[TripExport] Failed to export Excel for trip {}: {}", tripId, e.getMessage(), e);
            throw new BizException(ResultCode.COMMON_INTERNAL_ERROR, "生成 Excel 失败");
        }
    }

    public byte[] exportToCsv(Long userId, Long tripId) {
        TripDetailVo detail = tripAppService.getDetailForUser(tripId, userId);
        StringBuilder sb = new StringBuilder();
        
        sb.append("行程概览\n");
        sb.append(String.format("标题,%s\n", escapeCsv(detail.getTitle())));
        sb.append(String.format("目的地,%s\n", escapeCsv(detail.getDestinationName())));
        sb.append(String.format("开始日期,%s\n", formatDate(detail.getStartDate())));
        sb.append(String.format("结束日期,%s\n", formatDate(detail.getEndDate())));
        sb.append(String.format("预算,%s\n", detail.getBudget() != null ? "¥" + detail.getBudget() : ""));
        sb.append(String.format("天数,%d\n\n", detail.getDays().size()));
        
        sb.append("每日行程\n");
        sb.append("天数,日期,活动名称,类别,开始时间,结束时间,地点,描述\n");
        
        for (TripDayVo day : detail.getDays()) {
            for (TripActivityVo activity : day.getActivities()) {
                sb.append(String.format("%d,%s,%s,%s,%s,%s,%s,%s\n",
                        day.getDayNumber(),
                        formatDate(day.getDate()),
                        escapeCsv(activity.getName()),
                        escapeCsv(activity.getCategory()),
                        formatTime(activity.getStartTime()),
                        formatTime(activity.getEndTime()),
                        escapeCsv(activity.getPlace()),
                        escapeCsv(activity.getDescription())
                ));
            }
        }
        
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportToIcs(Long userId, Long tripId) {
        TripDetailVo detail = tripAppService.getDetailForUser(tripId, userId);
        StringBuilder sb = new StringBuilder();
        
        sb.append("BEGIN:VCALENDAR\n");
        sb.append("VERSION:2.0\n");
        sb.append("PRODID:-//TripDesigner//Trip Designer//CN\n");
        sb.append("CALSCALE:GREGORIAN\n");
        sb.append(String.format("X-WR-CALNAME:%s\n", escapeIcs(detail.getTitle())));
        
        String tripUid = String.format("trip-%d@tripdesigner.com", tripId);
        String tripDtStart = formatIcsDate(detail.getStartDate());
        String tripDtEnd = formatIcsDate(detail.getEndDate().plusDays(1));
        
        sb.append("BEGIN:VEVENT\n");
        sb.append(String.format("UID:%s\n", tripUid));
        sb.append(String.format("DTSTART;VALUE=DATE:%s\n", tripDtStart));
        sb.append(String.format("DTEND;VALUE=DATE:%s\n", tripDtEnd));
        sb.append(String.format("SUMMARY:%s\n", escapeIcs(detail.getTitle())));
        sb.append(String.format("DESCRIPTION:%s\n", escapeIcs(buildTripDescription(detail))));
        sb.append(String.format("LOCATION:%s\n", escapeIcs(detail.getDestinationName())));
        sb.append("END:VEVENT\n");
        
        int activityIndex = 0;
        for (TripDayVo day : detail.getDays()) {
            for (TripActivityVo activity : day.getActivities()) {
                activityIndex++;
                String activityUid = String.format("trip-%d-activity-%d@tripdesigner.com", tripId, activityIndex);
                String dtStart = formatIcsDateTime(day.getDate(), activity.getStartTime());
                String dtEnd = formatIcsDateTime(day.getDate(), activity.getEndTime());
                
                sb.append("BEGIN:VEVENT\n");
                sb.append(String.format("UID:%s\n", activityUid));
                sb.append(String.format("DTSTART:%s\n", dtStart));
                sb.append(String.format("DTEND:%s\n", dtEnd));
                sb.append(String.format("SUMMARY:%s\n", escapeIcs(activity.getName())));
                if (activity.getCategory() != null) {
                    sb.append(String.format("CATEGORIES:%s\n", escapeIcs(activity.getCategory())));
                }
                if (activity.getPlace() != null) {
                    sb.append(String.format("LOCATION:%s\n", escapeIcs(activity.getPlace())));
                }
                if (activity.getDescription() != null) {
                    sb.append(String.format("DESCRIPTION:%s\n", escapeIcs(activity.getDescription())));
                }
                sb.append("END:VEVENT\n");
            }
        }
        
        sb.append("END:VCALENDAR\n");
        
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportToJson(Long userId, Long tripId) {
        TripDetailVo detail = tripAppService.getDetailForUser(tripId, userId);
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(detail).getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("[TripExport] Failed to export JSON for trip {}: {}", tripId, e.getMessage(), e);
            throw new BizException(ResultCode.COMMON_INTERNAL_ERROR, "生成 JSON 失败");
        }
    }

    private void createTripSummarySheet(Workbook workbook, TripDetailVo detail) {
        Sheet sheet = workbook.createSheet("行程概览");
        
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        
        int rowNum = 0;
        
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("行程信息");
        titleCell.setCellStyle(headerStyle);
        
        Row nameRow = sheet.createRow(rowNum++);
        nameRow.createCell(0).setCellValue("标题");
        nameRow.createCell(1).setCellValue(detail.getTitle());
        
        Row destRow = sheet.createRow(rowNum++);
        destRow.createCell(0).setCellValue("目的地");
        destRow.createCell(1).setCellValue(detail.getDestinationName());
        
        Row startRow = sheet.createRow(rowNum++);
        startRow.createCell(0).setCellValue("开始日期");
        startRow.createCell(1).setCellValue(formatDate(detail.getStartDate()));
        
        Row endRow = sheet.createRow(rowNum++);
        endRow.createCell(0).setCellValue("结束日期");
        endRow.createCell(1).setCellValue(formatDate(detail.getEndDate()));
        
        Row budgetRow = sheet.createRow(rowNum++);
        budgetRow.createCell(0).setCellValue("预算");
        budgetRow.createCell(1).setCellValue(detail.getBudget() != null ? "¥" + detail.getBudget() : "");
        
        Row dayCountRow = sheet.createRow(rowNum++);
        dayCountRow.createCell(0).setCellValue("天数");
        dayCountRow.createCell(1).setCellValue(detail.getDays().size());
        
        int activityCount = detail.getDays().stream()
                .mapToInt(d -> d.getActivities().size()).sum();
        Row activityCountRow = sheet.createRow(rowNum++);
        activityCountRow.createCell(0).setCellValue("活动总数");
        activityCountRow.createCell(1).setCellValue(activityCount);
        
        if (detail.getDescription() != null && !detail.getDescription().isBlank()) {
            Row descRow = sheet.createRow(rowNum++);
            descRow.createCell(0).setCellValue("描述");
            descRow.createCell(1).setCellValue(detail.getDescription());
        }
        
        sheet.setColumnWidth(0, 5000);
        sheet.setColumnWidth(1, 15000);
    }

    private void createDailyScheduleSheet(Workbook workbook, TripDetailVo detail) {
        Sheet sheet = workbook.createSheet("每日行程");
        
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        
        int rowNum = 0;
        
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"天数", "日期", "活动名称", "类别", "开始时间", "结束时间", "地点", "描述"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        for (TripDayVo day : detail.getDays()) {
            for (TripActivityVo activity : day.getActivities()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(day.getDayNumber());
                row.createCell(1).setCellValue(formatDate(day.getDate()));
                row.createCell(2).setCellValue(activity.getName());
                row.createCell(3).setCellValue(activity.getCategory() != null ? activity.getCategory() : "");
                row.createCell(4).setCellValue(formatTime(activity.getStartTime()));
                row.createCell(5).setCellValue(formatTime(activity.getEndTime()));
                row.createCell(6).setCellValue(activity.getPlace() != null ? activity.getPlace() : "");
                row.createCell(7).setCellValue(activity.getDescription() != null ? activity.getDescription() : "");
            }
        }
        
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    private String formatTime(LocalTime time) {
        return time != null ? time.format(TIME_FORMATTER) : "";
    }

    private String formatIcsDate(LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.BASIC_ISO_DATE) : "";
    }

    private String formatIcsDateTime(LocalDate date, LocalTime time) {
        if (date == null) return "";
        if (time == null) {
            return formatIcsDate(date) + "T000000";
        }
        return date.format(DateTimeFormatter.BASIC_ISO_DATE) + "T" + time.format(DateTimeFormatter.ofPattern("HHmmss"));
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String escapeIcs(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n");
    }

    private String buildTripDescription(TripDetailVo detail) {
        StringBuilder sb = new StringBuilder();
        sb.append("目的地：").append(detail.getDestinationName()).append("\n");
        sb.append("日期：").append(formatDate(detail.getStartDate()))
                .append(" ~ ").append(formatDate(detail.getEndDate())).append("\n");
        if (detail.getBudget() != null) {
            sb.append("预算：¥").append(detail.getBudget()).append("\n");
        }
        sb.append("\n行程安排：\n");
        for (TripDayVo day : detail.getDays()) {
            sb.append("Day ").append(day.getDayNumber()).append("：\n");
            for (TripActivityVo activity : day.getActivities()) {
                sb.append("  - ").append(formatTime(activity.getStartTime()))
                        .append(" ").append(activity.getName())
                        .append("（").append(activity.getPlace()).append("）\n");
            }
        }
        return sb.toString();
    }
}
