package com.exemple.inscriptionservice.entities;

import com.exemple.inscriptionservice.enums.CampaignType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "campaigns")
public class Campaign {
    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private CampaignType type; // INSCRIPTION or REINSCRIPTION

    private LocalDate startDate;
    private LocalDate endDate;

    private boolean active;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }
}
