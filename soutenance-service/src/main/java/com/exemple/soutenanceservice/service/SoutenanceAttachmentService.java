package com.exemple.soutenanceservice.service;

import com.exemple.soutenanceservice.model.Soutenance;
import com.exemple.soutenanceservice.repository.SoutenanceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class SoutenanceAttachmentService {

    private final SoutenanceRepository soutenanceRepository;

    @Value("${storage.base-path:#{systemProperties['user.home']}/doctoral-uploads}")
    private String storageBasePath;

    public SoutenanceAttachmentService(SoutenanceRepository soutenanceRepository) {
        this.soutenanceRepository = soutenanceRepository;
    }

    public Soutenance saveAttachments(String id,
                                      MultipartFile manuscrit,
                                      MultipartFile rapportAntiPlagiat,
                                      MultipartFile attestationsFormations,
                                      MultipartFile demandeManuscrite) {
        Soutenance s = soutenanceRepository.findById(id).orElseThrow();
        Path dir = getSoutenanceDir(id);
        try {
            Files.createDirectories(dir);
            String manuscritPath = saveOneAttachment(dir, manuscrit, "manuscrit");
            if (manuscritPath != null) {
                s.setManuscritPath(manuscritPath);
            }
            String rapportPath = saveOneAttachment(dir, rapportAntiPlagiat, "rapport_anti_plagiat");
            if (rapportPath != null) {
                s.setRapportAntiPlagiatPath(rapportPath);
            }
            String attestationsPath = saveOneAttachment(dir, attestationsFormations, "attestations_formations");
            if (attestationsPath != null) {
                s.setAttestationsFormationsPath(attestationsPath);
            }
            String demandePath = saveOneAttachment(dir, demandeManuscrite, "demande_manuscrite");
            if (demandePath != null) {
                s.setDemandeManuscritePath(demandePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Echec du televersement des documents de soutenance", e);
        }
        return soutenanceRepository.save(s);
    }

    public List<String> listAttachments(String id) {
        Path dir = getSoutenanceDir(id);
        try {
            if (!Files.exists(dir)) {
                return List.of();
            }
            try (var s = Files.list(dir)) {
                return s
                        .filter(Files::isRegularFile)
                        .map(p -> p.getFileName().toString())
                        .toList();
            }
        } catch (IOException e) {
            return List.of();
        }
    }

    public Resource loadAttachment(String id, String fileName) {
        Path dir = getSoutenanceDir(id);
        Path target = dir.resolve(fileName);
        if (!Files.exists(target)) {
            return null;
        }
        return new FileSystemResource(target.toFile());
    }

    private Path getSoutenanceDir(String id) {
        Path base = Paths.get(storageBasePath);
        return base.resolve(Paths.get("soutenances", id));
    }

    private String saveOneAttachment(Path dir, MultipartFile file, String prefix) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String original = file.getOriginalFilename();
        String safeName = original == null ? (prefix + "_" + System.currentTimeMillis()) : original.replaceAll("[^a-zA-Z0-9._-]", "_");
        int maxLen = 100;
        if (safeName.length() > maxLen) {
            int dot = safeName.lastIndexOf('.') > 0 ? safeName.lastIndexOf('.') : safeName.length();
            String base = safeName.substring(0, Math.min(dot, maxLen - 10));
            String ext = dot < safeName.length() ? safeName.substring(dot) : "";
            safeName = base + ext;
        }
        Path target = dir.resolve(prefix + "_" + System.currentTimeMillis() + "_" + safeName);
        Files.createDirectories(target.getParent());
        file.transferTo(target.toFile());
        return target.getFileName().toString();
    }
}
