package com.exemple.soutenanceservice.service;

import lombok.Data;

/**
 * Input GraphQL pour la configuration des prérequis de soutenance
 * par doctorant.
 */
@Data
public class PrerequisConfigInput {

    private String doctorantId;

    private int minPublications;
    private int minCommunications;
    private int minHeuresFormation;
    private int maxDureeAnnees;

    private boolean requireManuscrit;
    private boolean requireAntiPlagiat;
    private boolean requireAttestations;
    private boolean requireDemandeManuscrite;
}
