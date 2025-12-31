package com.exemple.soutenanceservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration des prérequis de soutenance par doctorant.
 * Si aucune entrée n'existe pour un doctorant donné, les valeurs globales
 * définies dans la configuration de l'application sont utilisées.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "soutenance_prerequis_configs")
public class PrerequisSoutenanceConfig {

    @Id
    private String doctorantId;

    private int minPublications;      // >= 2 Q1/Q2 par défaut
    private int minCommunications;    // >= 2 par défaut
    private int minHeuresFormation;   // >= 200 par défaut
    private int maxDureeAnnees;       // <= 6 par défaut

    private boolean requireManuscrit;         // true par défaut
    private boolean requireAntiPlagiat;       // true par défaut
    private boolean requireAttestations;      // true par défaut
    private boolean requireDemandeManuscrite; // true par défaut
}
