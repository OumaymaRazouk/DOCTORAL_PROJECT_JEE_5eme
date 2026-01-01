package com.exemple.soutenanceservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "soutenance_prerequis_dossiers")
public class PrerequisDossier {

    @Id
    private String doctorantId;

    private int publicationsCount;
    private int communicationsCount;
    private int heuresFormationCumulees;
    private int dureeDoctoratAnnees;

    @Enumerated(EnumType.STRING)
    private PrerequisDossierEtat etatScientifique;

    private String commentaireScientifique;
    private LocalDateTime dateValidationScientifique;

    @Enumerated(EnumType.STRING)
    private PrerequisDossierEtat etatAdministratif;

    private String commentaireAdministratif;
    private LocalDateTime dateValidationAdministratif;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (etatScientifique == null) {
            etatScientifique = PrerequisDossierEtat.EN_ATTENTE;
        }
        if (etatAdministratif == null) {
            etatAdministratif = PrerequisDossierEtat.EN_ATTENTE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
