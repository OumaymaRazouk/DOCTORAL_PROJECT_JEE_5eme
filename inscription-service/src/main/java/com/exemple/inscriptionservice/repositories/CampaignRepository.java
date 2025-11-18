package com.exemple.inscriptionservice.repositories;

import com.exemple.inscriptionservice.entities.Campaign;
import com.exemple.inscriptionservice.enums.CampaignType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CampaignRepository extends JpaRepository<Campaign, String> {
    Optional<Campaign> findFirstByTypeAndActiveIsTrue(CampaignType type);
}
