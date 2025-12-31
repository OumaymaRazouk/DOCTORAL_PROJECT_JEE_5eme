package com.exemple.documentservice.service;

import com.exemple.documentservice.dto.InscriptionDto;
import com.exemple.documentservice.dto.InscriptionStatus;
import com.exemple.documentservice.dto.InscriptionType;
import com.exemple.documentservice.dto.JuryMemberDto;
import com.exemple.documentservice.dto.SoutenanceDto;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class HtmlTemplatePdfService {

    public byte[] generateAttestationInscription(InscriptionDto ins) throws Exception {
        String template = loadTemplate("templates/attestation-inscription.html");

        String fullName = nullSafe(ins.getNom());
        String cne = nullSafe(ins.getCne());
        String annee = nullSafe(ins.getAnneeAcademique());
        String specialite = nullSafe(ins.getSpecialite());
        String sujet = nullSafe(ins.getSujetThese());
        String laboratoire = nullSafe(ins.getLaboratoire());
        String dateInscription = ins.getDateDemande() != null ? ins.getDateDemande().toString() : "";

        String html = template
                .replace("${NOM_COMPLET}", fullName)
                .replace("${CNE}", cne)
                .replace("${ANNEE_ACADEMIQUE}", annee)
                .replace("${SPECIALITE}", specialite)
                .replace("${SUJET_THESE}", sujet)
                .replace("${LABORATOIRE}", laboratoire)
                .replace("${DATE_INSCRIPTION}", dateInscription);

        return renderHtmlToPdf(html);
    }

    public byte[] generateAccuseReception(InscriptionDto ins) throws Exception {
        String template = loadTemplate("templates/accuse-reception.html");

        String fullName = nullSafe(ins.getNom());
        String cne = nullSafe(ins.getCne());
        String dateDepot = ins.getDateDemande() != null ? ins.getDateDemande().toString() : "";
        String numDossier = nullSafe(ins.getId());

        String html = template
                .replace("${NOM_COMPLET}", fullName)
                .replace("${CNE}", cne)
                .replace("${DATE_DEPOT}", dateDepot)
                .replace("${NUM_DOSSIER}", numDossier);

        return renderHtmlToPdf(html);
    }

    public byte[] generateReponseDecision(InscriptionDto ins) throws Exception {
        String template = loadTemplate("templates/reponse-decision.html");

        String fullName = nullSafe(ins.getNom());
        String cne = nullSafe(ins.getCne());

        String decisionTexte;
        if (ins.getStatus() == InscriptionStatus.VALIDE) {
            decisionTexte = "Suite a l'etude de votre dossier d'inscription, nous vous informons qu'il a ete VALIDE.";
        } else {
            String motif = nullSafe(ins.getMotifRejet());
            decisionTexte = "Suite a l'etude de votre dossier d'inscription, nous vous informons qu'il a ete REJETE." +
                    (motif.isEmpty() ? "" : " Motif du rejet : " + motif);
        }

        String html = template
                .replace("${NOM_COMPLET}", fullName)
                .replace("${CNE}", cne)
                .replace("${DECISION_TEXTE}", decisionTexte);

        return renderHtmlToPdf(html);
    }

    public byte[] generateAutorisationSoutenance(SoutenanceDto s) throws Exception {
        String template = loadTemplate("templates/autorisation-soutenance.html");

        String doctorant = nullSafe(s.getDoctorantId());
        String directeur = nullSafe(s.getDirecteurId());
        String titre = nullSafe(s.getTitreTheseDefinitif());
        String salle = nullSafe(s.getSalle());
        String lieu = nullSafe(s.getLieu());
        String dateAutorisation = s.getDateAutorisation() != null ? s.getDateAutorisation().toString() : "";

        String html = template
                .replace("${DOCTORANT}", doctorant)
                .replace("${DIRECTEUR}", directeur)
                .replace("${TITRE_THESE}", titre)
                .replace("${SALLE}", salle)
                .replace("${LIEU}", lieu)
                .replace("${DATE_AUTORISATION}", dateAutorisation);

        return renderHtmlToPdf(html);
    }

    public byte[] generateAttestationReinscription(InscriptionDto ins) throws Exception {
        String template = loadTemplate("templates/attestation-reinscription.html");

        String fullName = nullSafe(ins.getNom());
        String cne = nullSafe(ins.getCne());
        String annee = nullSafe(ins.getAnneeAcademique());
        String specialite = nullSafe(ins.getSpecialite());
        String sujet = nullSafe(ins.getSujetThese());
        String laboratoire = nullSafe(ins.getLaboratoire());
        String dateReinscription = ins.getDateDemande() != null ? ins.getDateDemande().toString() : "";

        String html = template
                .replace("${NOM_COMPLET}", fullName)
                .replace("${CNE}", cne)
                .replace("${ANNEE_ACADEMIQUE}", annee)
                .replace("${SPECIALITE}", specialite)
                .replace("${SUJET_THESE}", sujet)
                .replace("${LABORATOIRE}", laboratoire)
                .replace("${DATE_REINSCRIPTION}", dateReinscription);

        return renderHtmlToPdf(html);
    }

    public byte[] generateRapportParcours(List<InscriptionDto> list, String doctorantId) throws Exception {
        String template = loadTemplate("templates/rapport-parcours.html");

        StringBuilder rows = new StringBuilder();
        for (InscriptionDto ins : list) {
            String type = ins.getType() != null ? ins.getType().name() : "";
            String status = ins.getStatus() != null ? ins.getStatus().name() : "";
            String date = ins.getDateDemande() != null ? ins.getDateDemande().toString() : "";
            String annee = nullSafe(ins.getAnneeAcademique());
            rows.append("<tr>")
                    .append("<td>").append(type).append("</td>")
                    .append("<td>").append(status).append("</td>")
                    .append("<td>").append(date).append("</td>")
                    .append("<td>").append(annee).append("</td>")
                    .append("</tr>");
        }

        String html = template
                .replace("${DOCTORANT_ID}", nullSafe(doctorantId))
                .replace("${LIGNES}", rows.toString());

        return renderHtmlToPdf(html);
    }

    public byte[] generateTableauSuivi() throws Exception {
        String template = loadTemplate("templates/tableau-suivi.html");

        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            rows.append("<tr>");
            for (int c = 0; c < 12; c++) {
                rows.append("<td></td>");
            }
            rows.append("</tr>");
        }

        String html = template.replace("${LIGNES}", rows.toString());
        return renderHtmlToPdf(html);
    }

    private String mapInscriptionEtape(InscriptionStatus status) {
        if (status == null) return "";
        switch (status) {
            case BROUILLON:
                return "Brouillon";
            case EN_ATTENTE_DIRECTEUR:
                return "En attente du directeur";
            case EN_ATTENTE_ADMIN:
            case EN_ATTENTE_VALIDATION:
                return "En cours de validation";
            case VALIDE:
                return "Validé";
            case REJETE:
                return "Rejeté";
            default:
                return status.name();
        }
    }

    private String mapInscriptionStatutDocument(InscriptionStatus status) {
        if (status == null) return "";
        switch (status) {
            case BROUILLON:
                return "À déposer";
            case EN_ATTENTE_DIRECTEUR:
            case EN_ATTENTE_ADMIN:
            case EN_ATTENTE_VALIDATION:
                return "Déposé";
            case VALIDE:
                return "Validé";
            case REJETE:
                return "Refusé";
            default:
                return status.name();
        }
    }

    public byte[] generateTableauSuiviForDoctorant(List<InscriptionDto> list) throws Exception {
        String template = loadTemplate("templates/tableau-suivi.html");

        StringBuilder rows = new StringBuilder();
        if (list != null) {
            for (InscriptionDto ins : list) {
                String nomDoc;
                String typeDoc;

                if (ins.getType() != null) {
                    InscriptionType type = ins.getType();
                    switch (type) {
                        case INSCRIPTION:
                            nomDoc = "Dossier d'inscription";
                            typeDoc = "inscription";
                            break;
                        case REINSCRIPTION:
                            nomDoc = "Dossier de réinscription";
                            typeDoc = "réinscription";
                            break;
                        default:
                            nomDoc = "Dossier d'inscription";
                            typeDoc = type.name();
                            break;
                    }
                } else {
                    nomDoc = "Dossier d'inscription";
                    typeDoc = "";
                }

                InscriptionStatus status = ins.getStatus();
                String etape = mapInscriptionEtape(status);
                String statutDocument = mapInscriptionStatutDocument(status);
                String version = "v1";
                String dateDepot = ins.getDateDemande() != null ? ins.getDateDemande().toString() : "";
                String deposePar = "Doctorant";
                String validePar = (status == InscriptionStatus.VALIDE) ? "Administration" : "";
                String dateValidation = (status == InscriptionStatus.VALIDE || status == InscriptionStatus.REJETE)
                        ? (ins.getUpdatedAt() != null ? ins.getUpdatedAt().toString() : "")
                        : "";
                String commentaire = nullSafe(ins.getMotifRejet());

                String lien;
                if (status == InscriptionStatus.VALIDE && ins.getType() != null) {
                    InscriptionType type = ins.getType();
                    switch (type) {
                        case INSCRIPTION:
                            lien = "/documents/inscriptions/" + nullSafe(ins.getId()) + "/attestation.pdf";
                            break;
                        case REINSCRIPTION:
                            lien = "/documents/inscriptions/" + nullSafe(ins.getId()) + "/attestation-reinscription.pdf";
                            break;
                        default:
                            lien = "/inscriptions/" + nullSafe(ins.getId());
                            break;
                    }
                } else if (status == InscriptionStatus.VALIDE || status == InscriptionStatus.REJETE) {
                    lien = "/documents/inscriptions/" + nullSafe(ins.getId()) + "/REPONSE-decision.pdf";
                } else {
                    lien = "/inscriptions/" + nullSafe(ins.getId());
                }

                String created = ins.getCreatedAt() != null ? ins.getCreatedAt().toString() : "";
                String updated = ins.getUpdatedAt() != null ? ins.getUpdatedAt().toString() : "";
                String historique;
                if (!created.isEmpty() || !updated.isEmpty()) {
                    historique = ("Créé le " + created + (updated.isEmpty() ? "" : ", mis à jour le " + updated)).trim();
                } else {
                    historique = "";
                }

                rows.append("<tr>")
                        .append("<td>").append(nomDoc).append("</td>")
                        .append("<td>").append(typeDoc).append("</td>")
                        .append("<td>").append(etape).append("</td>")
                        .append("<td>").append(statutDocument).append("</td>")
                        .append("<td>").append(version).append("</td>")
                        .append("<td>").append(dateDepot).append("</td>")
                        .append("<td>").append(deposePar).append("</td>")
                        .append("<td>").append(validePar).append("</td>")
                        .append("<td>").append(dateValidation).append("</td>")
                        .append("<td>").append(commentaire).append("</td>")
                        .append("<td>").append(lien).append("</td>")
                        .append("<td>").append(historique).append("</td>")
                        .append("</tr>");
            }
        }

        String html = template.replace("${LIGNES}", rows.toString());
        return renderHtmlToPdf(html);
    }

    public byte[] generateProcesVerbal(SoutenanceDto s, List<JuryMemberDto> jury) throws Exception {
        String template = loadTemplate("templates/proces-verbal-soutenance.html");

        StringBuilder juryRows = new StringBuilder();
        if (jury != null) {
            for (JuryMemberDto m : jury) {
                String role = m.getRole() != null ? m.getRole() : "";
                String nom = nullSafe(m.getNom());
                String prenom = nullSafe(m.getPrenom());
                String aff = nullSafe(m.getAffiliation());
                juryRows.append("<tr>")
                        .append("<td>").append(role).append("</td>")
                        .append("<td>").append(nom).append(" ").append(prenom).append("</td>")
                        .append("<td>").append(aff).append("</td>")
                        .append("</tr>");
            }
        }

        String datePlanifiee = s.getDatePlanifiee() != null ? s.getDatePlanifiee().toString() : "";

        String html = template
                .replace("${DOCTORANT}", nullSafe(s.getDoctorantId()))
                .replace("${DIRECTEUR}", nullSafe(s.getDirecteurId()))
                .replace("${TITRE_THESE}", nullSafe(s.getTitreTheseDefinitif()))
                .replace("${DATE_SOUTENANCE}", datePlanifiee)
                .replace("${SALLE}", nullSafe(s.getSalle()))
                .replace("${LIEU}", nullSafe(s.getLieu()))
                .replace("${JURY_ROWS}", juryRows.toString());

        return renderHtmlToPdf(html);
    }

    private String loadTemplate(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        byte[] bytes = resource.getInputStream().readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private byte[] renderHtmlToPdf(String html) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
