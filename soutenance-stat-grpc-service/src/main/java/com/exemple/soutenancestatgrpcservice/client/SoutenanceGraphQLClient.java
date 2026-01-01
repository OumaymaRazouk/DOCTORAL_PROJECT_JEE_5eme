package com.exemple.soutenancestatgrpcservice.client;

import com.exemple.soutenancestatgrpcservice.model.JuryMemberView;
import com.exemple.soutenancestatgrpcservice.model.SoutenanceView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SoutenanceGraphQLClient {

    private static final Logger log = LoggerFactory.getLogger(SoutenanceGraphQLClient.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${soutenance.service.url:http://localhost:8084/graphql}")
    private String graphqlUrl;

    public List<SoutenanceView> listPlannedSoutenances() {
        String query = "query SoutenancesAdmin($status: SoutenanceStatus) {" +
                "  soutenancesForAdmin(status: $status) {" +
                "    id doctorantId directeurId status titreTheseDefinitif datePlanifiee salle lieu" +
                "  }" +
                "}";
        Map<String, Object> variables = new HashMap<>();
        variables.put("status", "SOUTENANCE_PLANIFIEE");

        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("variables", variables);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(graphqlUrl, entity, Map.class);
        if (response == null) {
            log.warn("GraphQL response is null when calling {}", graphqlUrl);
            return List.of();
        }
        if (response.containsKey("errors")) {
            log.warn("GraphQL returned errors from {}: {}", graphqlUrl, response.get("errors"));
        }
        if (!response.containsKey("data") || response.get("data") == null) {
            log.warn("GraphQL response has no data when calling {}", graphqlUrl);
            return List.of();
        }
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        List<Map<String, Object>> list = (List<Map<String, Object>>) data.getOrDefault("soutenancesForAdmin", List.of());
        return list.stream().map(this::toViewWithJury).toList();
    }

    private SoutenanceView toViewWithJury(Map<String, Object> map) {
        SoutenanceView v = new SoutenanceView();
        v.setId((String) map.get("id"));
        v.setDoctorantId((String) map.get("doctorantId"));
        v.setDirecteurId((String) map.get("directeurId"));
        v.setStatus((String) map.get("status"));
        v.setTitreTheseDefinitif((String) map.get("titreTheseDefinitif"));
        v.setDatePlanifiee((String) map.get("datePlanifiee"));
        v.setSalle((String) map.get("salle"));
        v.setLieu((String) map.get("lieu"));
        List<JuryMemberView> jury = listJuryMembers(v.getId());
        v.setJury(jury);
        return v;
    }

    private List<JuryMemberView> listJuryMembers(String soutenanceId) {
        String query = "query JuryForSoutenance($soutenanceId: String!) {" +
                "  juryForSoutenance(soutenanceId: $soutenanceId) {" +
                "    nom prenom affiliation role" +
                "  }" +
                "}";
        Map<String, Object> variables = new HashMap<>();
        variables.put("soutenanceId", soutenanceId);

        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("variables", variables);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(graphqlUrl, entity, Map.class);
        if (response == null) {
            log.warn("GraphQL response is null when calling {}", graphqlUrl);
            return List.of();
        }
        if (response.containsKey("errors")) {
            log.warn("GraphQL returned errors from {}: {}", graphqlUrl, response.get("errors"));
        }
        if (!response.containsKey("data") || response.get("data") == null) {
            log.warn("GraphQL response has no data when calling {}", graphqlUrl);
            return List.of();
        }
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        List<Map<String, Object>> list = (List<Map<String, Object>>) data.getOrDefault("juryForSoutenance", List.of());
        return list.stream().map(this::toJuryView).toList();
    }

    private JuryMemberView toJuryView(Map<String, Object> map) {
        JuryMemberView v = new JuryMemberView();
        v.setNom((String) map.get("nom"));
        v.setPrenom((String) map.get("prenom"));
        v.setAffiliation((String) map.get("affiliation"));
        v.setRole((String) map.get("role"));
        return v;
    }
}
