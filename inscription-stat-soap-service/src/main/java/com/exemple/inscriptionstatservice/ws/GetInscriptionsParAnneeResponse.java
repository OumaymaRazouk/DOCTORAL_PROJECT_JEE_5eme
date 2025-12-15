package com.exemple.inscriptionstatservice.ws;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetInscriptionsParAnneeResponse", propOrder = {"summaries"})
@XmlRootElement(name = "GetInscriptionsParAnneeResponse", namespace = "http://exemple.com/inscriptionstats")
public class GetInscriptionsParAnneeResponse {

    @XmlElement(name = "summary")
    private List<InscriptionYearSummaryType> summaries;

    public List<InscriptionYearSummaryType> getSummaries() {
        return summaries;
    }

    public void setSummaries(List<InscriptionYearSummaryType> summaries) {
        this.summaries = summaries;
    }
}
