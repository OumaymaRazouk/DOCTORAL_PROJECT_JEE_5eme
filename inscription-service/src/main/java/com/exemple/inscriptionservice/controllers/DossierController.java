package com.exemple.inscriptionservice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dossiers")
public class DossierController {

    // Exemple: dossier du doctorant courant
    @PreAuthorize("hasRole('DOCTORANT')")
    @GetMapping("/me")
    public ResponseEntity<?> getMyDossier(Authentication auth) {
        // Placeholder: brancher plus tard sur service/metier + DB
        return ResponseEntity.ok(Map.of(
                "owner", auth.getName(),
                "status", "BROUILLON",
                "message", "Dossier du doctorant courant (exemple)."
        ));
    }

    // Exemple: action de validation par encadrant/administration
    @PreAuthorize("hasAnyRole('DIRECTEUR','PERSONNEL_ADMINISTRATIF','SUPERADMIN')")
    @PostMapping("/{id}/valider")
    public ResponseEntity<?> validerDossier(@PathVariable Long id, Authentication auth) {
        // Placeholder: brancher plus tard sur service/metier + DB
        return ResponseEntity.ok(Map.of(
                "dossierId", id,
                "validatedBy", auth.getName(),
                "message", "Validation effectuée (exemple)."
        ));
    }
}
