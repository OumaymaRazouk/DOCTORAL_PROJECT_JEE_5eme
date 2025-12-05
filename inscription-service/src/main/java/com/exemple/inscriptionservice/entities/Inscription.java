package com.exemple.inscriptionservice.entities;

import com.exemple.inscriptionservice.enums.InscriptionStatus;
import com.exemple.inscriptionservice.enums.InscriptionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "inscriptions")
public class Inscription {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private InscriptionType type;           // INSCRIPTION ou REINSCRIPTION

    @Enumerated(EnumType.STRING)
    private InscriptionStatus status;       // BROUILLON, EN_ATTENTE_VALIDATION, VALIDE, REJETE

    // Références aux acteurs (IDs venant du user-service)
    private String doctorantId;
    private String directeurId;
    private String coDirecteurId;

    // Données académiques
    private String sujetThese;
    private String laboratoire;
    private String specialite;
    private String anneeAcademique;         // ex: 2025-2026

    // Données personnelles liées au dossier d'inscription
    private String nom;
    private String cin;
    private String telephone;
    private String emailContact;
    private String cne;
    private String dateNaissance;
    private String lieuNaissance;
    private String nationalite;
    private String adresse;
    private String situationFamiliale;
    private String resumeThese;

    // Motif de rejet éventuel (directeur ou administration)
    private String motifRejet;

    // Métadonnées
    private LocalDate dateDemande;          // date de soumission
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.status == null) this.status = InscriptionStatus.BROUILLON;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.dateDemande == null) this.dateDemande = LocalDate.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
