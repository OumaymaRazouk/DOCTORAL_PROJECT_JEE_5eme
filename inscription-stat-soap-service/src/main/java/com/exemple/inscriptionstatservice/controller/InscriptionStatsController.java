package com.exemple.inscriptionstatservice.controller;

import com.exemple.inscriptionstatservice.model.InscriptionYearSummary;
import com.exemple.inscriptionstatservice.service.InscriptionStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stats/inscriptions")
public class InscriptionStatsController {

    private final InscriptionStatsService statsService;

    public InscriptionStatsController(InscriptionStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public ResponseEntity<List<InscriptionYearSummary>> getStats(@RequestParam(value = "annee", required = false) String annee) {
        return ResponseEntity.ok(statsService.getSummaries(annee));
    }
}
