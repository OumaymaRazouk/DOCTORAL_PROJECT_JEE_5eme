package com.exemple.soutenancestatgrpcservice.controller;

import com.exemple.soutenancestatgrpcservice.model.WeeklySoutenanceSummary;
import com.exemple.soutenancestatgrpcservice.service.SoutenanceStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stats/soutenances")
public class SoutenanceStatsController {

    private final SoutenanceStatsService statsService;

    public SoutenanceStatsController(SoutenanceStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/weekly")
    public ResponseEntity<List<WeeklySoutenanceSummary>> getWeekly(
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end,
            @RequestParam(value = "includeDetails", required = false, defaultValue = "false") boolean includeDetails
    ) {
        return ResponseEntity.ok(statsService.getWeeklyStats(start, end, includeDetails));
    }
}
