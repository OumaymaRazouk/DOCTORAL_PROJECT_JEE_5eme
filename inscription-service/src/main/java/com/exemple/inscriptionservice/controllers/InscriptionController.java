package com.exemple.inscriptionservice.controllers;

import com.exemple.inscriptionservice.dto.CreateInscriptionRequest;
import com.exemple.inscriptionservice.dto.InscriptionDTO;
import com.exemple.inscriptionservice.services.InscriptionService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.security.PermitAll;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

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

    // Mise à jour d'une inscription par le doctorant (brouillon uniquement)
    @PreAuthorize("hasAnyAuthority('DOCTORANT','SUPERADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<InscriptionDTO> update(@PathVariable String id, @RequestBody CreateInscriptionRequest request) {
        return ResponseEntity.ok(service.updateInscription(id, request));
    }

    // Suppression d'une inscription en brouillon par le doctorant
    @PreAuthorize("hasAnyAuthority('DOCTORANT','SUPERADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteInscription(id);
        return ResponseEntity.noContent().build();
    }

    // Réinscription annuelle
    @PreAuthorize("hasAnyAuthority('DOCTORANT','SUPERADMIN')")
    @PostMapping("/reinscriptions")
    public ResponseEntity<InscriptionDTO> createReinscription(@RequestBody CreateInscriptionRequest request) {
        return ResponseEntity.ok(service.createReinscription(request));
    }

    // Pré-remplissage depuis N-1
    @PreAuthorize("hasAnyAuthority('DOCTORANT','SUPERADMIN')")
    @PostMapping("/reinscriptions/prefill")
    public ResponseEntity<InscriptionDTO> prefill(@RequestParam String doctorantId, @RequestParam String annee) {
        return ResponseEntity.ok(service.prefillFromLastYear(doctorantId, annee));
    }

    // Consultation d'une demande
    @PreAuthorize("hasAnyAuthority('DOCTORANT','DIRECTEUR','PERSONNEL_ADMINISTRATIF','SUPERADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<InscriptionDTO> getById(@PathVariable String id) {
        var dto = service.getById(id);
        return dto == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
    }

    // Historique d'un doctorant
    @PreAuthorize("hasAnyAuthority('DOCTORANT','DIRECTEUR','PERSONNEL_ADMINISTRATIF','SUPERADMIN')")
    @GetMapping("/doctorants/{doctorantId}")
    public ResponseEntity<List<InscriptionDTO>> listByDoctorant(@PathVariable String doctorantId) {
        return ResponseEntity.ok(service.listByDoctorant(doctorantId));
    }

    // Liste des demandes pour un directeur de thèse (tous les dossiers qu'il encadre)
    @PermitAll
    @GetMapping("/directeurs/{directeurId}")
    public ResponseEntity<List<InscriptionDTO>> listByDirecteur(@PathVariable String directeurId) {
        return ResponseEntity.ok(service.listByDirecteur(directeurId));
    }

    // Liste des demandes en attente de validation administrative
    @PreAuthorize("hasAnyAuthority('PERSONNEL_ADMINISTRATIF','SUPERADMIN')")
    @GetMapping("/admin/en-attente")
    public ResponseEntity<List<InscriptionDTO>> listPendingForAdmin() {
        return ResponseEntity.ok(service.listPendingForAdmin());
    }

    // Soumission au directeur de thèse
    @PreAuthorize("hasAnyAuthority('DOCTORANT','SUPERADMIN')")
    @PostMapping("/{id}/submit")
    public ResponseEntity<InscriptionDTO> submit(@PathVariable String id) {
        return ResponseEntity.ok(service.submitToDirector(id));
    }

    // Validation directeur
    @PreAuthorize("hasAnyAuthority('DIRECTEUR','SUPERADMIN')")
    @PostMapping("/{id}/director-approve")
    public ResponseEntity<InscriptionDTO> directorApprove(@PathVariable String id) {
        return ResponseEntity.ok(service.approveByDirector(id));
    }

    // Validation administration (personnel administratif)
    @PreAuthorize("hasAnyAuthority('PERSONNEL_ADMINISTRATIF','SUPERADMIN')")
    @PostMapping("/{id}/admin-approve")
    public ResponseEntity<InscriptionDTO> adminApprove(@PathVariable String id) {
        return ResponseEntity.ok(service.approveByAdmin(id));
    }

    // Rejet (par directeur ou personnel administratif)
    @PreAuthorize("hasAnyAuthority('DIRECTEUR','PERSONNEL_ADMINISTRATIF','SUPERADMIN')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<InscriptionDTO> reject(@PathVariable String id, @RequestParam(required = false) String motif) {
        return ResponseEntity.ok(service.reject(id, motif));
    }

    @PermitAll
    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InscriptionDTO> uploadAttachments(
            @PathVariable String id,
            @RequestParam(value = "diplome", required = false) MultipartFile diplome,
            @RequestParam(value = "cv", required = false) MultipartFile cv,
            @RequestParam(value = "lettreMotivation", required = false) MultipartFile lettreMotivation,
            @RequestParam(value = "cin", required = false) MultipartFile cin) {
        return ResponseEntity.ok(service.saveAttachments(id, diplome, cv, lettreMotivation, cin));
    }

    @PreAuthorize("hasAnyAuthority('DOCTORANT','DIRECTEUR','PERSONNEL_ADMINISTRATIF','SUPERADMIN')")
    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<String>> listAttachments(@PathVariable String id) {
        return ResponseEntity.ok(service.listAttachments(id));
    }

    @PreAuthorize("hasAnyAuthority('DOCTORANT','DIRECTEUR','PERSONNEL_ADMINISTRATIF','SUPERADMIN')")
    @GetMapping("/{id}/attachments/{fileName}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable String id, @PathVariable String fileName) {
        Resource resource = service.loadAttachment(id, fileName);
        if (resource == null || !resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
