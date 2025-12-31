package com.exemple.soutenanceservice.dto;

import com.exemple.soutenanceservice.model.JuryRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JuryMemberDto {

    private String nom;
    private String prenom;
    private String affiliation;
    private String email;
    private String telephone;
    private JuryRole role;
}
