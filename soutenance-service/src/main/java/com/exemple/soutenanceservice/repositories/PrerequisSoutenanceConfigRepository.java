package com.exemple.soutenanceservice.repositories;

import com.exemple.soutenanceservice.model.PrerequisSoutenanceConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrerequisSoutenanceConfigRepository {
    Optional<PrerequisSoutenanceConfig> findByDoctorantId(String doctorantId);
}
