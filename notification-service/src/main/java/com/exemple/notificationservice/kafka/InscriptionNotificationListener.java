package com.exemple.notificationservice.kafka;

import com.exemple.notificationservice.clients.UserClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.annotation.Retry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class InscriptionNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(InscriptionNotificationListener.class);

    private final ObjectMapper objectMapper;
    private final JavaMailSender mailSender;
    private final UserClient userClient;

    @Value("${app.notifications.email.from:no-reply@doctorat.local}")
    private String fromAddress;

    @Value("${app.notifications.email.to:admin@doctorat.local}")
    private String defaultToAddress;

    @Value("${app.notifications.email.to.doctorant:}")
    private String doctorantTo;

    @Value("${app.notifications.email.to.directeur:}")
    private String directeurTo;

    @Value("${app.notifications.email.to.admin:}")
    private String adminTo;

    public InscriptionNotificationListener(ObjectMapper objectMapper,
                                           JavaMailSender mailSender,
                                           UserClient userClient) {
        this.objectMapper = objectMapper;
        this.mailSender = mailSender;
        this.userClient = userClient;
    }

    @KafkaListener(topics = "${app.notifications.topic:inscription-events}", groupId = "notification-service")
    @Retry(name = "notificationEmail")
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("Notification recue depuis Kafka: topic={}, key={}, payload={}",
                record.topic(), record.key(), record.value());
        try {
            JsonNode node = objectMapper.readTree(record.value());
            String eventType = node.path("eventType").asText("EVENEMENT");
            String doctorantId = node.path("doctorantId").asText("");
            String directeurId = node.path("directeurId").asText("");
            String status = node.path("status").asText("");

            String inscriptionId = node.path("inscriptionId").asText("");
            String inscriptionType = node.path("inscriptionType").asText("");
            String anneeAcademique = node.path("anneeAcademique").asText("");

            String soutenanceId = node.path("soutenanceId").asText("");
            String datePlanifiee = node.path("datePlanifiee").asText("");
            String salle = node.path("salle").asText("");
            String lieu = node.path("lieu").asText("");

            String defaultDoctorant = isBlank(doctorantTo) ? defaultToAddress : doctorantTo;
            String defaultDirecteur = isBlank(directeurTo) ? defaultToAddress : directeurTo;
            String adminEmail = isBlank(adminTo) ? defaultToAddress : adminTo;

            String doctorantEmail = resolveEmail(doctorantId, defaultDoctorant);
            String directeurEmail = resolveEmail(directeurId, defaultDirecteur);

            boolean handled = false;

            // 1. INSCRIPTION / REINSCRIPTION (module 2 + 3)
            if (!inscriptionId.isEmpty()) {
                boolean isReinscription = "REINSCRIPTION".equalsIgnoreCase(inscriptionType);
                String typeLabel = isReinscription ? "réinscription" : "inscription";

                switch (eventType) {
                    case "INSCRIPTION_SUBMITTED" -> {
                        // SOUMIS -> doctorant + directeur
                        String subject = capitalize(typeLabel) + " soumise";
                        String body = "La " + typeLabel + " a été soumise.\n" +
                                "Dossier: " + inscriptionId + "\n" +
                                (!anneeAcademique.isEmpty() ? ("Année académique: " + anneeAcademique + "\n") : "") +
                                (!doctorantId.isEmpty() ? ("Doctorant: " + doctorantId + "\n") : "") +
                                (!directeurId.isEmpty() ? ("Directeur: " + directeurId + "\n") : "");
                        sendMail(subject, body, doctorantEmail, directeurEmail);
                        handled = true;
                    }
                    case "INSCRIPTION_DIRECTOR_APPROVED" -> {
                        // VALIDÉ_DIRECTEUR -> doctorant + admin
                        String subject = capitalize(typeLabel) + " validée par le directeur";
                        String body = "La " + typeLabel + " a été validée par le directeur.\n" +
                                "Dossier: " + inscriptionId + "\n" +
                                (!anneeAcademique.isEmpty() ? ("Année académique: " + anneeAcademique + "\n") : "") +
                                (!doctorantId.isEmpty() ? ("Doctorant: " + doctorantId + "\n") : "") +
                                (!directeurId.isEmpty() ? ("Directeur: " + directeurId + "\n") : "");
                        sendMail(subject, body, doctorantEmail, adminEmail);
                        handled = true;
                    }
                    case "INSCRIPTION_ADMIN_APPROVED" -> {
                        // VALIDÉ_ADMIN -> doctorant ("acceptée")
                        String subject = capitalize(typeLabel) + " acceptée";
                        String body = "Votre " + typeLabel + " a été acceptée par l'administration.\n" +
                                "Dossier: " + inscriptionId + "\n" +
                                (!anneeAcademique.isEmpty() ? ("Année académique: " + anneeAcademique + "\n") : "");
                        sendMail(subject, body, doctorantEmail);
                        handled = true;
                    }
                    case "REINSCRIPTION_DURATION_ALERT" -> {
                        // Alerte durée doctorat > 5 ans -> admin
                        String subject = "Alerte durée doctorat > 5 ans";
                        String body = "Alerte sur la durée du doctorat (>5 ans) pour le doctorant " + doctorantId + "\n" +
                                "Dossier de réinscription: " + inscriptionId + "\n" +
                                (!anneeAcademique.isEmpty() ? ("Année académique: " + anneeAcademique + "\n") : "");
                        sendMail(subject, body, adminEmail);
                        handled = true;
                    }
                    default -> {
                    }
                }
            }

            // 2. SOUTENANCE (module 2 complément)
            if (!handled && !soutenanceId.isEmpty() && "SOUTENANCE_UPDATED".equals(eventType)) {
                switch (status) {
                    case "SOUMIS" -> {
                        // SOUMIS -> Doctorant + Directeur
                        String subject = "Soutenance soumise";
                        String body = "Le dossier de soutenance a été soumis.\n" +
                                "Soutenance: " + soutenanceId + "\n" +
                                (!doctorantId.isEmpty() ? ("Doctorant: " + doctorantId + "\n") : "") +
                                (!directeurId.isEmpty() ? ("Directeur: " + directeurId + "\n") : "");
                        sendMail(subject, body, doctorantEmail, directeurEmail);
                        handled = true;
                    }
                    case "PREREQUIS_VALIDES" -> {
                        // PREREQUIS_OK -> Directeur "Vérifiez jury"
                        String subject = "Prérequis soutenance validés - vérifier le jury";
                        String body = "Les prérequis de soutenance sont validés. Merci de vérifier / proposer le jury.\n" +
                                "Soutenance: " + soutenanceId + "\n" +
                                (!doctorantId.isEmpty() ? ("Doctorant: " + doctorantId + "\n") : "");
                        sendMail(subject, body, directeurEmail);
                        handled = true;
                    }
                    case "JURY_PROPOSE" -> {
                        // JURY_PROPOSE -> Admin
                        String subject = "Jury de soutenance proposé";
                        String body = "Un jury de soutenance a été proposé.\n" +
                                "Soutenance: " + soutenanceId + "\n" +
                                (!doctorantId.isEmpty() ? ("Doctorant: " + doctorantId + "\n") : "") +
                                (!directeurId.isEmpty() ? ("Directeur: " + directeurId + "\n") : "");
                        sendMail(subject, body, adminEmail);
                        handled = true;
                    }
                    case "SOUTENANCE_AUTORISEE" -> {
                        // AUTORISE -> Tous "Soutenance autorisée"
                        String subject = "Soutenance autorisée";
                        String body = "La soutenance a été autorisée.\n" +
                                "Soutenance: " + soutenanceId + "\n" +
                                (!doctorantId.isEmpty() ? ("Doctorant: " + doctorantId + "\n") : "") +
                                (!directeurId.isEmpty() ? ("Directeur: " + directeurId + "\n") : "");
                        sendMail(subject, body, doctorantEmail, directeurEmail, adminEmail);
                        handled = true;
                    }
                    case "SOUTENANCE_PLANIFIEE" -> {
                        // PLANIFIÉ -> Tous avec date/heure/lieu
                        String subject = "Soutenance planifiée";
                        StringBuilder body = new StringBuilder("La soutenance a été planifiée.\n");
                        body.append("Soutenance: ").append(soutenanceId).append("\n");
                        if (!doctorantId.isEmpty()) body.append("Doctorant: ").append(doctorantId).append("\n");
                        if (!directeurId.isEmpty()) body.append("Directeur: ").append(directeurId).append("\n");
                        if (!datePlanifiee.isEmpty()) body.append("Date/heure: ").append(datePlanifiee).append("\n");
                        if (!salle.isEmpty()) body.append("Salle: ").append(salle).append("\n");
                        if (!lieu.isEmpty()) body.append("Lieu: ").append(lieu).append("\n");
                        sendMail(subject, body.toString(), doctorantEmail, directeurEmail, adminEmail);
                        handled = true;
                    }
                    default -> {
                    }
                }
            }

            // 3. Fallback générique (pour debug ou événements non mappés)
            if (!handled) {
                String subject = "Notification evenement doctoral - " + eventType;
                StringBuilder body = new StringBuilder();
                body.append("Type d'evenement : ").append(eventType).append("\n")
                        .append("Cle Kafka : ").append(record.key()).append("\n")
                        .append("Topic : ").append(record.topic()).append("\n");
                if (!doctorantId.isEmpty()) {
                    body.append("DoctorantId : ").append(doctorantId).append("\n");
                }
                if (!directeurId.isEmpty()) {
                    body.append("DirecteurId : ").append(directeurId).append("\n");
                }
                if (!status.isEmpty()) {
                    body.append("Statut : ").append(status).append("\n");
                }
                sendMail(subject, body.toString(), defaultToAddress);
            }
        } catch (Exception e) {
            log.warn("Impossible de traiter le message Kafka pour notification email: {}", e.getMessage());
        }
    }

    private void sendMail(String subject, String body, String... to) {
        try {
            if (to == null || to.length == 0) {
                to = new String[]{defaultToAddress};
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Echec de l'envoi de l'email de notification: {}", e.getMessage());
        }
    }

    private String resolveEmail(String userId, String fallback) {
        if (userId == null || userId.isBlank()) return fallback;
        try {
            Map<String, Object> wrapper = userClient.getById(Long.valueOf(userId));
            if (wrapper == null) return fallback;
            Object dataObj = wrapper.get("data");
            if (!(dataObj instanceof Map<?, ?> data)) return fallback;
            Object email = data.get("email");
            if (email == null) return fallback;
            String v = String.valueOf(email);
            return isBlank(v) ? fallback : v;
        } catch (Exception e) {
            log.warn("Impossible de recuperer l'email pour user {}: {}", userId, e.getMessage());
            return fallback;
        }
    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    private String capitalize(String v) {
        if (v == null || v.isEmpty()) return v;
        return v.substring(0, 1).toUpperCase() + v.substring(1);
    }
}
