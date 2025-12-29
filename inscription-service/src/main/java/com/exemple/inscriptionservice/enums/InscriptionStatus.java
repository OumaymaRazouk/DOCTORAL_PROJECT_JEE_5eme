package com.exemple.inscriptionservice.enums;

public enum InscriptionStatus {
    BROUILLON,
    EN_ATTENTE_DIRECTEUR,
    EN_ATTENTE_ADMIN,
    // Statut legacy conserve pour compatibilite avec d'anciens enregistrements
    EN_ATTENTE_VALIDATION,
    VALIDE,
    REJETE
}
