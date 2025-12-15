package com.exemple.soutenanceservice.graphql;

import com.exemple.soutenanceservice.model.JuryMember;
import com.exemple.soutenanceservice.model.Soutenance;
import com.exemple.soutenanceservice.model.SoutenanceStatus;
import com.exemple.soutenanceservice.repository.JuryMemberRepository;
import com.exemple.soutenanceservice.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import reactor.core.publisher.Flux;

@Controller
@RequiredArgsConstructor
public class SoutenanceGraphQLController {

    private final SoutenanceDomainService domainService;
    private final JuryMemberRepository juryMemberRepository;

    @QueryMapping
    public Soutenance soutenanceById(@Argument String id) {
        return domainService.getById(id);
    }

    @QueryMapping
    public List<Soutenance> soutenancesForDoctorant(@Argument String doctorantId) {
        return domainService.listForDoctorant(doctorantId);
    }

    @QueryMapping
    public List<Soutenance> soutenancesForDirecteur(@Argument String directeurId) {
        return domainService.listForDirecteur(directeurId);
    }

    @QueryMapping
    public List<Soutenance> soutenancesForAdmin(@Argument(name = "status") SoutenanceStatus status) {
        return domainService.listForAdmin(status);
    }

    @MutationMapping
    public Soutenance saveSoutenanceDraft(@Argument SoutenanceDraftInput input) {
        return domainService.saveDraft(input);
    }

    @MutationMapping
    public Soutenance submitSoutenance(@Argument String id) {
        return domainService.submit(id);
    }

    @MutationMapping
    public Soutenance adminMarkPrerequisUnderReview(@Argument String id) {
        return domainService.adminVerifiePrerequis(id);
    }

    @MutationMapping
    public Soutenance adminValidatePrerequis(@Argument String id, @Argument String commentaire) {
        return domainService.adminValidePrerequis(id, commentaire);
    }

    @MutationMapping
    public Soutenance adminRejectSoutenance(@Argument String id, @Argument String motif) {
        return domainService.adminRejette(id, motif);
    }

    @MutationMapping
    public Soutenance directeurProposeJury(@Argument String id, @Argument List<JuryMemberInput> membres) {
        return domainService.directeurProposeJury(id, membres);
    }

    @MutationMapping
    public Soutenance adminValidateJury(@Argument String id, @Argument String commentaire) {
        return domainService.adminValideJury(id, commentaire);
    }

    @MutationMapping
    public Soutenance adminAuthorizeSoutenance(@Argument String id) {
        return domainService.adminAutoriseSoutenance(id);
    }

    @MutationMapping
    public Soutenance directeurProposePlanification(@Argument String id, @Argument PlanificationInput input) {
        return domainService.directeurProposePlanification(id, input);
    }

    @MutationMapping
    public Soutenance adminPlanifySoutenance(@Argument String id, @Argument PlanificationInput input) {
        return domainService.adminPlanifieSoutenance(id, input);
    }

    @MutationMapping
    public Soutenance adminMarkSoutenanceDone(@Argument String id, @Argument String dateEffective) {
        LocalDateTime effective = null;
        if (dateEffective != null && !dateEffective.isBlank()) {
            try {
                effective = LocalDateTime.parse(dateEffective);
            } catch (DateTimeParseException ignored) {
            }
        }
        return domainService.adminMarqueEffectuee(id, effective);
    }

    // Champ jury: exposer la liste des membres pour une soutenance
    @QueryMapping
    public List<JuryMember> juryForSoutenance(@Argument String soutenanceId) {
        if (soutenanceId == null) return Collections.emptyList();
        return juryMemberRepository.findAllBySoutenance_Id(soutenanceId);
    }

    @SubscriptionMapping
    public Flux<Soutenance> soutenanceUpdated(@Argument String id) {
        Soutenance current = domainService.getById(id);
        Flux<Soutenance> initial = current != null ? Flux.just(current) : Flux.empty();
        return initial.concatWith(domainService.soutenanceUpdates(id));
    }
}
