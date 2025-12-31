package com.exemple.documentservice.web;

import com.exemple.documentservice.clients.InscriptionClient;
import com.exemple.documentservice.clients.SoutenanceClient;
import com.exemple.documentservice.dto.InscriptionDto;
import com.exemple.documentservice.dto.InscriptionStatus;
import com.exemple.documentservice.dto.InscriptionType;
import com.exemple.documentservice.dto.JuryMemberDto;
import com.exemple.documentservice.dto.SoutenanceDto;
import com.exemple.documentservice.dto.SoutenanceStatus;
import com.exemple.documentservice.service.HtmlTemplatePdfService;

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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final InscriptionClient inscriptionClient;
    private final SoutenanceClient soutenanceClient;
    private final HtmlTemplatePdfService htmlTemplatePdfService;

    // =============================
    //  Attestation d'inscription
    // =============================

    // Via gateway: GET /documents/inscriptions/{id}/attestation.pdf
    @GetMapping("/inscriptions/{id}/attestation.pdf")
    public ResponseEntity<byte[]> generateAttestationInscription(@PathVariable String id) {
        InscriptionDto ins = inscriptionClient.getById(id);
        if (ins == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (ins.getType() != InscriptionType.INSCRIPTION || ins.getStatus() != InscriptionStatus.VALIDE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'inscription doit etre une inscription initiale valide pour generer l'attestation");
        }

        try {
            byte[] data = htmlTemplatePdfService.generateAttestationInscription(ins);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("attestation-inscription-" + id + ".pdf")
                    .build());
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la generation de l'attestation d'inscription", e);
        }
    }

    // =============================
    //  Autorisation de soutenance
    // =============================

    // Via gateway: GET /documents/soutenances/{id}/autorisation.pdf
    @GetMapping("/soutenances/{id}/autorisation.pdf")
    public ResponseEntity<byte[]> generateAutorisationSoutenance(@PathVariable String id) {
        SoutenanceDto s = soutenanceClient.getById(id);
        if (s == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if (s.getStatus() != SoutenanceStatus.SOUTENANCE_AUTORISEE
                && s.getStatus() != SoutenanceStatus.SOUTENANCE_PLANIFIEE
                && s.getStatus() != SoutenanceStatus.SOUTENANCE_EFFECTUEE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La soutenance doit etre autorisee pour generer l'autorisation");
        }

        try {
            byte[] data = htmlTemplatePdfService.generateAutorisationSoutenance(s);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("autorisation-" + id + ".pdf")
                    .build());
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la generation du PDF d'autorisation", e);
        }
    }

    // =============================
    //  Proces-verbal de soutenance
    // =============================

    // Via gateway: GET /documents/soutenances/{id}/proces-verbal.pdf
    @GetMapping("/soutenances/{id}/proces-verbal.pdf")
    public ResponseEntity<byte[]> generateProcesVerbal(@PathVariable String id) {
        SoutenanceDto s = soutenanceClient.getById(id);
        if (s == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if (s.getStatus() != SoutenanceStatus.SOUTENANCE_PLANIFIEE
                && s.getStatus() != SoutenanceStatus.SOUTENANCE_EFFECTUEE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La soutenance doit etre planifiee ou effectuee pour generer le proces-verbal");
        }

        List<JuryMemberDto> jury = soutenanceClient.getJury(id);

        try {
            byte[] data = htmlTemplatePdfService.generateProcesVerbal(s, jury);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("proces-verbal-" + id + ".pdf")
                    .build());
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la generation du proces-verbal de soutenance", e);
        }
    }

    // =============================
    //  Attestation de reinscription annuelle
    // =============================

    // Via gateway: GET /documents/inscriptions/{id}/attestation-reinscription.pdf
    @GetMapping("/inscriptions/{id}/attestation-reinscription.pdf")
    public ResponseEntity<byte[]> generateAttestationReinscription(@PathVariable String id) {
        InscriptionDto ins = inscriptionClient.getById(id);
        if (ins == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (ins.getType() != InscriptionType.REINSCRIPTION || ins.getStatus() != InscriptionStatus.VALIDE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'inscription doit etre une reinscription valide pour generer l'attestation");
        }

        try {
            byte[] data = htmlTemplatePdfService.generateAttestationReinscription(ins);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("attestation-reinscription-" + id + ".pdf")
                    .build());
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la generation de l'attestation de reinscription", e);
        }
    }

    // =============================
    //  Accuse de reception du dossier
    // =============================

    // Via gateway: GET /documents/inscriptions/{id}/accuse-reception.pdf
    @GetMapping("/inscriptions/{id}/accuse-reception.pdf")
    public ResponseEntity<byte[]> generateAccuseReception(@PathVariable String id) {
        InscriptionDto ins = inscriptionClient.getById(id);
        if (ins == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        try {
            byte[] data = htmlTemplatePdfService.generateAccuseReception(ins);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("accuse-reception-inscription-" + id + ".pdf")
                    .build());
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la generation de l'accuse de reception", e);
        }
    }

    // =============================
    //  Reponse de validation / refus
    // =============================

    // Via gateway: GET /documents/inscriptions/{id}/REPONSE-decision.pdf
    @GetMapping("/inscriptions/{id}/REPONSE-decision.pdf")
    public ResponseEntity<byte[]> generateReponseDecision(@PathVariable String id) {
        InscriptionDto ins = inscriptionClient.getById(id);
        if (ins == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (ins.getStatus() != InscriptionStatus.VALIDE && ins.getStatus() != InscriptionStatus.REJETE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La decision doit etre VALIDE ou REJETE pour generer la reponse");
        }

        try {
            byte[] data = htmlTemplatePdfService.generateReponseDecision(ins);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("reponse-decision-inscription-" + id + ".pdf")
                    .build());
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la generation de la reponse a la decision", e);
        }
    }

    // =============================
    //  Rapport recapitulatf du parcours doctoral
    // =============================

    // Via gateway: GET /documents/inscriptions/doctorants/{doctorantId}/rapport-parcours.pdf
    @GetMapping("/inscriptions/doctorants/{doctorantId}/rapport-parcours.pdf")
    public ResponseEntity<byte[]> generateRapportParcours(@PathVariable String doctorantId) {
        List<InscriptionDto> list = inscriptionClient.listByDoctorant(doctorantId);
        if (list == null || list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Aucune inscription trouvee pour ce doctorant");
        }

        try {
            byte[] data = htmlTemplatePdfService.generateRapportParcours(list, doctorantId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("rapport-parcours-" + doctorantId + ".pdf")
                    .build());
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la generation du rapport de parcours doctoral", e);
        }
    }

    // =============================
    //  Tableau de suivi de la documentation recueillie
    // =============================

    // Via gateway: GET /documents/inscriptions/doctorants/{doctorantId}/tableau-suivi.pdf
    @GetMapping("/inscriptions/doctorants/{doctorantId}/tableau-suivi.pdf")
    public ResponseEntity<byte[]> generateTableauSuiviForDoctorant(@PathVariable String doctorantId) {
        List<InscriptionDto> list = inscriptionClient.listByDoctorant(doctorantId);
        if (list == null || list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Aucune inscription trouvee pour ce doctorant");
        }

        try {
            byte[] data = htmlTemplatePdfService.generateTableauSuiviForDoctorant(list);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("tableau-suivi-documentation-" + doctorantId + ".pdf")
                    .build());
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la generation du tableau de suivi de la documentation", e);
        }
    }

    // Via gateway: GET /documents/administratif/tableau-suivi.pdf
    @GetMapping("/administratif/tableau-suivi.pdf")
    public ResponseEntity<byte[]> generateTableauSuivi() {
        try {
            byte[] data = htmlTemplatePdfService.generateTableauSuivi();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("tableau-suivi-documentation.pdf")
                    .build());
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la generation du tableau de suivi de la documentation", e);
        }
    }

    // =============================
    //  PDF builders (PDFBox)
    // =============================

    private byte[] buildAttestationInscriptionPdf(InscriptionDto ins) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 18);
                content.newLineAtOffset(70, 780);
                content.showText("Attestation d'inscription en doctorat");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(70, 740);
                content.showText("Nom et prenom : " + nullSafe(ins.getNom()));
                content.newLineAtOffset(0, -18);
                content.showText("CNE : " + nullSafe(ins.getCne()));
                content.newLineAtOffset(0, -18);
                content.showText("Annee academique : " + nullSafe(ins.getAnneeAcademique()));
                content.newLineAtOffset(0, -18);
                content.showText("Specialite : " + nullSafe(ins.getSpecialite()));
                content.newLineAtOffset(0, -18);
                content.showText("Sujet de these : " + nullSafe(ins.getSujetThese()));
                content.newLineAtOffset(0, -18);
                content.showText("Laboratoire : " + nullSafe(ins.getLaboratoire()));
                content.newLineAtOffset(0, -18);
                if (ins.getDateDemande() != null) {
                    content.showText("Date d'inscription : " + ins.getDateDemande());
                } else {
                    content.showText("Date d'inscription : non renseignee");
                }
                content.endText();
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] buildTableauSuiviPdf() throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                // Titre
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 14);
                content.newLineAtOffset(70, 780);
                content.showText("Modele de tableau de suivi de la documentation recueillie");
                content.endText();

                float marginLeft = 50;
                float marginTop = 730;
                float tableWidth = 500;
                float rowHeight = 20;

                String[] headers = new String[]{
                        "Reference",
                        "Date du document",
                        "Origine du document",
                        "Auteur du document",
                        "Description du document",
                        "Copie ou original conserve",
                        "Date de restitution du document"
                };

                int headerCount = headers.length;
                int rows = 10; // 1 ligne d'entete + 9 lignes vides
                float colWidth = tableWidth / headerCount;

                float startX = marginLeft;
                float startY = marginTop;

                // Dessin de la grille
                content.setLineWidth(0.5f);

                // Lignes horizontales
                for (int i = 0; i <= rows; i++) {
                    float y = startY - i * rowHeight;
                    content.moveTo(startX, y);
                    content.lineTo(startX + tableWidth, y);
                }

                // Lignes verticales
                for (int i = 0; i <= headerCount; i++) {
                    float x = startX + i * colWidth;
                    content.moveTo(x, startY);
                    content.lineTo(x, startY - rows * rowHeight);
                }

                content.stroke();

                // Texte des entetes
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 8);
                float textY = startY - 15;
                for (int i = 0; i < headerCount; i++) {
                    float textX = startX + i * colWidth + 2;
                    content.newLineAtOffset(textX - (i == 0 ? 0 : (startX + (i - 1) * colWidth + 2)),
                            i == 0 ? textY : 0);
                    content.showText(headers[i]);
                }
                content.endText();
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] buildAutorisationPdf(SoutenanceDto s) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 18);
                content.newLineAtOffset(70, 780);
                content.showText("Autorisation de soutenance de these");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(70, 740);
                content.showText("Doctorant : " + nullSafe(s.getDoctorantId()));
                content.newLineAtOffset(0, -18);
                content.showText("Directeur de these : " + nullSafe(s.getDirecteurId()));
                content.newLineAtOffset(0, -18);
                content.showText("Titre de la these : " + nullSafe(s.getTitreTheseDefinitif()));
                content.newLineAtOffset(0, -18);
                content.showText("Statut : " + (s.getStatus() != null ? s.getStatus().name() : ""));
                content.newLineAtOffset(0, -18);
                if (s.getDateAutorisation() != null) {
                    String date = s.getDateAutorisation().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    content.showText("Date d'autorisation : " + date);
                } else {
                    content.showText("Date d'autorisation : non renseignee");
                }
                content.endText();
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] buildProcesVerbalPdf(SoutenanceDto s, List<JuryMemberDto> jury) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 18);
                content.newLineAtOffset(70, 780);
                content.showText("Proces-verbal de soutenance de these");
                content.endText();

                float y = 740;
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(70, y);
                content.showText("Doctorant : " + nullSafe(s.getDoctorantId()));
                content.newLineAtOffset(0, -18);
                content.showText("Directeur de these : " + nullSafe(s.getDirecteurId()));
                content.newLineAtOffset(0, -18);
                content.showText("Titre de la these : " + nullSafe(s.getTitreTheseDefinitif()));
                content.newLineAtOffset(0, -18);
                if (s.getDatePlanifiee() != null) {
                    String date = s.getDatePlanifiee().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    content.showText("Date et heure : " + date);
                } else {
                    content.showText("Date et heure : non renseignees");
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
                    content.showText("Aucun jury defini.");
                } else {
                    for (JuryMemberDto m : jury) {
                        String role = m.getRole() != null ? m.getRole() : "";
                        String line = String.format("%s - %s %s (%s)",
                                role,
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
                content.showText("Decision du jury");
                content.endText();

                y -= 24;
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(70, y);
                content.showText("Decision : ...............................................................");
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

    private byte[] buildAttestationReinscriptionPdf(InscriptionDto ins) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 18);
                content.newLineAtOffset(70, 780);
                content.showText("Attestation de reinscription annuelle en doctorat");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(70, 740);
                content.showText("Nom et prenom : " + nullSafe(ins.getNom()));
                content.newLineAtOffset(0, -18);
                content.showText("CNE : " + nullSafe(ins.getCne()));
                content.newLineAtOffset(0, -18);
                content.showText("Annee academique : " + nullSafe(ins.getAnneeAcademique()));
                content.newLineAtOffset(0, -18);
                content.showText("Specialite : " + nullSafe(ins.getSpecialite()));
                content.newLineAtOffset(0, -18);
                content.showText("Sujet de these : " + nullSafe(ins.getSujetThese()));
                content.newLineAtOffset(0, -18);
                content.showText("Laboratoire : " + nullSafe(ins.getLaboratoire()));
                content.newLineAtOffset(0, -18);
                if (ins.getDateDemande() != null) {
                    content.showText("Date de reinscription : " + ins.getDateDemande());
                } else {
                    content.showText("Date de reinscription : non renseignee");
                }
                content.endText();
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] buildAccuseReceptionPdf(InscriptionDto ins) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 18);
                content.newLineAtOffset(70, 780);
                content.showText("Accuse de reception du dossier d'inscription");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(70, 740);
                content.showText("Nom et prenom : " + nullSafe(ins.getNom()));
                content.newLineAtOffset(0, -18);
                content.showText("CNE : " + nullSafe(ins.getCne()));
                content.newLineAtOffset(0, -18);
                if (ins.getDateDemande() != null) {
                    content.showText("Date de depot du dossier : " + ins.getDateDemande());
                } else {
                    content.showText("Date de depot du dossier : non renseignee");
                }
                content.newLineAtOffset(0, -18);
                content.showText("Numero de dossier : " + nullSafe(ins.getId()));
                content.endText();
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] buildReponseDecisionPdf(InscriptionDto ins) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            String decisionTexte = ins.getStatus() == InscriptionStatus.VALIDE
                    ? "Suite a votre demande d'inscription, nous vous informons qu'elle a ete VALIDEE."
                    : "Suite a votre demande d'inscription, nous vous informons qu'elle a ete REJETEE.";

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 18);
                content.newLineAtOffset(70, 780);
                content.showText("Reponse a la decision sur la demande d'inscription");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(70, 740);
                content.showText("Nom et prenom : " + nullSafe(ins.getNom()));
                content.newLineAtOffset(0, -18);
                content.showText("CNE : " + nullSafe(ins.getCne()));
                content.newLineAtOffset(0, -18);
                content.showText(decisionTexte);
                content.newLineAtOffset(0, -18);
                if (ins.getStatus() == InscriptionStatus.REJETE) {
                    content.showText("Motif du rejet : " + nullSafe(ins.getMotifRejet()));
                }
                content.endText();
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] buildRapportParcoursPdf(List<InscriptionDto> list, String doctorantId) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 18);
                content.newLineAtOffset(70, 780);
                content.showText("Rapport recapitulatf du parcours doctoral");
                content.endText();

                float y = 740;
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(70, y);
                content.showText("Doctorant ID : " + nullSafe(doctorantId));
                content.newLineAtOffset(0, -22);
                content.showText("Nombre d'inscriptions : " + list.size());
                content.newLineAtOffset(0, -22);

                for (InscriptionDto ins : list) {
                    String ligne = String.format("- %s | %s | %s | annee %s",
                            ins.getType() != null ? ins.getType().name() : "?",
                            ins.getStatus() != null ? ins.getStatus().name() : "?",
                            ins.getDateDemande() != null ? ins.getDateDemande().toString() : "date ?",
                            nullSafe(ins.getAnneeAcademique()));
                    content.showText(ligne);
                    content.newLineAtOffset(0, -18);
                }
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
