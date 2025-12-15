package com.exemple.inscriptionstatservice.ws;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InscriptionYearSummary", propOrder = {
        "anneeAcademique",
        "total",
        "inscriptions"
})
public class InscriptionYearSummaryType {

    private String anneeAcademique;
    private long total;
    private List<InscriptionViewType> inscriptions;

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

    public List<InscriptionViewType> getInscriptions() {
        return inscriptions;
    }

    public void setInscriptions(List<InscriptionViewType> inscriptions) {
        this.inscriptions = inscriptions;
    }
}
