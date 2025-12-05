package com.exemple.inscriptionservice.services;

import com.exemple.inscriptionservice.clients.UserClient;
import com.exemple.inscriptionservice.dto.CreateInscriptionRequest;
import com.exemple.inscriptionservice.dto.InscriptionDTO;
import com.exemple.inscriptionservice.dto.UserResponse;
import com.exemple.inscriptionservice.dto.UserResponseWrapper;
import com.exemple.inscriptionservice.entities.Inscription;
import com.exemple.inscriptionservice.enums.InscriptionStatus;
import com.exemple.inscriptionservice.enums.InscriptionType;
import com.exemple.inscriptionservice.repositories.InscriptionRepository;
import com.exemple.inscriptionservice.repositories.CampaignRepository;
import com.exemple.inscriptionservice.enums.CampaignType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InscriptionService {

    private static final Logger log = LoggerFactory.getLogger(InscriptionService.class);

    private final UserClient userClient;
    private final InscriptionRepository repo;
    private final CampaignRepository campaigns;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${storage.base-path:#{systemProperties['user.home']}/doctoral-uploads}")
    private String storageBasePath;

    @Value("${app.notifications.topic:inscription-events}")
    private String notificationsTopic;

    public InscriptionService(UserClient userClient,
                             InscriptionRepository repo,
                             CampaignRepository campaigns,
                             KafkaTemplate<String, String> kafkaTemplate,
                             ObjectMapper objectMapper) {
        this.userClient = userClient;
        this.repo = repo;
        this.campaigns = campaigns;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public InscriptionDTO createInscription(CreateInscriptionRequest request) {
        log.info("Creation d'une nouvelle inscription pour: {}", request.getDoctorantId());

        // campagne active
        requireActiveCampaign(CampaignType.INSCRIPTION);
        // validations via user-service
        UserResponse doctorant = requireUser(request.getDoctorantId(), "CANDIDAT", "Doctorant non trouve: ");
        UserResponse directeur = requireUser(request.getDirecteurId(), "DIRECTEUR_THESE", "Directeur non trouve: ");

        if (request.getAnneeAcademique() == null || request.getAnneeAcademique().isBlank())
            throw new IllegalArgumentException("Annee academique requise");

        var existing = repo.findTopByDoctorantIdAndAnneeAcademiqueAndTypeOrderByCreatedAtDesc(
                request.getDoctorantId(),
                request.getAnneeAcademique(),
                InscriptionType.INSCRIPTION
        ).orElse(null);
        if (existing != null) {
            log.info("Inscription deja existante pour cette annee, retour de l'existante id={}", existing.getId());
            return toDTO(existing);
        }

        Inscription entity = Inscription.builder()
                .type(InscriptionType.INSCRIPTION)
                .status(InscriptionStatus.BROUILLON)
                .doctorantId(request.getDoctorantId())
                .directeurId(request.getDirecteurId())
                .coDirecteurId(request.getCoDirecteurId())
                .sujetThese(request.getSujetThese())
                .laboratoire(request.getLaboratoire())
                .specialite(request.getSpecialite())
                .anneeAcademique(request.getAnneeAcademique())
                .nom(request.getNom())
                .cin(request.getCin())
                .telephone(request.getTelephone())
                .emailContact(request.getEmailContact())
                .cne(request.getCne())
                .dateNaissance(request.getDateNaissance())
                .lieuNaissance(request.getLieuNaissance())
                .nationalite(request.getNationalite())
                .adresse(request.getAdresse())
                .situationFamiliale(request.getSituationFamiliale())
                .resumeThese(request.getResumeThese())
                .build();
        entity = repo.save(entity);
        publishEvent("INSCRIPTION_CREATED", entity);
        return toDTO(entity);
    }

    public InscriptionDTO createReinscription(CreateInscriptionRequest request) {
        log.info("Creation d'une nouvelle reinscription pour: {}", request.getDoctorantId());
        // campagne active
        requireActiveCampaign(CampaignType.REINSCRIPTION);
        // validations via user-service
        requireUser(request.getDoctorantId(), "DOCTORANT", "Doctorant non trouve: ");
        requireUser(request.getDirecteurId(), "DIRECTEUR_THESE", "Directeur non trouve: ");

        if (request.getAnneeAcademique() == null || request.getAnneeAcademique().isBlank())
            throw new IllegalArgumentException("Annee academique requise");

        if (repo.existsByDoctorantIdAndAnneeAcademiqueAndType(request.getDoctorantId(), request.getAnneeAcademique(), InscriptionType.REINSCRIPTION)) {
            throw new IllegalStateException("Reinscription deja existante pour cette annee");
        }
        // règle: maximum 3 réinscriptions validées sans dérogation
        long reinscriptionsValidees = repo.countByDoctorantIdAndTypeAndStatus(request.getDoctorantId(), InscriptionType.REINSCRIPTION, InscriptionStatus.VALIDE);
        if (reinscriptionsValidees >= 3) {
            throw new IllegalStateException("Limite de 3 ans de réinscription atteinte (sans dérogation)");
        }
        // règle complémentaire CdC: alerte/limite durée totale du doctorat (> 6 ans depuis première inscription)
        long dureeSimulee = 1L + reinscriptionsValidees; // 1 année pour l'inscription initiale + réinscriptions validées
        if (dureeSimulee >= 6) {
            log.warn("Alerte duree doctorat >=6 ans (simule) pour doctorant {}", request.getDoctorantId());
        }

        Inscription entity = Inscription.builder()
                .type(InscriptionType.REINSCRIPTION)
                .status(InscriptionStatus.BROUILLON)
                .doctorantId(request.getDoctorantId())
                .directeurId(request.getDirecteurId())
                .coDirecteurId(request.getCoDirecteurId())
                .sujetThese(request.getSujetThese())
                .laboratoire(request.getLaboratoire())
                .specialite(request.getSpecialite())
                .anneeAcademique(request.getAnneeAcademique())
                .nom(request.getNom())
                .cin(request.getCin())
                .telephone(request.getTelephone())
                .emailContact(request.getEmailContact())
                .cne(request.getCne())
                .dateNaissance(request.getDateNaissance())
                .lieuNaissance(request.getLieuNaissance())
                .nationalite(request.getNationalite())
                .adresse(request.getAdresse())
                .situationFamiliale(request.getSituationFamiliale())
                .resumeThese(request.getResumeThese())
                .build();
        entity = repo.save(entity);
        return toDTO(entity);
    }

    public InscriptionDTO updateInscription(String id, CreateInscriptionRequest request) {
        var i = repo.findById(id).orElseThrow();
        if (i.getStatus() != InscriptionStatus.BROUILLON) {
            throw new IllegalStateException("Seules les inscriptions en brouillon peuvent etre modifiees");
        }

        i.setDirecteurId(request.getDirecteurId());
        i.setCoDirecteurId(request.getCoDirecteurId());
        i.setSujetThese(request.getSujetThese());
        i.setLaboratoire(request.getLaboratoire());
        i.setSpecialite(request.getSpecialite());
        i.setAnneeAcademique(request.getAnneeAcademique());
        i.setNom(request.getNom());
        i.setCin(request.getCin());
        i.setTelephone(request.getTelephone());
        i.setEmailContact(request.getEmailContact());
        i.setCne(request.getCne());
        i.setDateNaissance(request.getDateNaissance());
        i.setLieuNaissance(request.getLieuNaissance());
        i.setNationalite(request.getNationalite());
        i.setAdresse(request.getAdresse());
        i.setSituationFamiliale(request.getSituationFamiliale());
        i.setResumeThese(request.getResumeThese());

        i = repo.save(i);
        return toDTO(i);
    }

    public void deleteInscription(String id) {
        var i = repo.findById(id).orElseThrow();
        if (i.getStatus() != InscriptionStatus.BROUILLON) {
            throw new IllegalStateException("Seules les inscriptions en brouillon peuvent etre supprimees");
        }
        repo.delete(i);
        publishEvent("INSCRIPTION_DELETED", i);
    }

    public InscriptionDTO getById(String id) {
        return repo.findById(id).map(this::toDTO).orElse(null);
    }

    public List<InscriptionDTO> listByDoctorant(String doctorantId) {
        return repo.findAllByDoctorantIdOrderByCreatedAtDesc(doctorantId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<InscriptionDTO> listByDirecteur(String directeurId) {
        return repo.findAllByDirecteurIdOrderByCreatedAtDesc(directeurId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<InscriptionDTO> listPendingForAdmin() {
        return repo.findAllByStatusOrderByCreatedAtDesc(InscriptionStatus.EN_ATTENTE_ADMIN)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public InscriptionDTO prefillFromLastYear(String doctorantId, String nouvelleAnnee) {
        var last = repo.findTopByDoctorantIdOrderByCreatedAtDesc(doctorantId)
                .orElse(null);
        if (last == null) throw new IllegalStateException("Aucune inscription precedente");
        Inscription copy = Inscription.builder()
                .type(InscriptionType.REINSCRIPTION)
                .status(InscriptionStatus.BROUILLON)
                .doctorantId(last.getDoctorantId())
                .directeurId(last.getDirecteurId())
                .coDirecteurId(last.getCoDirecteurId())
                .sujetThese(last.getSujetThese())
                .laboratoire(last.getLaboratoire())
                .specialite(last.getSpecialite())
                .anneeAcademique(nouvelleAnnee)
                .build();
        copy = repo.save(copy);
        return toDTO(copy);
    }

    public InscriptionDTO submitToDirector(String id) {
        var i = repo.findById(id).orElseThrow();
        i.setStatus(InscriptionStatus.EN_ATTENTE_DIRECTEUR);
        i = repo.save(i);
        publishEvent("INSCRIPTION_SUBMITTED", i);
        return toDTO(i);
    }

    public InscriptionDTO approveByDirector(String id) {
        var i = repo.findById(id).orElseThrow();
        i.setStatus(InscriptionStatus.EN_ATTENTE_ADMIN);
        i = repo.save(i);
        publishEvent("INSCRIPTION_DIRECTOR_APPROVED", i);
        return toDTO(i);
    }

    public InscriptionDTO approveByAdmin(String id) {
        var i = repo.findById(id).orElseThrow();
        // contrôle des pièces jointes (placeholder, à brancher sur module documents)
        if (!hasAllRequiredDocuments(i.getId())) {
            throw new IllegalStateException("Documents requis manquants ou non conformes");
        }
        // Validation finale administrative
        i.setStatus(InscriptionStatus.VALIDE);
        i = repo.save(i);
        publishEvent("INSCRIPTION_ADMIN_APPROVED", i);
        return toDTO(i);
    }

    public InscriptionDTO reject(String id, String motif) {
        var i = repo.findById(id).orElseThrow();
        i.setStatus(InscriptionStatus.REJETE);
        i.setMotifRejet(motif);
        i = repo.save(i);
        publishEvent("INSCRIPTION_REJECTED", i);
        return toDTO(i);
    }

    public InscriptionDTO saveAttachments(String id,
                                          MultipartFile diplome,
                                          MultipartFile cv,
                                          MultipartFile lettreMotivation,
                                          MultipartFile cin) {
        var i = repo.findById(id).orElseThrow();
        Path dir = getInscriptionDir(id);
        try {
            Files.createDirectories(dir);
            saveOneAttachment(dir, diplome, "diplome");
            saveOneAttachment(dir, cv, "cv");
            saveOneAttachment(dir, lettreMotivation, "lettre_motivation");
            saveOneAttachment(dir, cin, "cin");
        } catch (IOException e) {
            log.error("Echec du televersement des pieces pour {}: {}", id, e.getMessage());
            throw new RuntimeException("Echec du televersement des documents", e);
        }
        publishEvent("INSCRIPTION_DOCUMENTS_UPLOADED", i);
        return toDTO(i);
    }

    private void saveOneAttachment(Path dir, MultipartFile file, String prefix) throws IOException {
        if (file == null || file.isEmpty()) return;
        String original = file.getOriginalFilename();
        String safeName = original == null ? (prefix + "_" + System.currentTimeMillis()) : original.replaceAll("[^a-zA-Z0-9._-]", "_");
        int maxLen = 100;
        if (safeName.length() > maxLen) {
            int dot = safeName.lastIndexOf('.') > 0 ? safeName.lastIndexOf('.') : safeName.length();
            String base = safeName.substring(0, Math.min(dot, maxLen - 10));
            String ext = dot < safeName.length() ? safeName.substring(dot) : "";
            safeName = base + ext;
        }
        Path target = dir.resolve(prefix + "_" + System.currentTimeMillis() + "_" + safeName);
        Files.createDirectories(target.getParent());
        file.transferTo(target.toFile());
    }

    private void validateUser(UserResponse user, String requiredRole) {
        if (user == null) throw new RuntimeException("Utilisateur introuvable");
        // TODO: verifications de roles/statuts selon le modele reel
    }

    private UserResponse requireUser(String id, String role, String errorPrefix) {
        try {
            UserResponseWrapper wrapper = userClient.getUserById(id);
            UserResponse u = wrapper.getData();
            validateUser(u, role);
            return u;
        } catch (Exception e) {
            log.error("Erreur lors de la recuperation utilisateur {}: {}", id, e.getMessage());
            throw new RuntimeException(errorPrefix + id);
        }
    }

    private InscriptionDTO toDTO(Inscription i) {
        return InscriptionDTO.builder()
                .id(i.getId())
                .type(i.getType())
                .status(i.getStatus())
                .doctorantId(i.getDoctorantId())
                .directeurId(i.getDirecteurId())
                .coDirecteurId(i.getCoDirecteurId())
                .sujetThese(i.getSujetThese())
                .laboratoire(i.getLaboratoire())
                .specialite(i.getSpecialite())
                .anneeAcademique(i.getAnneeAcademique())
                .nom(i.getNom())
                .cin(i.getCin())
                .telephone(i.getTelephone())
                .emailContact(i.getEmailContact())
                .cne(i.getCne())
                .dateNaissance(i.getDateNaissance())
                .lieuNaissance(i.getLieuNaissance())
                .nationalite(i.getNationalite())
                .adresse(i.getAdresse())
                .situationFamiliale(i.getSituationFamiliale())
                .resumeThese(i.getResumeThese())
                .motifRejet(i.getMotifRejet())
                .dateDemande(i.getDateDemande())
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }

    private void requireActiveCampaign(CampaignType type) {
        var active = campaigns.findFirstByTypeAndActiveIsTrue(type).orElse(null);
        if (active == null) {
            throw new IllegalStateException("Aucune campagne active pour " + type);
        }
    }

    private boolean hasAllRequiredDocuments(String inscriptionId) {
        try {
            Path dir = getInscriptionDir(inscriptionId);
            if (!Files.exists(dir)) return false;
            try (var s = Files.list(dir)) {
                return s.findAny().isPresent();
            }
        } catch (IOException e) {
            log.warn("Controle pieces: erreur d'acces au repertoire pour {}: {}", inscriptionId, e.getMessage());
            return false;
        }
    }

    private void publishEvent(String type, Inscription i) {
        if (i == null) return;
        try {
            Map<String, Object> payload = Map.of(
                    "eventType", type,
                    "inscriptionId", i.getId(),
                    "doctorantId", i.getDoctorantId(),
                    "directeurId", i.getDirecteurId(),
                    "status", String.valueOf(i.getStatus()),
                    "timestamp", System.currentTimeMillis()
            );
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(notificationsTopic, i.getId(), json);
            if (log.isDebugEnabled()) {
                log.debug("Evenement Kafka publie: topic={}, key={}, type={}", notificationsTopic, i.getId(), type);
            }
        } catch (Exception e) {
            log.warn("Echec envoi evenement Kafka {} pour inscription {}: {}", type, i.getId(), e.getMessage());
        }
    }

    private Path getInscriptionDir(String id) {
        Path base = Paths.get(storageBasePath);
        return base.resolve(Paths.get("inscriptions", id));
    }
}
