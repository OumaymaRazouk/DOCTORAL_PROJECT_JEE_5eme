package com.exemple.soutenanceservice.service;

import lombok.Data;

import java.util.List;

@Data
public class SoutenanceDraftInput {
    private String doctorantId;
    private String directeurId;
    private String titreTheseDefinitif;
    private String resumeFinal;
    private List<String> motsCles;
    private String manuscritPath;
    private String rapportThesePath;
    private String rapportAntiPlagiatPath;
    private String preuvesPublicationsPath;
    private String preuvesCommunicationsPath;
    private String attestationsFormationsPath;
    private String demandeManuscritePath;
}
