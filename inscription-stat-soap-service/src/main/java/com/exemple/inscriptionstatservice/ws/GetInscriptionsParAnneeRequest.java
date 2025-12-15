package com.exemple.inscriptionstatservice.ws;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetInscriptionsParAnneeRequest", propOrder = {"anneeAcademique"})
@XmlRootElement(name = "GetInscriptionsParAnneeRequest", namespace = "http://exemple.com/inscriptionstats")
public class GetInscriptionsParAnneeRequest {

    private String anneeAcademique;

    public String getAnneeAcademique() {
        return anneeAcademique;
    }

    public void setAnneeAcademique(String anneeAcademique) {
        this.anneeAcademique = anneeAcademique;
    }
}
