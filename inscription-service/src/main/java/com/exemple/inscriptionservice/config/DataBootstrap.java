package com.exemple.inscriptionservice.config;

import com.exemple.inscriptionservice.entities.Campaign;
import com.exemple.inscriptionservice.enums.CampaignType;
import com.exemple.inscriptionservice.repositories.CampaignRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
public class DataBootstrap {

    @Bean
    CommandLineRunner seedCampaigns(CampaignRepository repo) {
        return args -> {
            // Ensure there is an active INSCRIPTION campaign for development
            if (repo.findFirstByTypeAndActiveIsTrue(CampaignType.INSCRIPTION).isEmpty()) {
                repo.save(Campaign.builder()
                        .type(CampaignType.INSCRIPTION)
                        .startDate(LocalDate.now().minusDays(1))
                        .endDate(LocalDate.now().plusMonths(6))
                        .active(true)
                        .build());
            }
            // Ensure there is an active REINSCRIPTION campaign as well (optional)
            if (repo.findFirstByTypeAndActiveIsTrue(CampaignType.REINSCRIPTION).isEmpty()) {
                repo.save(Campaign.builder()
                        .type(CampaignType.REINSCRIPTION)
                        .startDate(LocalDate.now().minusDays(1))
                        .endDate(LocalDate.now().plusMonths(6))
                        .active(true)
                        .build());
            }
        };
    }
}
