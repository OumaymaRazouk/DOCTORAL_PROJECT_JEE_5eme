package com.exemple.soutenancestatgrpcservice.model;

import java.util.List;

public class SoutenanceView {
    private String id;
    private String doctorantId;
    private String directeurId;
    private String status;
    private String titreTheseDefinitif;
    private String datePlanifiee;
    private String salle;
    private String lieu;
    private List<JuryMemberView> jury;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDoctorantId() { return doctorantId; }
    public void setDoctorantId(String doctorantId) { this.doctorantId = doctorantId; }

    public String getDirecteurId() { return directeurId; }
    public void setDirecteurId(String directeurId) { this.directeurId = directeurId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTitreTheseDefinitif() { return titreTheseDefinitif; }
    public void setTitreTheseDefinitif(String titreTheseDefinitif) { this.titreTheseDefinitif = titreTheseDefinitif; }

    public String getDatePlanifiee() { return datePlanifiee; }
    public void setDatePlanifiee(String datePlanifiee) { this.datePlanifiee = datePlanifiee; }

    public String getSalle() { return salle; }
    public void setSalle(String salle) { this.salle = salle; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public List<JuryMemberView> getJury() { return jury; }
    public void setJury(List<JuryMemberView> jury) { this.jury = jury; }
}
