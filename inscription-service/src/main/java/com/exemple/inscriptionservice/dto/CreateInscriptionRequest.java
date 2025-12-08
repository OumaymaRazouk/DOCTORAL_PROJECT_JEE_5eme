package com.exemple.inscriptionservice.dto;

import lombok.Data;

@Data
public class CreateInscriptionRequest {
    private String doctorantId;
    private String directeurId;
    private String coDirecteurId;

    private String sujetThese;
    private String laboratoire;
    private String specialite;
    private String anneeAcademique; // ex: 2025-2026

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
    private String rapportAvancement;
    private String publicationsN1;
    private String communicationsN1;
    private String heuresFormationCumulees;
}
