package com.exemple.documentservice.dto;

import java.time.LocalDateTime;

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

    public SoutenanceDto() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDoctorantId() { return doctorantId; }
    public void setDoctorantId(String doctorantId) { this.doctorantId = doctorantId; }

    public String getDirecteurId() { return directeurId; }
    public void setDirecteurId(String directeurId) { this.directeurId = directeurId; }

    public SoutenanceStatus getStatus() { return status; }
    public void setStatus(SoutenanceStatus status) { this.status = status; }

    public String getTitreTheseDefinitif() { return titreTheseDefinitif; }
    public void setTitreTheseDefinitif(String titreTheseDefinitif) { this.titreTheseDefinitif = titreTheseDefinitif; }

    public LocalDateTime getDateAutorisation() { return dateAutorisation; }
    public void setDateAutorisation(LocalDateTime dateAutorisation) { this.dateAutorisation = dateAutorisation; }

    public LocalDateTime getDatePlanifiee() { return datePlanifiee; }
    public void setDatePlanifiee(LocalDateTime datePlanifiee) { this.datePlanifiee = datePlanifiee; }

    public String getSalle() { return salle; }
    public void setSalle(String salle) { this.salle = salle; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }
}
