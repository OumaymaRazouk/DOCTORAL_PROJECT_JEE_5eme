package com.exemple.soutenancestatgrpcservice.service;

import com.exemple.soutenancestatgrpcservice.client.SoutenanceGraphQLClient;
import com.exemple.soutenancestatgrpcservice.model.SoutenanceView;
import com.exemple.soutenancestatgrpcservice.model.WeeklySoutenanceSummary;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.time.temporal.TemporalAccessor;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SoutenanceStatsService {

    private final SoutenanceGraphQLClient graphQLClient;

    public SoutenanceStatsService(SoutenanceGraphQLClient graphQLClient) {
        this.graphQLClient = graphQLClient;
    }

    public List<WeeklySoutenanceSummary> getWeeklyStats(String startDate, String endDate, boolean includeDetails) {
        LocalDate start = LocalDate.MIN;
        LocalDate end = LocalDate.MAX;
        if (startDate != null && !startDate.isBlank()) {
            start = LocalDate.parse(startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            end = LocalDate.parse(endDate);
        }

        final LocalDate startFinal = start;
        final LocalDate endFinal = end;

        List<SoutenanceView> planned = graphQLClient.listPlannedSoutenances();
        WeekFields wf = WeekFields.of(Locale.getDefault());

        Map<String, List<SoutenanceView>> byWeek = planned.stream()
                .filter(v -> v.getDatePlanifiee() != null && !v.getDatePlanifiee().isBlank())
                .filter(v -> isBetween(v.getDatePlanifiee(), startFinal, endFinal))
                .collect(Collectors.groupingBy(v -> weekKey(v.getDatePlanifiee(), wf)));

        return byWeek.entrySet().stream()
                .filter(e -> e.getKey() != null && !e.getKey().equals("INVALID"))
                .map(e -> toSummary(e.getKey(), e.getValue(), wf, includeDetails))
                .sorted(Comparator.comparing(WeeklySoutenanceSummary::getWeekStart))
                .collect(Collectors.toList());
    }

    private boolean isBetween(String dateTimeStr, LocalDate start, LocalDate end) {
        LocalDateTime dt = parseDateTime(dateTimeStr);
        if (dt == null) {
            return false;
        }
        LocalDate d = dt.toLocalDate();
        return (d.isEqual(start) || d.isAfter(start)) && (d.isEqual(end) || d.isBefore(end));
    }

    private String weekKey(String dateTimeStr, WeekFields wf) {
        LocalDateTime dt = parseDateTime(dateTimeStr);
        if (dt == null) {
            return "INVALID";
        }
        LocalDate d = dt.toLocalDate();
        int week = d.get(wf.weekOfWeekBasedYear());
        int year = d.get(wf.weekBasedYear());
        return year + "-W" + week;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            TemporalAccessor ta = DateTimeFormatter.ISO_DATE_TIME
                    .parseBest(value, OffsetDateTime::from, ZonedDateTime::from, LocalDateTime::from);
            if (ta instanceof OffsetDateTime odt) {
                return odt.toLocalDateTime();
            }
            if (ta instanceof ZonedDateTime zdt) {
                return zdt.toLocalDateTime();
            }
            if (ta instanceof LocalDateTime ldt) {
                return ldt;
            }
            return null;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private WeeklySoutenanceSummary toSummary(String key, List<SoutenanceView> list, WeekFields wf, boolean includeDetails) {
        WeeklySoutenanceSummary s = new WeeklySoutenanceSummary();
        String[] parts = key.split("-W");
        int year = Integer.parseInt(parts[0]);
        int week = Integer.parseInt(parts[1]);
        LocalDate weekStart = LocalDate.now().withYear(year).with(wf.weekOfWeekBasedYear(), week).with(wf.dayOfWeek(), 1);
        LocalDate weekEnd = weekStart.plusDays(6);
        s.setWeekStart(weekStart.toString());
        s.setWeekEnd(weekEnd.toString());
        s.setTotal(list.size());
        if (includeDetails) {
            s.setSoutenances(list);
        }
        return s;
    }
}
