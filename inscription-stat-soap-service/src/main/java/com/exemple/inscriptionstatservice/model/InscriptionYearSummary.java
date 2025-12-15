package com.exemple.inscriptionstatservice.model;

import java.util.List;

public class InscriptionYearSummary {
    private String anneeAcademique;
    private long total;
    private List<InscriptionView> inscriptions;

    public String getAnneeAcademique() {
        return anneeAcademique;
    }

    public void setAnneeAcademique(String anneeAcademique) {
        this.anneeAcademique = anneeAcademique;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<InscriptionView> getInscriptions() {
        return inscriptions;
    }

    public void setInscriptions(List<InscriptionView> inscriptions) {
        this.inscriptions = inscriptions;
    }
}
