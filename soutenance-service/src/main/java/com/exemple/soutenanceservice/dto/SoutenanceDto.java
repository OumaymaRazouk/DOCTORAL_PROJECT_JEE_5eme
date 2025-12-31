package com.exemple.soutenanceservice.dto;

import com.exemple.soutenanceservice.model.SoutenanceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SoutenanceDto {

    private String id;
    private String doctorantId;
    private String directeurId;
    private SoutenanceStatus status;
    private String titreTheseDefinitif;
    private LocalDateTime dateAutorisation;
    private LocalDateTime datePlanifiee;
    private String salle;
    private String lieu;
}
