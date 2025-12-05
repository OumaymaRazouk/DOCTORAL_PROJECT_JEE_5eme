package com.exemple.inscriptionservice.dto;

import com.exemple.inscriptionservice.enums.InscriptionStatus;
import com.exemple.inscriptionservice.enums.InscriptionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class InscriptionDTO {
    private String id;
    private InscriptionType type;
    private InscriptionStatus status;
    private String doctorantId;
    private String directeurId;
    private String coDirecteurId;
    private String sujetThese;
    private String laboratoire;
    private String specialite;
    private String anneeAcademique;

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
    private String motifRejet;
    private LocalDate dateDemande;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
