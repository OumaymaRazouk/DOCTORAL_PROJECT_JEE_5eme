package com.exemple.soutenanceservice.repository;

import com.exemple.soutenanceservice.model.PrerequisSoutenanceConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrerequisSoutenanceConfigRepository extends JpaRepository<PrerequisSoutenanceConfig, String> {

    Optional<PrerequisSoutenanceConfig> findByDoctorantId(String doctorantId);
}
