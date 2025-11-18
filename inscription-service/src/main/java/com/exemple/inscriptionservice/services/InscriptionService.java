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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InscriptionService {

    private static final Logger log = LoggerFactory.getLogger(InscriptionService.class);

    private final UserClient userClient;
    private final InscriptionRepository repo;
    private final CampaignRepository campaigns;

    public InscriptionService(UserClient userClient, InscriptionRepository repo, CampaignRepository campaigns) {
        this.userClient = userClient;
        this.repo = repo;
        this.campaigns = campaigns;
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

        if (repo.existsByDoctorantIdAndAnneeAcademiqueAndType(request.getDoctorantId(), request.getAnneeAcademique(), InscriptionType.INSCRIPTION)) {
            throw new IllegalStateException("Inscription deja existante pour cette annee");
        }

        Inscription entity = Inscription.builder()
                .type(InscriptionType.INSCRIPTION)
                .status(InscriptionStatus.EN_ATTENTE_ADMIN)
                .doctorantId(request.getDoctorantId())
                .directeurId(request.getDirecteurId())
                .coDirecteurId(request.getCoDirecteurId())
                .sujetThese(request.getSujetThese())
                .laboratoire(request.getLaboratoire())
                .specialite(request.getSpecialite())
                .anneeAcademique(request.getAnneeAcademique())
                .build();
        entity = repo.save(entity);
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

        Inscription entity = Inscription.builder()
                .type(InscriptionType.REINSCRIPTION)
                .status(InscriptionStatus.EN_ATTENTE_ADMIN)
                .doctorantId(request.getDoctorantId())
                .directeurId(request.getDirecteurId())
                .coDirecteurId(request.getCoDirecteurId())
                .sujetThese(request.getSujetThese())
                .laboratoire(request.getLaboratoire())
                .specialite(request.getSpecialite())
                .anneeAcademique(request.getAnneeAcademique())
                .build();
        entity = repo.save(entity);
        return toDTO(entity);
    }

    public InscriptionDTO getById(String id) {
        return repo.findById(id).map(this::toDTO).orElse(null);
    }

    public List<InscriptionDTO> listByDoctorant(String doctorantId) {
        return repo.findAllByDoctorantIdOrderByCreatedAtDesc(doctorantId)
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
        i.setStatus(InscriptionStatus.EN_ATTENTE_ADMIN);
        i = repo.save(i);
        return toDTO(i);
    }

    public InscriptionDTO approveByDirector(String id) {
        var i = repo.findById(id).orElseThrow();
        i.setStatus(InscriptionStatus.VALIDE);
        i = repo.save(i);
        return toDTO(i);
    }

    public InscriptionDTO approveByAdmin(String id) {
        var i = repo.findById(id).orElseThrow();
        // contrôle des pièces jointes (placeholder, à brancher sur module documents)
        if (!hasAllRequiredDocuments(i.getId())) {
            throw new IllegalStateException("Documents requis manquants ou non conformes");
        }
        // Après validation administrative, passer à la validation scientifique (directeur)
        i.setStatus(InscriptionStatus.EN_ATTENTE_DIRECTEUR);
        i = repo.save(i);
        return toDTO(i);
    }

    public InscriptionDTO reject(String id) {
        var i = repo.findById(id).orElseThrow();
        i.setStatus(InscriptionStatus.REJETE);
        i = repo.save(i);
        return toDTO(i);
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

    // TODO: brancher sur service de documents pour un vrai contrôle
    private boolean hasAllRequiredDocuments(String inscriptionId) {
        return true;
    }
}
