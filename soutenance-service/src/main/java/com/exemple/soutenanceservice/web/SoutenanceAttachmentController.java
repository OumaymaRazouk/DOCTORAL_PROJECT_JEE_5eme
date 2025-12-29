package com.exemple.soutenanceservice.web;

import com.exemple.soutenanceservice.model.Soutenance;
import com.exemple.soutenanceservice.service.SoutenanceAttachmentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/soutenances")
public class SoutenanceAttachmentController {

    private final SoutenanceAttachmentService attachmentService;

    public SoutenanceAttachmentController(SoutenanceAttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Soutenance> uploadAttachments(
            @PathVariable String id,
            @RequestParam(value = "manuscrit", required = false) MultipartFile manuscrit,
            @RequestParam(value = "rapportAntiPlagiat", required = false) MultipartFile rapportAntiPlagiat,
            @RequestParam(value = "attestationsFormations", required = false) MultipartFile attestationsFormations,
            @RequestParam(value = "demandeManuscrite", required = false) MultipartFile demandeManuscrite
    ) {
        Soutenance s = attachmentService.saveAttachments(id, manuscrit, rapportAntiPlagiat, attestationsFormations, demandeManuscrite);
        return ResponseEntity.ok(s);
    }

    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<String>> listAttachments(@PathVariable String id) {
        return ResponseEntity.ok(attachmentService.listAttachments(id));
    }

    @GetMapping("/{id}/attachments/{fileName}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable String id, @PathVariable String fileName) {
        Resource resource = attachmentService.loadAttachment(id, fileName);
        if (resource == null || !resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(resource);
    }
}
