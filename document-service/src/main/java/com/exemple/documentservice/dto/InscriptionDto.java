package com.exemple.documentservice.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class InscriptionDto {
    private String id;
    private InscriptionType type;
    private InscriptionStatus status;
    private String doctorantId;
    private String directeurId;
    private String coDirecteurId;
    private String sujetThese;
    private String laboratoire;
    private String specialite;
    private String anneeAcademique;
    private String nom;
    private String cin;
    private String telephone;
    private String emailContact;
    private String cne;
    private String dateNaissance;
    private String lieuNaissance;
    private String nationalite;
    private String adresse;
    private String situationFamiliale;
    private String resumeThese;
    private String rapportAvancement;
    private String publicationsN1;
    private String communicationsN1;
    private String heuresFormationCumulees;
    private String motifRejet;
    private LocalDate dateDemande;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InscriptionDto() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public InscriptionType getType() {
        return type;
    }

    public void setType(InscriptionType type) {
        this.type = type;
    }

    public InscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(InscriptionStatus status) {
        this.status = status;
    }

    public String getDoctorantId() {
        return doctorantId;
    }

    public void setDoctorantId(String doctorantId) {
        this.doctorantId = doctorantId;
    }

    public String getDirecteurId() {
        return directeurId;
    }

    public void setDirecteurId(String directeurId) {
        this.directeurId = directeurId;
    }

    public String getCoDirecteurId() {
        return coDirecteurId;
    }

    public void setCoDirecteurId(String coDirecteurId) {
        this.coDirecteurId = coDirecteurId;
    }

    public String getSujetThese() {
        return sujetThese;
    }

    public void setSujetThese(String sujetThese) {
        this.sujetThese = sujetThese;
    }

    public String getLaboratoire() {
        return laboratoire;
    }

    public void setLaboratoire(String laboratoire) {
        this.laboratoire = laboratoire;
    }

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public String getAnneeAcademique() {
        return anneeAcademique;
    }

    public void setAnneeAcademique(String anneeAcademique) {
        this.anneeAcademique = anneeAcademique;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getCin() {
        return cin;
    }

    public void setCin(String cin) {
        this.cin = cin;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getEmailContact() {
        return emailContact;
    }

    public void setEmailContact(String emailContact) {
        this.emailContact = emailContact;
    }

    public String getCne() {
        return cne;
    }

    public void setCne(String cne) {
        this.cne = cne;
    }

    public String getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(String dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getLieuNaissance() {
        return lieuNaissance;
    }

    public void setLieuNaissance(String lieuNaissance) {
        this.lieuNaissance = lieuNaissance;
    }

    public String getNationalite() {
        return nationalite;
    }

    public void setNationalite(String nationalite) {
        this.nationalite = nationalite;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getSituationFamiliale() {
        return situationFamiliale;
    }

    public void setSituationFamiliale(String situationFamiliale) {
        this.situationFamiliale = situationFamiliale;
    }

    public String getResumeThese() {
        return resumeThese;
    }

    public void setResumeThese(String resumeThese) {
        this.resumeThese = resumeThese;
    }

    public String getRapportAvancement() {
        return rapportAvancement;
    }

    public void setRapportAvancement(String rapportAvancement) {
        this.rapportAvancement = rapportAvancement;
    }

    public String getPublicationsN1() {
        return publicationsN1;
    }

    public void setPublicationsN1(String publicationsN1) {
        this.publicationsN1 = publicationsN1;
    }

    public String getCommunicationsN1() {
        return communicationsN1;
    }

    public void setCommunicationsN1(String communicationsN1) {
        this.communicationsN1 = communicationsN1;
    }

    public String getHeuresFormationCumulees() {
        return heuresFormationCumulees;
    }

    public void setHeuresFormationCumulees(String heuresFormationCumulees) {
        this.heuresFormationCumulees = heuresFormationCumulees;
    }

    public String getMotifRejet() {
        return motifRejet;
    }

    public void setMotifRejet(String motifRejet) {
        this.motifRejet = motifRejet;
    }

    public LocalDate getDateDemande() {
        return dateDemande;
    }

    public void setDateDemande(LocalDate dateDemande) {
        this.dateDemande = dateDemande;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
