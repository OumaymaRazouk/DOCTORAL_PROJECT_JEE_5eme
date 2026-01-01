package com.exemple.soutenanceservice.service;

import com.exemple.soutenanceservice.model.*;
import com.exemple.soutenanceservice.repository.JuryMemberRepository;
import com.exemple.soutenanceservice.repository.SoutenanceRepository;
import com.exemple.soutenanceservice.repository.PrerequisSoutenanceConfigRepository;
import com.exemple.soutenanceservice.repository.PrerequisDossierRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
@RequiredArgsConstructor
public class SoutenanceDomainService {

    private static final Logger log = LoggerFactory.getLogger(SoutenanceDomainService.class);

    private final SoutenanceRepository soutenanceRepository;
    private final JuryMemberRepository juryMemberRepository;
    private final PrerequisSoutenanceConfigRepository prerequisConfigRepository;
    private final PrerequisDossierRepository prerequisDossierRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${soutenance.prereq.min-publications:2}")
    private int minPublications;

    @Value("${soutenance.prereq.min-communications:2}")
    private int minCommunications;

    @Value("${soutenance.prereq.min-heures-formation:200}")
    private int minHeuresFormation;

    @Value("${soutenance.prereq.max-duree-annees:6}")
    private int maxDureeAnnees;

    @Value("${app.notifications.topic:inscription-events}")
    private String notificationsTopic;

    private final Sinks.Many<Soutenance> soutenanceSink =
            Sinks.many().multicast().onBackpressureBuffer();

    public List<Soutenance> listForDoctorant(String doctorantId) {
        return soutenanceRepository.findAllByDoctorantIdOrderByCreatedAtDesc(doctorantId);
    }

    public List<Soutenance> listForDirecteur(String directeurId) {
        return soutenanceRepository.findAllByDirecteurIdOrderByCreatedAtDesc(directeurId);
    }

    public List<Soutenance> listForAdmin(SoutenanceStatus status) {
        if (status != null) {
            return soutenanceRepository.findAllByStatusOrderByCreatedAtDesc(status);
        }
        return soutenanceRepository.findAll();
    }

    public PrerequisSoutenanceConfig getPrerequisConfig(String doctorantId) {
        if (doctorantId == null || doctorantId.isBlank()) {
            return null;
        }
        return prerequisConfigRepository.findByDoctorantId(doctorantId)
                .orElseGet(() -> PrerequisSoutenanceConfig.builder()
                        .doctorantId(doctorantId)
                        .minPublications(minPublications)
                        .minCommunications(minCommunications)
                        .minHeuresFormation(minHeuresFormation)
                        .maxDureeAnnees(maxDureeAnnees)
                        .requireManuscrit(true)
                        .requireAntiPlagiat(true)
                        .requireAttestations(true)
                        .requireDemandeManuscrite(true)
                        .build());
    }

    public PrerequisDossier getPrerequisDossier(String doctorantId) {
        if (doctorantId == null || doctorantId.isBlank()) {
            return null;
        }
        return prerequisDossierRepository.findById(doctorantId).orElse(null);
    }

    @Transactional
    public PrerequisDossier savePrerequisDossier(PrerequisDossierInput input) {
        if (input == null || input.getDoctorantId() == null || input.getDoctorantId().isBlank()) {
            throw new IllegalArgumentException("doctorantId is required");
        }
        PrerequisDossier existing = prerequisDossierRepository.findById(input.getDoctorantId())
                .orElseGet(() -> PrerequisDossier.builder()
                        .doctorantId(input.getDoctorantId())
                        .build());

        existing.setPublicationsCount(input.getPublicationsCount());
        existing.setCommunicationsCount(input.getCommunicationsCount());
        existing.setHeuresFormationCumulees(input.getHeuresFormationCumulees());
        existing.setDureeDoctoratAnnees(input.getDureeDoctoratAnnees());

        existing.setEtatScientifique(PrerequisDossierEtat.EN_ATTENTE);
        existing.setEtatAdministratif(PrerequisDossierEtat.EN_ATTENTE);
        existing.setCommentaireScientifique(null);
        existing.setCommentaireAdministratif(null);
        existing.setDateValidationScientifique(null);
        existing.setDateValidationAdministratif(null);

        return prerequisDossierRepository.save(existing);
    }

    @Transactional
    public PrerequisDossier directeurValidePrerequisDossier(String doctorantId, boolean valide, String commentaire) {
        if (doctorantId == null || doctorantId.isBlank()) {
            throw new IllegalArgumentException("doctorantId is required");
        }
        PrerequisDossier dossier = prerequisDossierRepository.findById(doctorantId)
                .orElseThrow(() -> new IllegalStateException("Dossier de prérequis introuvable pour le doctorant"));

        dossier.setEtatScientifique(valide ? PrerequisDossierEtat.VALIDE : PrerequisDossierEtat.REFUSE);
        dossier.setCommentaireScientifique(commentaire);
        dossier.setDateValidationScientifique(LocalDateTime.now());

        return prerequisDossierRepository.save(dossier);
    }

    @Transactional
    public PrerequisDossier adminValidePrerequisDossier(String doctorantId, boolean valide, String commentaire) {
        if (doctorantId == null || doctorantId.isBlank()) {
            throw new IllegalArgumentException("doctorantId is required");
        }
        PrerequisDossier dossier = prerequisDossierRepository.findById(doctorantId)
                .orElseThrow(() -> new IllegalStateException("Dossier de prérequis introuvable pour le doctorant"));

        if (valide) {
            String errors = checkQuantitativePrerequis(dossier);
            if (!errors.isEmpty()) {
                throw new IllegalStateException("Prérequis quantitatifs non satisfaits : " + errors);
            }
        }

        dossier.setEtatAdministratif(valide ? PrerequisDossierEtat.VALIDE : PrerequisDossierEtat.REFUSE);
        dossier.setCommentaireAdministratif(commentaire);
        dossier.setDateValidationAdministratif(LocalDateTime.now());

        return prerequisDossierRepository.save(dossier);
    }

    @Transactional
    public PrerequisSoutenanceConfig savePrerequisConfig(PrerequisConfigInput input) {
        if (input == null || input.getDoctorantId() == null || input.getDoctorantId().isBlank()) {
            throw new IllegalArgumentException("doctorantId is required");
        }
        PrerequisSoutenanceConfig existing = prerequisConfigRepository
                .findByDoctorantId(input.getDoctorantId())
                .orElseGet(() -> PrerequisSoutenanceConfig.builder()
                        .doctorantId(input.getDoctorantId())
                        .build());

        existing.setMinPublications(input.getMinPublications());
        existing.setMinCommunications(input.getMinCommunications());
        existing.setMinHeuresFormation(input.getMinHeuresFormation());
        existing.setMaxDureeAnnees(input.getMaxDureeAnnees());
        existing.setRequireManuscrit(input.isRequireManuscrit());
        existing.setRequireAntiPlagiat(input.isRequireAntiPlagiat());
        existing.setRequireAttestations(input.isRequireAttestations());
        existing.setRequireDemandeManuscrite(input.isRequireDemandeManuscrite());

        return prerequisConfigRepository.save(existing);
    }

    public Soutenance getById(String id) {
        return soutenanceRepository.findById(id).orElse(null);
    }

    @Transactional
    public Soutenance saveDraft(SoutenanceDraftInput input) {
        Soutenance existing = soutenanceRepository.findTopByDoctorantIdOrderByCreatedAtDesc(input.getDoctorantId())
                .orElse(null);
        if (existing != null && existing.getStatus() == SoutenanceStatus.BROUILLON) {
            // Mise à jour du brouillon existant
            applyDraftFields(existing, input);
            return saveAndEmit(existing);
        }
        Soutenance s = Soutenance.builder()
                .doctorantId(input.getDoctorantId())
                .directeurId(input.getDirecteurId())
                .status(SoutenanceStatus.BROUILLON)
                .build();
        applyDraftFields(s, input);
        return saveAndEmit(s);
    }

    private void applyDraftFields(Soutenance s, SoutenanceDraftInput input) {
        s.setTitreTheseDefinitif(input.getTitreTheseDefinitif());
        s.setResumeFinal(input.getResumeFinal());
        if (input.getMotsCles() != null) {
            s.setMotsCles(String.join(",", input.getMotsCles()));
        }
        s.setManuscritPath(input.getManuscritPath());
        s.setRapportThesePath(input.getRapportThesePath());
        s.setRapportAntiPlagiatPath(input.getRapportAntiPlagiatPath());
        s.setPreuvesPublicationsPath(input.getPreuvesPublicationsPath());
        s.setPreuvesCommunicationsPath(input.getPreuvesCommunicationsPath());
        s.setAttestationsFormationsPath(input.getAttestationsFormationsPath());
        s.setDemandeManuscritePath(input.getDemandeManuscritePath());
    }

    @Transactional
    public Soutenance submit(String id) {
        Soutenance s = soutenanceRepository.findById(id).orElseThrow();
        if (s.getStatus() != SoutenanceStatus.BROUILLON) {
            throw new IllegalStateException("Seuls les brouillons peuvent être soumis");
        }
        String errors = checkPrerequis(s);
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Prérequis non satisfaits : " + errors);
        }
        s.setStatus(SoutenanceStatus.SOUMIS);
        s.setDateSoumission(LocalDateTime.now());
        return saveAndEmit(s);
    }

    @Transactional
    public boolean deleteDraft(String id) {
        Soutenance s = soutenanceRepository.findById(id).orElseThrow();
        if (s.getStatus() != SoutenanceStatus.BROUILLON) {
            throw new IllegalStateException("Seuls les brouillons peuvent être supprimés");
        }
        soutenanceRepository.delete(s);
        return true;
    }

    private String checkPrerequis(Soutenance s) {
        StringBuilder sb = new StringBuilder();

        PrerequisSoutenanceConfig cfg = null;
        if (s.getDoctorantId() != null) {
            cfg = prerequisConfigRepository.findByDoctorantId(s.getDoctorantId()).orElse(null);
        }

        boolean requireManuscrit = cfg != null ? cfg.isRequireManuscrit() : true;
        boolean requireAntiPlagiat = cfg != null ? cfg.isRequireAntiPlagiat() : true;
        boolean requireAttestations = cfg != null ? cfg.isRequireAttestations() : true;
        boolean requireDemande = cfg != null ? cfg.isRequireDemandeManuscrite() : true;

        // Contrôle du dossier de prérequis (quantitatif + états de validation)
        if (s.getDoctorantId() == null) {
            sb.append("dossier de prérequis non associé à un doctorant; ");
        } else {
            PrerequisDossier dossier = prerequisDossierRepository.findById(s.getDoctorantId()).orElse(null);
            if (dossier == null) {
                sb.append("dossier de prérequis non déclaré; ");
            } else {
                if (dossier.getEtatScientifique() != PrerequisDossierEtat.VALIDE) {
                    sb.append("validation scientifique des prérequis non effectuée ou refusée; ");
                }
                if (dossier.getEtatAdministratif() != PrerequisDossierEtat.VALIDE) {
                    sb.append("validation administrative des prérequis non effectuée ou refusée; ");
                }

                String quantErrors = checkQuantitativePrerequis(dossier);
                if (!quantErrors.isEmpty()) {
                    sb.append(quantErrors);
                }
            }
        }
        if (requireManuscrit && isBlank(s.getManuscritPath())) {
            sb.append("manuscrit final manquant; ");
        }
        if (requireAntiPlagiat && isBlank(s.getRapportAntiPlagiatPath())) {
            sb.append("rapport anti-plagiat manquant; ");
        }
        if (requireAttestations && isBlank(s.getAttestationsFormationsPath())) {
            sb.append("attestations de formations manquantes; ");
        }
        if (requireDemande && isBlank(s.getDemandeManuscritePath())) {
            sb.append("demande manuscrite manquante; ");
        }
        return sb.toString();
    }

    private String checkQuantitativePrerequis(PrerequisDossier dossier) {
        StringBuilder sb = new StringBuilder();

        PrerequisSoutenanceConfig cfg = null;
        if (dossier.getDoctorantId() != null) {
            cfg = prerequisConfigRepository.findByDoctorantId(dossier.getDoctorantId()).orElse(null);
        }

        int effectiveMinPublications = cfg != null && cfg.getMinPublications() > 0
                ? cfg.getMinPublications() : minPublications;
        int effectiveMinCommunications = cfg != null && cfg.getMinCommunications() > 0
                ? cfg.getMinCommunications() : minCommunications;
        int effectiveMinHeures = cfg != null && cfg.getMinHeuresFormation() > 0
                ? cfg.getMinHeuresFormation() : minHeuresFormation;
        int effectiveMaxDuree = cfg != null && cfg.getMaxDureeAnnees() > 0
                ? cfg.getMaxDureeAnnees() : maxDureeAnnees;

        if (dossier.getPublicationsCount() < effectiveMinPublications) {
            sb.append("au moins ")
                    .append(effectiveMinPublications)
                    .append(" publication(s) Q1/Q2; ");
        }
        if (dossier.getCommunicationsCount() < effectiveMinCommunications) {
            sb.append("au moins ")
                    .append(effectiveMinCommunications)
                    .append(" communication(s); ");
        }
        if (dossier.getHeuresFormationCumulees() < effectiveMinHeures) {
            sb.append(effectiveMinHeures)
                    .append("h de formation cumulées; ");
        }
        if (dossier.getDureeDoctoratAnnees() > effectiveMaxDuree) {
            sb.append("durée du doctorat > ")
                    .append(effectiveMaxDuree)
                    .append(" ans; ");
        }

        return sb.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Transactional
    public Soutenance adminValidePrerequis(String id, String commentaire) {
        Soutenance s = soutenanceRepository.findById(id).orElseThrow();
        if (s.getStatus() != SoutenanceStatus.SOUMIS && s.getStatus() != SoutenanceStatus.PREREQUIS_EN_VERIFICATION) {
            throw new IllegalStateException("Prérequis valides uniquement pour les dossiers soumis");
        }
        s.setStatus(SoutenanceStatus.PREREQUIS_VALIDES);
        s.setCommentairePrerequis(commentaire);
        return saveAndEmit(s);
    }

    @Transactional
    public Soutenance adminVerifiePrerequis(String id) {
        Soutenance s = soutenanceRepository.findById(id).orElseThrow();
        if (s.getStatus() != SoutenanceStatus.SOUMIS) {
            throw new IllegalStateException("Vérification des prérequis uniquement après soumission");
        }
        s.setStatus(SoutenanceStatus.PREREQUIS_EN_VERIFICATION);
        return saveAndEmit(s);
    }

    @Transactional
    public Soutenance adminRejette(String id, String motif) {
        Soutenance s = soutenanceRepository.findById(id).orElseThrow();
        s.setStatus(SoutenanceStatus.REJETE);
        s.setMotifRejet(motif);
        return saveAndEmit(s);
    }

    @Transactional
    public Soutenance directeurProposeJury(String id, List<JuryMemberInput> membres) {
        Soutenance s = soutenanceRepository.findById(id).orElseThrow();
        if (s.getStatus() != SoutenanceStatus.PREREQUIS_VALIDES
                && s.getStatus() != SoutenanceStatus.JURY_PROPOSE
                && s.getStatus() != SoutenanceStatus.JURY_VALIDE) {
            throw new IllegalStateException("Le jury ne peut être proposé ou modifié que après validation des prérequis");
        }
        // supprimer l'ancien jury
        juryMemberRepository.deleteAllBySoutenance_Id(id);

        List<JuryMember> saved = membres.stream()
                .map(m -> JuryMember.builder()
                        .soutenance(s)
                        .nom(m.getNom())
                        .prenom(m.getPrenom())
                        .affiliation(m.getAffiliation())
                        .email(m.getEmail())
                        .telephone(m.getTelephone())
                        .role(m.getRole())
                        .build())
                .map(juryMemberRepository::save)
                .collect(Collectors.toList());

        long rapp = saved.stream().filter(j -> j.getRole() == JuryRole.RAPPORTEUR).count();
        long pres = saved.stream().filter(j -> j.getRole() == JuryRole.PRESIDENT).count();
        if (rapp < 2 || pres < 1) {
            throw new IllegalStateException("Le jury doit contenir au moins 2 rapporteurs et 1 président");
        }
        s.setStatus(SoutenanceStatus.JURY_PROPOSE);
        return saveAndEmit(s);
    }

    @Transactional
    public Soutenance adminValideJury(String id, String commentaire) {
        Soutenance s = soutenanceRepository.findById(id).orElseThrow();
        if (s.getStatus() != SoutenanceStatus.JURY_PROPOSE) {
            throw new IllegalStateException("Le jury doit être proposé avant validation");
        }
        s.setStatus(SoutenanceStatus.JURY_VALIDE);
        s.setCommentaireJury(commentaire);
        return saveAndEmit(s);
    }

    @Transactional
    public Soutenance adminAutoriseSoutenance(String id) {
        Soutenance s = soutenanceRepository.findById(id).orElseThrow();
        if (s.getStatus() != SoutenanceStatus.JURY_VALIDE) {
            throw new IllegalStateException("La soutenance ne peut être autorisée qu'après validation du jury");
        }
        s.setStatus(SoutenanceStatus.SOUTENANCE_AUTORISEE);
        s.setDateAutorisation(LocalDateTime.now());
        return saveAndEmit(s);
    }

    @Transactional
    public Soutenance directeurProposePlanification(String id, PlanificationInput input) {
        Soutenance s = soutenanceRepository.findById(id).orElseThrow();
        if (s.getStatus() != SoutenanceStatus.SOUTENANCE_AUTORISEE
                && s.getStatus() != SoutenanceStatus.PLANIFICATION_PROPOSEE) {
            throw new IllegalStateException("La soutenance doit être autorisée avant que le directeur ne propose une planification");
        }
        s.setDatePlanifiee(input.getDateHeure());
        s.setSalle(input.getSalle());
        s.setLieu(input.getLieu());
        s.setLienVisioconference(input.getLienVisioconference());
        s.setStatus(SoutenanceStatus.PLANIFICATION_PROPOSEE);
        return saveAndEmit(s);
    }

    @Transactional
    public Soutenance adminPlanifieSoutenance(String id, PlanificationInput input) {
        Soutenance s = soutenanceRepository.findById(id).orElseThrow();
        if (s.getStatus() != SoutenanceStatus.SOUTENANCE_AUTORISEE
                && s.getStatus() != SoutenanceStatus.PLANIFICATION_PROPOSEE) {
            throw new IllegalStateException("La soutenance doit être autorisée ou une planification proposée avant validation de la planification");
        }
        if (input.getDateHeure() != null && input.getSalle() != null && !input.getSalle().isBlank()) {
            List<Soutenance> conflits = soutenanceRepository.findAllBySalleAndDatePlanifiee(input.getSalle(), input.getDateHeure());
            boolean hasOther = conflits.stream().anyMatch(other -> other.getId() != null && !other.getId().equals(id));
            if (hasOther) {
                throw new IllegalStateException("Conflit de planification : une autre soutenance est déjà planifiée dans cette salle à cette date/heure");
            }
        }
        s.setDatePlanifiee(input.getDateHeure());
        s.setSalle(input.getSalle());
        s.setLieu(input.getLieu());
        s.setLienVisioconference(input.getLienVisioconference());
        s.setStatus(SoutenanceStatus.SOUTENANCE_PLANIFIEE);
        return saveAndEmit(s);
    }

    @Transactional
    public Soutenance adminMarqueEffectuee(String id, LocalDateTime dateEffective) {
        Soutenance s = soutenanceRepository.findById(id).orElseThrow();
        if (s.getStatus() != SoutenanceStatus.SOUTENANCE_PLANIFIEE) {
            throw new IllegalStateException("La soutenance doit être planifiée avant d'être marquée comme effectuée");
        }
        if (dateEffective != null) {
            s.setDatePlanifiee(dateEffective);
        }
        s.setStatus(SoutenanceStatus.SOUTENANCE_EFFECTUEE);
        return saveAndEmit(s);
    }

    private Soutenance saveAndEmit(Soutenance s) {
        Soutenance saved = soutenanceRepository.save(s);
        soutenanceSink.tryEmitNext(saved);
        publishEvent(saved);
        return saved;
    }

    private void publishEvent(Soutenance s) {
        if (s == null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("eventType", "SOUTENANCE_UPDATED");
                payload.put("soutenanceId", s.getId());
                payload.put("doctorantId", s.getDoctorantId());
                payload.put("directeurId", s.getDirecteurId());
                if (s.getStatus() != null) {
                    payload.put("status", s.getStatus().name());
                }
                if (s.getDatePlanifiee() != null) {
                    payload.put("datePlanifiee", s.getDatePlanifiee().toString());
                }
                if (s.getSalle() != null) {
                    payload.put("salle", s.getSalle());
                }
                if (s.getLieu() != null) {
                    payload.put("lieu", s.getLieu());
                }
                payload.put("timestamp", System.currentTimeMillis());
                String json = objectMapper.writeValueAsString(payload);
                kafkaTemplate.send(notificationsTopic, s.getId(), json);
                if (log.isDebugEnabled()) {
                    log.debug("Evenement Kafka publie: topic={}, key={}, type={}", notificationsTopic, s.getId(), "SOUTENANCE_UPDATED");
                }
            } catch (Exception e) {
                log.warn("Echec envoi evenement Kafka pour soutenance {}: {}", s.getId(), e.getMessage());
            }
        });
    }

    public Flux<Soutenance> soutenanceUpdates(String id) {
        return soutenanceSink.asFlux()
                .filter(s -> s.getId() != null && s.getId().equals(id));
    }
}
