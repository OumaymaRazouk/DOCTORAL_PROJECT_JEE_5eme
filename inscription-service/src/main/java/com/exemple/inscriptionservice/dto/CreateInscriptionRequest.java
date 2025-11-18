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
}
