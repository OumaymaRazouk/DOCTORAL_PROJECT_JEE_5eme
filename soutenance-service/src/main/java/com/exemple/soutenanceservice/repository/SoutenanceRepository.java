package com.exemple.soutenanceservice.repository;

import com.exemple.soutenanceservice.model.Soutenance;
import com.exemple.soutenanceservice.model.SoutenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SoutenanceRepository extends JpaRepository<Soutenance, String> {

    Optional<Soutenance> findTopByDoctorantIdOrderByCreatedAtDesc(String doctorantId);

    List<Soutenance> findAllByDoctorantIdOrderByCreatedAtDesc(String doctorantId);

    List<Soutenance> findAllByDirecteurIdOrderByCreatedAtDesc(String directeurId);

    List<Soutenance> findAllByStatusOrderByCreatedAtDesc(SoutenanceStatus status);
}
