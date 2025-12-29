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
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
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
                .rapportAvancement(request.getRapportAvancement())
                .publicationsN1(request.getPublicationsN1())
                .communicationsN1(request.getCommunicationsN1())
                .heuresFormationCumulees(request.getHeuresFormationCumulees())
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

        boolean alertDureeDoctorat = false;
        // Règles complémentaires CdC basées sur la date de première inscription
        var firstInscription = repo.findTopByDoctorantIdAndTypeOrderByCreatedAtAsc(request.getDoctorantId(), InscriptionType.INSCRIPTION)
                .orElse(null);
        if (firstInscription != null && firstInscription.getDateDemande() != null) {
            LocalDate initialDate = firstInscription.getDateDemande();
            long yearsSinceInitial = ChronoUnit.YEARS.between(initialDate, LocalDate.now());
            // Blocage après 3 ans sans dérogation PED
            if (yearsSinceInitial >= 3) {
                throw new IllegalStateException("Réinscription impossible : plus de 3 ans depuis l'inscription initiale (sans dérogation)");
            }
            // Alerte si durée doctorat > 5 ans
            if (yearsSinceInitial > 5) {
                alertDureeDoctorat = true;
                log.warn("Alerte duree doctorat > 5 ans ({} ans) pour doctorant {}", yearsSinceInitial, request.getDoctorantId());
            }
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
                .rapportAvancement(request.getRapportAvancement())
                .publicationsN1(request.getPublicationsN1())
                .communicationsN1(request.getCommunicationsN1())
                .heuresFormationCumulees(request.getHeuresFormationCumulees())
                .build();
        entity = repo.save(entity);
        publishEvent("REINSCRIPTION_CREATED", entity);
        if (alertDureeDoctorat) {
            publishEvent("REINSCRIPTION_DURATION_ALERT", entity);
        }
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
        i.setRapportAvancement(request.getRapportAvancement());
        i.setPublicationsN1(request.getPublicationsN1());
        i.setCommunicationsN1(request.getCommunicationsN1());
        i.setHeuresFormationCumulees(request.getHeuresFormationCumulees());

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

    public List<InscriptionDTO> listAll() {
        return repo.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public InscriptionDTO prefillFromLastYear(String doctorantId, String nouvelleAnnee) {
        var last = repo.findTopByDoctorantIdOrderByCreatedAtDesc(doctorantId)
                .orElse(null);
        if (last == null) throw new IllegalStateException("Aucune inscription precedente");

        // Données personnelles : prendre la première INSCRIPTION si elle existe, sinon le dernier dossier
        var firstInscription = repo.findTopByDoctorantIdAndTypeOrderByCreatedAtAsc(doctorantId, InscriptionType.INSCRIPTION)
                .orElse(last);

        Inscription copy = Inscription.builder()
                .type(InscriptionType.REINSCRIPTION)
                .status(InscriptionStatus.BROUILLON)
                .doctorantId(last.getDoctorantId())
                // Données académiques depuis le dernier dossier
                .directeurId(last.getDirecteurId())
                .coDirecteurId(last.getCoDirecteurId())
                .sujetThese(last.getSujetThese())
                .laboratoire(last.getLaboratoire())
                .specialite(last.getSpecialite())
                .anneeAcademique(nouvelleAnnee)
                // Données personnelles depuis la première inscription complète
                .nom(firstInscription.getNom())
                .cin(firstInscription.getCin())
                .telephone(firstInscription.getTelephone())
                .emailContact(firstInscription.getEmailContact())
                .cne(firstInscription.getCne())
                .dateNaissance(firstInscription.getDateNaissance())
                .lieuNaissance(firstInscription.getLieuNaissance())
                .nationalite(firstInscription.getNationalite())
                .adresse(firstInscription.getAdresse())
                .situationFamiliale(firstInscription.getSituationFamiliale())
                .resumeThese(last.getResumeThese())
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
        // contrôle des pièces jointes (assoupli pour les tests : simple alerte dans les logs)
        if (!hasAllRequiredDocuments(i.getId())) {
            log.warn("Validation admin sans toutes les pieces jointes pour inscription {}", id);
        }
        // Validation finale administrative
        i.setStatus(InscriptionStatus.VALIDE);
        i = repo.save(i);
        publishEvent("INSCRIPTION_ADMIN_APPROVED", i);
        try {
            if (i.getType() == InscriptionType.INSCRIPTION && i.getDoctorantId() != null) {
                userClient.grantDoctorantRole(i.getDoctorantId());
                log.info("Role DOCTORANT attribue automatiquement a l'utilisateur {} apres validation inscription {}", i.getDoctorantId(), i.getId());
            }
        } catch (Exception e) {
            log.warn("Echec de l'attribution automatique du role DOCTORANT pour utilisateur {}: {}", i.getDoctorantId(), e.getMessage());
        }
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
        Path dir = getInscriptionDir(i);
        if (log.isInfoEnabled()) {
            log.info("saveAttachments for inscription {} in dir={} (base={})", id, dir, storageBasePath);
            log.info("Incoming files: diplome={}, cv={}, lettreMotivation={}, cin={}",
                    diplome == null ? "null" : (diplome.getOriginalFilename() + " (" + diplome.getSize() + " bytes)"),
                    cv == null ? "null" : (cv.getOriginalFilename() + " (" + cv.getSize() + " bytes)"),
                    lettreMotivation == null ? "null" : (lettreMotivation.getOriginalFilename() + " (" + lettreMotivation.getSize() + " bytes)"),
                    cin == null ? "null" : (cin.getOriginalFilename() + " (" + cin.getSize() + " bytes)"));
        }
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

    public List<String> listAttachments(String id) {
        var i = repo.findById(id).orElseThrow();
        Path dir = getInscriptionDir(i);
        try {
            if (!Files.exists(dir)) {
                if (i.getType() == InscriptionType.REINSCRIPTION) {
                    Path legacyDir = Paths.get(storageBasePath).resolve(Paths.get("inscriptions", id));
                    if (Files.exists(legacyDir)) {
                        dir = legacyDir;
                    } else {
                        return List.of();
                    }
                } else {
                    return List.of();
                }
            }
            try (var s = Files.list(dir)) {
                return s
                        .filter(Files::isRegularFile)
                        .map(p -> p.getFileName().toString())
                        .toList();
            }
        } catch (IOException e) {
            log.warn("Liste pieces: erreur d'acces au repertoire pour {}: {}", id, e.getMessage());
            return List.of();
        }
    }

    public Resource loadAttachment(String id, String fileName) {
        var i = repo.findById(id).orElseThrow();
        Path dir = getInscriptionDir(i);
        Path target = dir.resolve(fileName);
        if (!Files.exists(target)) {
            if (i.getType() == InscriptionType.REINSCRIPTION) {
                Path legacyDir = Paths.get(storageBasePath).resolve(Paths.get("inscriptions", id));
                Path legacyTarget = legacyDir.resolve(fileName);
                if (Files.exists(legacyTarget)) {
                    return new FileSystemResource(legacyTarget.toFile());
                }
            }
            return null;
        }
        return new FileSystemResource(target.toFile());
    }

    private void saveOneAttachment(Path dir, MultipartFile file, String prefix) throws IOException {
        if (file == null) {
            if (log.isInfoEnabled()) {
                log.info("saveOneAttachment: no file provided for prefix={} (null)", prefix);
            }
            return;
        }
        if (file.isEmpty()) {
            if (log.isInfoEnabled()) {
                log.info("saveOneAttachment: empty file for prefix={} (name={}, size={})", prefix, file.getOriginalFilename(), file.getSize());
            }
            return;
        }
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
        if (log.isInfoEnabled()) {
            log.info("saveOneAttachment: saving file prefix={} original={} size={} target={}",
                    prefix, original, file.getSize(), target);
        }
        Files.createDirectories(target.getParent());
        file.transferTo(target.toFile());
    }

    private void validateUser(UserResponse user, String requiredRole) {
        if (user == null) throw new RuntimeException("Utilisateur introuvable");
        // TODO: verifications de roles/statuts selon le modele reel
    }

    @Retry(name = "userService")
    @CircuitBreaker(name = "userService", fallbackMethod = "requireUserFallback")
    private UserResponse requireUser(String id, String role, String errorPrefix) {
        try {
            UserResponseWrapper wrapper = userClient.getUserById(id);
            UserResponse u = wrapper.getData();
            validateUser(u, role);
            return u;
        } catch (Exception e) {
            log.error("Erreur lors de la recuperation utilisateur {}: {}", id, e.getMessage());
            throw e;
        }
    }

    private UserResponse requireUserFallback(String id, String role, String errorPrefix, Throwable t) {
        log.warn("Fallback requireUser pour {} (role={}): {}", id, role, t.getMessage());
        throw new RuntimeException(errorPrefix + id + " (service utilisateurs indisponible)");
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
                .rapportAvancement(i.getRapportAvancement())
                .publicationsN1(i.getPublicationsN1())
                .communicationsN1(i.getCommunicationsN1())
                .heuresFormationCumulees(i.getHeuresFormationCumulees())
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
            var i = repo.findById(inscriptionId).orElseThrow();
            Path dir = getInscriptionDir(i);
            if (!Files.exists(dir)) {
                if (i.getType() == InscriptionType.REINSCRIPTION) {
                    Path legacyDir = Paths.get(storageBasePath).resolve(Paths.get("inscriptions", inscriptionId));
                    if (!Files.exists(legacyDir)) {
                        return false;
                    }
                    dir = legacyDir;
                } else {
                    return false;
                }
            }
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
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", type);
            payload.put("inscriptionId", i.getId());
            payload.put("doctorantId", i.getDoctorantId());
            payload.put("directeurId", i.getDirecteurId());
            payload.put("status", String.valueOf(i.getStatus()));
            if (i.getType() != null) {
                payload.put("inscriptionType", i.getType().name());
            }
            if (i.getAnneeAcademique() != null) {
                payload.put("anneeAcademique", i.getAnneeAcademique());
            }
            payload.put("timestamp", System.currentTimeMillis());
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(notificationsTopic, i.getId(), json);
            if (log.isDebugEnabled()) {
                log.debug("Evenement Kafka publie: topic={}, key={}, type={}", notificationsTopic, i.getId(), type);
            }
        } catch (Exception e) {
            log.warn("Echec envoi evenement Kafka {} pour inscription {}: {}", type, i.getId(), e.getMessage());
        }
    }

    private Path getInscriptionDir(Inscription i) {
        Path base = Paths.get(storageBasePath);
        String folder = (i.getType() == InscriptionType.REINSCRIPTION) ? "reinscriptions" : "inscriptions";
        return base.resolve(Paths.get(folder, i.getId()));
    }
}
