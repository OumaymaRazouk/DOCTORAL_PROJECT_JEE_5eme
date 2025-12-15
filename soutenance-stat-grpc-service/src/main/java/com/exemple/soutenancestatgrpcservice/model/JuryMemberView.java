package com.exemple.soutenancestatgrpcservice.model;

public class JuryMemberView {
    private String nom;
    private String prenom;
    private String affiliation;
    private String role;

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getAffiliation() { return affiliation; }
    public void setAffiliation(String affiliation) { this.affiliation = affiliation; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
