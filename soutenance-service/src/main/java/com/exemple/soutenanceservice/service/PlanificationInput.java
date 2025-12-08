package com.exemple.soutenanceservice.service;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlanificationInput {
    private LocalDateTime dateHeure;
    private String salle;
    private String lieu;
    private String lienVisioconference;
}
