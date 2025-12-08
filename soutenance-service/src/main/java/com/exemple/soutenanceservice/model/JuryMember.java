package com.exemple.soutenanceservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "jury_members")
public class JuryMember {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "soutenance_id")
    private Soutenance soutenance;

    private String nom;
    private String prenom;
    private String affiliation;
    private String email;
    private String telephone;

    @Enumerated(EnumType.STRING)
    private JuryRole role;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
}
