package com.exemple.soutenanceservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "soutenances")
public class Soutenance {

    @Id
    private String id;

    private String doctorantId;
    private String directeurId;

    @Enumerated(EnumType.STRING)
    private SoutenanceStatus status;

    // Contenu scientifique
    private String titreTheseDefinitif;

    @Column(length = 2000)
    private String resumeFinal;

    /**
     * Mots clés séparés par des virgules, max 5.
     */
    private String motsCles;

    // Prérequis quantitatifs
    private int publicationsCount;          // >= 2 Q1/Q2
    private int communicationsCount;        // >= 2
    private int heuresFormationCumulees;    // >= 200
    private int dureeDoctoratAnnees;        // <= 6

    // Pièces jointes (chemins sur disque ou URLs de stockage)
    private String manuscritPath;
    private String rapportThesePath;
    private String rapportAntiPlagiatPath;
    private String preuvesPublicationsPath;
    private String preuvesCommunicationsPath;
    private String attestationsFormationsPath;
    private String demandeManuscritePath;

    // Commentaires / historique synthétique
    private String commentairePrerequis;
    private String commentaireJury;
    private String motifRejet;

    // Planification
    private LocalDateTime dateSoumission;
    private LocalDateTime dateAutorisation;
    private LocalDateTime datePlanifiee;
    private String salle;
    private String lieu;
    private String lienVisioconference;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.status == null) {
            this.status = SoutenanceStatus.BROUILLON;
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
