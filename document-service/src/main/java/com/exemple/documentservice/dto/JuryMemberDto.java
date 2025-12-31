package com.exemple.documentservice.dto;

public class JuryMemberDto {
    private String nom;
    private String prenom;
    private String affiliation;
    private String email;
    private String telephone;
    private String role; // simple String, on n'a besoin que de l'affichage

    public JuryMemberDto() {}

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getAffiliation() { return affiliation; }
    public void setAffiliation(String affiliation) { this.affiliation = affiliation; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
