package com.exemple.soutenancestatgrpcservice.service;

import com.exemple.soutenancestatgrpcservice.client.SoutenanceGraphQLClient;
import com.exemple.soutenancestatgrpcservice.model.SoutenanceView;
import com.exemple.soutenancestatgrpcservice.model.WeeklySoutenanceSummary;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
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
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusWeeks(4);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now().plusWeeks(4);

        List<SoutenanceView> planned = graphQLClient.listPlannedSoutenances();
        WeekFields wf = WeekFields.of(Locale.getDefault());

        Map<String, List<SoutenanceView>> byWeek = planned.stream()
                .filter(v -> v.getDatePlanifiee() != null)
                .filter(v -> isBetween(v.getDatePlanifiee(), start, end))
                .collect(Collectors.groupingBy(v -> weekKey(v.getDatePlanifiee(), wf)));

        return byWeek.entrySet().stream()
                .map(e -> toSummary(e.getKey(), e.getValue(), wf, includeDetails))
                .sorted(Comparator.comparing(WeeklySoutenanceSummary::getWeekStart))
                .collect(Collectors.toList());
    }

    private boolean isBetween(String dateTimeStr, LocalDate start, LocalDate end) {
        LocalDateTime dt = LocalDateTime.parse(dateTimeStr);
        LocalDate d = dt.toLocalDate();
        return (d.isEqual(start) || d.isAfter(start)) && (d.isEqual(end) || d.isBefore(end));
    }

    private String weekKey(String dateTimeStr, WeekFields wf) {
        LocalDateTime dt = LocalDateTime.parse(dateTimeStr);
        LocalDate d = dt.toLocalDate();
        int week = d.get(wf.weekOfWeekBasedYear());
        int year = d.get(wf.weekBasedYear());
        return year + "-W" + week;
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
