package com.exemple.utilisateursservice.entities;

import com.exemple.utilisateursservice.model.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.time.Instant;

@Entity
@Table(name = "users", uniqueConstraints = {@UniqueConstraint(columnNames = "email")})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Builder.Default
    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // Identifiant fonctionnel (ex: DIR-2026-000123)
    @Column(unique = true)
    private String identifiant;

    // Métadonnées établissement et spécialité/laboratoire
    private String etablissement;
    private String specialite;

    // Activation via lien (Option B)
    @Column(length = 200)
    private String activationToken;
    private Instant activationExpiresAt;
}
