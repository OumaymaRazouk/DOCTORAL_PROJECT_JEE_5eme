package com.exemple.inscriptionservice.controllers;

import com.exemple.inscriptionservice.entities.Campaign;
import com.exemple.inscriptionservice.enums.CampaignType;
import com.exemple.inscriptionservice.repositories.CampaignRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/campaigns")
public class CampaignController {

    private final CampaignRepository repo;

    public CampaignController(CampaignRepository repo) { this.repo = repo; }

    @PreAuthorize("hasAnyRole('PERSONNEL_ADMINISTRATIF','SUPERADMIN')")
    @PostMapping
    public ResponseEntity<Campaign> open(@RequestParam CampaignType type,
                                         @RequestParam String startDate,
                                         @RequestParam String endDate) {
        var c = Campaign.builder()
                .type(type)
                .startDate(LocalDate.parse(startDate))
                .endDate(LocalDate.parse(endDate))
                .active(true)
                .build();
        return ResponseEntity.ok(repo.save(c));
    }

    @PreAuthorize("hasAnyRole('PERSONNEL_ADMINISTRATIF','SUPERADMIN')")
    @PostMapping("/{id}/close")
    public ResponseEntity<Campaign> close(@PathVariable String id) {
        var c = repo.findById(id).orElseThrow();
        c.setActive(false);
        return ResponseEntity.ok(repo.save(c));
    }

    @GetMapping("/active")
    public ResponseEntity<Campaign> active(@RequestParam CampaignType type) {
        return repo.findFirstByTypeAndActiveIsTrue(type)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
