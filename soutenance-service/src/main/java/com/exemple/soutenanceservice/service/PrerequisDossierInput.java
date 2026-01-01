package com.exemple.soutenanceservice.service;

import lombok.Data;

@Data
public class PrerequisDossierInput {
    private String doctorantId;
    private int publicationsCount;
    private int communicationsCount;
    private int heuresFormationCumulees;
    private int dureeDoctoratAnnees;
}
