package com.exemple.soutenanceservice.service;

import com.exemple.soutenanceservice.model.*;
import com.exemple.soutenanceservice.repository.JuryMemberRepository;
import com.exemple.soutenanceservice.repository.SoutenanceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
@RequiredArgsConstructor
public class SoutenanceDomainService {

    private static final Logger log = LoggerFactory.getLogger(SoutenanceDomainService.class);

    private final SoutenanceRepository soutenanceRepository;
    private final JuryMemberRepository juryMemberRepository;

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
        s.setPublicationsCount(input.getPublicationsCount());
        s.setCommunicationsCount(input.getCommunicationsCount());
        s.setHeuresFormationCumulees(input.getHeuresFormationCumulees());
        s.setDureeDoctoratAnnees(input.getDureeDoctoratAnnees());
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

    private String checkPrerequis(Soutenance s) {
        StringBuilder sb = new StringBuilder();
        if (s.getPublicationsCount() < 2) {
            sb.append("au moins 2 publications Q1/Q2; ");
        }
        if (s.getCommunicationsCount() < 2) {
            sb.append("au moins 2 communications; ");
        }
        if (s.getHeuresFormationCumulees() < 200) {
            sb.append("200h de formation cumulées; ");
        }
        if (s.getDureeDoctoratAnnees() > 6) {
            sb.append("durée du doctorat > 6 ans; ");
        }
        return sb.toString();
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
        return saved;
    }

    public Flux<Soutenance> soutenanceUpdates(String id) {
        return soutenanceSink.asFlux()
                .filter(s -> s.getId() != null && s.getId().equals(id));
    }
}
