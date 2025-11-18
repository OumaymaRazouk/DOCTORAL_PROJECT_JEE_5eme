package com.exemple.inscriptionservice.controllers;

import com.exemple.inscriptionservice.dto.CreateInscriptionRequest;
import com.exemple.inscriptionservice.dto.InscriptionDTO;
import com.exemple.inscriptionservice.services.InscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.security.PermitAll;

import java.util.List;

@RestController
@RequestMapping("/inscriptions")
public class InscriptionController {

    private final InscriptionService service;

    public InscriptionController(InscriptionService service) {
        this.service = service;
    }

    // Inscription initiale par le candidat (ou via back-office)
    @PermitAll
    @PostMapping
    public ResponseEntity<InscriptionDTO> create(@RequestBody CreateInscriptionRequest request) {
        return ResponseEntity.ok(service.createInscription(request));
    }

    // Réinscription annuelle
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTORANT','DOCTORANT','ROLE_ADMIN','ADMIN','ROLE_SUPER_ADMIN','SUPER_ADMIN')")
    @PostMapping("/reinscriptions")
    public ResponseEntity<InscriptionDTO> createReinscription(@RequestBody CreateInscriptionRequest request) {
        return ResponseEntity.ok(service.createReinscription(request));
    }

    // Pré-remplissage depuis N-1
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTORANT','DOCTORANT','ROLE_ADMIN','ADMIN','ROLE_SUPER_ADMIN','SUPER_ADMIN')")
    @PostMapping("/reinscriptions/prefill")
    public ResponseEntity<InscriptionDTO> prefill(@RequestParam String doctorantId, @RequestParam String annee) {
        return ResponseEntity.ok(service.prefillFromLastYear(doctorantId, annee));
    }

    // Consultation d'une demande
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTORANT','DOCTORANT','ROLE_ENCADRANT','ENCADRANT','ROLE_ADMIN','ADMIN','ROLE_SUPER_ADMIN','SUPER_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<InscriptionDTO> getById(@PathVariable String id) {
        var dto = service.getById(id);
        return dto == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
    }

    // Historique d'un doctorant
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTORANT','DOCTORANT','ROLE_ENCADRANT','ENCADRANT','ROLE_ADMIN','ADMIN','ROLE_SUPER_ADMIN','SUPER_ADMIN')")
    @GetMapping("/doctorants/{doctorantId}")
    public ResponseEntity<List<InscriptionDTO>> listByDoctorant(@PathVariable String doctorantId) {
        return ResponseEntity.ok(service.listByDoctorant(doctorantId));
    }

    // Soumission à l'encadrant
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTORANT','DOCTORANT','ROLE_ADMIN','ADMIN','ROLE_SUPER_ADMIN','SUPER_ADMIN')")
    @PostMapping("/{id}/submit")
    public ResponseEntity<InscriptionDTO> submit(@PathVariable String id) {
        return ResponseEntity.ok(service.submitToDirector(id));
    }

    // Validation encadrant
    @PreAuthorize("hasAnyAuthority('ROLE_ENCADRANT','ENCADRANT','ROLE_ADMIN','ADMIN','ROLE_SUPER_ADMIN','SUPER_ADMIN')")
    @PostMapping("/{id}/director-approve")
    public ResponseEntity<InscriptionDTO> directorApprove(@PathVariable String id) {
        return ResponseEntity.ok(service.approveByDirector(id));
    }

    // Validation administration
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN','ROLE_SUPER_ADMIN','SUPER_ADMIN')")
    @PostMapping("/{id}/admin-approve")
    public ResponseEntity<InscriptionDTO> adminApprove(@PathVariable String id) {
        return ResponseEntity.ok(service.approveByAdmin(id));
    }

    // Rejet (par encadrant ou admin)
    @PreAuthorize("hasAnyAuthority('ROLE_ENCADRANT','ENCADRANT','ROLE_ADMIN','ADMIN','ROLE_SUPER_ADMIN','SUPER_ADMIN')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<InscriptionDTO> reject(@PathVariable String id) {
        return ResponseEntity.ok(service.reject(id));
    }
}
