package com.exemple.inscriptionstatservice.service;

import com.exemple.inscriptionstatservice.client.InscriptionClient;
import com.exemple.inscriptionstatservice.model.InscriptionView;
import com.exemple.inscriptionstatservice.model.InscriptionYearSummary;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InscriptionStatsService {

    private final InscriptionClient inscriptionClient;

    public InscriptionStatsService(InscriptionClient inscriptionClient) {
        this.inscriptionClient = inscriptionClient;
    }

    public List<InscriptionYearSummary> getSummaries(String anneeAcademique) {
        List<InscriptionView> all = inscriptionClient.listAll();
        if (all == null || all.isEmpty()) {
            return List.of();
        }
        List<InscriptionView> filtered = all;
        if (anneeAcademique != null && !anneeAcademique.isBlank()) {
            filtered = all.stream()
                    .filter(i -> anneeAcademique.equals(i.getAnneeAcademique()))
                    .collect(Collectors.toList());
        }
        Map<String, List<InscriptionView>> byYear = filtered.stream()
                .filter(i -> i.getAnneeAcademique() != null && !i.getAnneeAcademique().isBlank())
                .collect(Collectors.groupingBy(InscriptionView::getAnneeAcademique));
        return byYear.entrySet().stream()
                .map(e -> {
                    InscriptionYearSummary s = new InscriptionYearSummary();
                    s.setAnneeAcademique(e.getKey());
                    s.setTotal(e.getValue().size());
                    s.setInscriptions(e.getValue());
                    return s;
                })
                .sorted(Comparator.comparing(InscriptionYearSummary::getAnneeAcademique).reversed())
                .collect(Collectors.toList());
    }
}
