package com.exemple.inscriptionservice.repositories;

import com.exemple.inscriptionservice.entities.Inscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InscriptionRepository extends JpaRepository<Inscription, String> {
    List<Inscription> findAllByDoctorantIdOrderByCreatedAtDesc(String doctorantId);
    boolean existsByDoctorantIdAndAnneeAcademiqueAndType(String doctorantId, String anneeAcademique, com.exemple.inscriptionservice.enums.InscriptionType type);
    Optional<Inscription> findTopByDoctorantIdOrderByCreatedAtDesc(String doctorantId);
    Optional<Inscription> findTopByDoctorantIdAndAnneeAcademiqueAndTypeOrderByCreatedAtDesc(
            String doctorantId,
            String anneeAcademique,
            com.exemple.inscriptionservice.enums.InscriptionType type);
    Optional<Inscription> findTopByDoctorantIdAndTypeOrderByCreatedAtAsc(String doctorantId, com.exemple.inscriptionservice.enums.InscriptionType type);
    List<Inscription> findAllByDirecteurIdOrderByCreatedAtDesc(String directeurId);
    List<Inscription> findAllByDirecteurIdAndStatusOrderByCreatedAtDesc(String directeurId, com.exemple.inscriptionservice.enums.InscriptionStatus status);
    List<Inscription> findAllByStatusOrderByCreatedAtDesc(com.exemple.inscriptionservice.enums.InscriptionStatus status);
    long countByDoctorantIdAndTypeAndStatus(String doctorantId, com.exemple.inscriptionservice.enums.InscriptionType type, com.exemple.inscriptionservice.enums.InscriptionStatus status);
    List<Inscription> findAllByOrderByCreatedAtDesc();
}
