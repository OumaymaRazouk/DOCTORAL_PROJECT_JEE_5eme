package com.exemple.soutenanceservice.service;

import com.exemple.soutenanceservice.model.JuryRole;
import lombok.Data;

@Data
public class JuryMemberInput {
    private String nom;
    private String prenom;
    private String affiliation;
    private String email;
    private String telephone;
    private JuryRole role;
}
