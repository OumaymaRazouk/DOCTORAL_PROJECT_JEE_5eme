package com.exemple.inscriptionstatservice.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class InscriptionView {
    private String id;
    private String type;
    private String status;
    private String doctorantId;
    private String directeurId;
    private String sujetThese;
    private String laboratoire;
    private String specialite;
    private String anneeAcademique;
    private String nom;
    private String emailContact;
    private LocalDate dateDemande;
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public String getEmailContact() {
        return emailContact;
    }

    public void setEmailContact(String emailContact) {
        this.emailContact = emailContact;
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
}
