package com.exemple.inscriptionstatservice.ws;

import com.exemple.inscriptionstatservice.model.InscriptionView;
import com.exemple.inscriptionstatservice.model.InscriptionYearSummary;
import com.exemple.inscriptionstatservice.service.InscriptionStatsService;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.util.List;
import java.util.stream.Collectors;

@Endpoint
public class DoctorantInscriptionEndpoint {

    private static final String NAMESPACE = "http://exemple.com/inscriptionstats";

    private final InscriptionStatsService statsService;

    public DoctorantInscriptionEndpoint(InscriptionStatsService statsService) {
        this.statsService = statsService;
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "GetInscriptionsParAnneeRequest")
    @ResponsePayload
    public GetInscriptionsParAnneeResponse getInscriptionsParAnnee(@RequestPayload GetInscriptionsParAnneeRequest request) {
        String annee = request != null ? request.getAnneeAcademique() : null;
        List<InscriptionYearSummary> summaries = statsService.getSummaries(annee);
        GetInscriptionsParAnneeResponse response = new GetInscriptionsParAnneeResponse();
        List<InscriptionYearSummaryType> types = summaries.stream()
                .map(this::toType)
                .collect(Collectors.toList());
        response.setSummaries(types);
        return response;
    }

    private InscriptionYearSummaryType toType(InscriptionYearSummary s) {
        InscriptionYearSummaryType t = new InscriptionYearSummaryType();
        t.setAnneeAcademique(s.getAnneeAcademique());
        t.setTotal(s.getTotal());
        if (s.getInscriptions() != null) {
            List<InscriptionViewType> views = s.getInscriptions().stream()
                    .map(this::toViewType)
                    .collect(Collectors.toList());
            t.setInscriptions(views);
        }
        return t;
    }

    private InscriptionViewType toViewType(InscriptionView v) {
        InscriptionViewType t = new InscriptionViewType();
        t.setId(v.getId());
        t.setType(v.getType());
        t.setStatus(v.getStatus());
        t.setDoctorantId(v.getDoctorantId());
        t.setDirecteurId(v.getDirecteurId());
        t.setSujetThese(v.getSujetThese());
        t.setLaboratoire(v.getLaboratoire());
        t.setSpecialite(v.getSpecialite());
        t.setAnneeAcademique(v.getAnneeAcademique());
        t.setNom(v.getNom());
        t.setEmailContact(v.getEmailContact());
        return t;
    }
}
