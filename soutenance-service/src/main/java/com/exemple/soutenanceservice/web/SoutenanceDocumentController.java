package com.exemple.soutenanceservice.web;

import com.exemple.soutenanceservice.model.Soutenance;
import com.exemple.soutenanceservice.model.SoutenanceStatus;
import com.exemple.soutenanceservice.model.JuryMember;
import com.exemple.soutenanceservice.repository.SoutenanceRepository;
import com.exemple.soutenanceservice.repository.JuryMemberRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/soutenances")
@RequiredArgsConstructor
public class SoutenanceDocumentController {

    private final SoutenanceRepository soutenanceRepository;
    private final JuryMemberRepository juryMemberRepository;

    @GetMapping("/{id}/autorisation.pdf")
    public ResponseEntity<byte[]> generateAutorisationPdf(@PathVariable String id) {
        Soutenance s = soutenanceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (s.getStatus() != SoutenanceStatus.SOUTENANCE_AUTORISEE
                && s.getStatus() != SoutenanceStatus.SOUTENANCE_PLANIFIEE
                && s.getStatus() != SoutenanceStatus.SOUTENANCE_EFFECTUEE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La soutenance doit être autorisée pour générer l'autorisation");
        }

        try {
            byte[] data = buildAutorisationPdf(s);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("autorisation-" + id + ".pdf")
                    .build());
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la génération du PDF d'autorisation", e);
        }
    }

    @GetMapping("/{id}/proces-verbal.pdf")
    public ResponseEntity<byte[]> generateProcesVerbalPdf(@PathVariable String id) {
        Soutenance s = soutenanceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (s.getStatus() != SoutenanceStatus.SOUTENANCE_PLANIFIEE
                && s.getStatus() != SoutenanceStatus.SOUTENANCE_EFFECTUEE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La soutenance doit être planifiée ou effectuée pour générer le procès-verbal");
        }

        List<JuryMember> jury = juryMemberRepository.findAllBySoutenance_Id(id);

        try {
            byte[] data = buildProcesVerbalPdf(s, jury);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("proces-verbal-" + id + ".pdf")
                    .build());
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la génération du procès-verbal de soutenance", e);
        }
    }

    private byte[] buildAutorisationPdf(Soutenance s) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 18);
                content.newLineAtOffset(70, 780);
                content.showText("Autorisation de soutenance de thèse");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(70, 740);
                content.showText("Doctorant : " + nullSafe(s.getDoctorantId()));
                content.newLineAtOffset(0, -18);
                content.showText("Directeur de thèse : " + nullSafe(s.getDirecteurId()));
                content.newLineAtOffset(0, -18);
                content.showText("Titre de la thèse : " + nullSafe(s.getTitreTheseDefinitif()));
                content.newLineAtOffset(0, -18);
                content.showText("Statut : " + s.getStatus().name());
                content.newLineAtOffset(0, -18);
                if (s.getDateAutorisation() != null) {
                    String date = s.getDateAutorisation().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    content.showText("Date d'autorisation : " + date);
                } else {
                    content.showText("Date d'autorisation : non renseignée");
                }
                content.endText();
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] buildProcesVerbalPdf(Soutenance s, List<JuryMember> jury) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 18);
                content.newLineAtOffset(70, 780);
                content.showText("Procès-verbal de soutenance de thèse");
                content.endText();

                float y = 740;
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(70, y);
                content.showText("Doctorant : " + nullSafe(s.getDoctorantId()));
                content.newLineAtOffset(0, -18);
                content.showText("Directeur de thèse : " + nullSafe(s.getDirecteurId()));
                content.newLineAtOffset(0, -18);
                content.showText("Titre de la thèse : " + nullSafe(s.getTitreTheseDefinitif()));
                content.newLineAtOffset(0, -18);
                if (s.getDatePlanifiee() != null) {
                    String date = s.getDatePlanifiee().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    content.showText("Date et heure : " + date);
                } else {
                    content.showText("Date et heure : non renseignées");
                }
                content.newLineAtOffset(0, -18);
                content.showText("Salle / Lieu : " + nullSafe(s.getSalle()) + " " + nullSafe(s.getLieu()));
                content.endText();

                y = 620;
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 14);
                content.newLineAtOffset(70, y);
                content.showText("Composition du jury");
                content.endText();

                y -= 24;
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(70, y);
                if (jury == null || jury.isEmpty()) {
                    content.showText("Aucun jury défini.");
                } else {
                    for (JuryMember m : jury) {
                        String line = String.format("%s - %s %s (%s)",
                                m.getRole() != null ? m.getRole().name() : "",
                                nullSafe(m.getNom()),
                                nullSafe(m.getPrenom()),
                                nullSafe(m.getAffiliation()));
                        content.showText(line);
                        content.newLineAtOffset(0, -16);
                    }
                }
                content.endText();

                y -= 120;
                if (y < 200) {
                    y = 200;
                }
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 14);
                content.newLineAtOffset(70, y);
                content.showText("Décision du jury");
                content.endText();

                y -= 24;
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(70, y);
                content.showText("Décision : ...............................................................");
                content.newLineAtOffset(0, -18);
                content.showText("Mention : ................................................................");
                content.newLineAtOffset(0, -18);
                content.showText("Observations : ...........................................................");
                content.endText();
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
