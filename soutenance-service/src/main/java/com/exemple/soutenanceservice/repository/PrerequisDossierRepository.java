package com.exemple.soutenanceservice.repository;

import com.exemple.soutenanceservice.model.PrerequisDossier;
import com.exemple.soutenanceservice.model.PrerequisDossierEtat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrerequisDossierRepository extends JpaRepository<PrerequisDossier, String> {

    List<PrerequisDossier> findAllByEtatScientifiqueOrderByUpdatedAtDesc(PrerequisDossierEtat etatScientifique);

    List<PrerequisDossier> findAllByEtatScientifiqueAndEtatAdministratifOrderByUpdatedAtDesc(
            PrerequisDossierEtat etatScientifique,
            PrerequisDossierEtat etatAdministratif
    );
}
