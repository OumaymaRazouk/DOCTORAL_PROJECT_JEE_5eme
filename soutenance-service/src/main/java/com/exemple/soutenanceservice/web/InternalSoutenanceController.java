package com.exemple.soutenanceservice.web;

import com.exemple.soutenanceservice.dto.JuryMemberDto;
import com.exemple.soutenanceservice.dto.SoutenanceDto;
import com.exemple.soutenanceservice.model.JuryMember;
import com.exemple.soutenanceservice.model.Soutenance;
import com.exemple.soutenanceservice.repository.JuryMemberRepository;
import com.exemple.soutenanceservice.repository.SoutenanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/soutenances")
@RequiredArgsConstructor
public class InternalSoutenanceController {

    private final SoutenanceRepository soutenanceRepository;
    private final JuryMemberRepository juryMemberRepository;

    @GetMapping("/{id}")
    public ResponseEntity<SoutenanceDto> getById(@PathVariable String id) {
        Soutenance s = soutenanceRepository.findById(id).orElse(null);
        if (s == null) {
            return ResponseEntity.notFound().build();
        }
        SoutenanceDto dto = new SoutenanceDto(
                s.getId(),
                s.getDoctorantId(),
                s.getDirecteurId(),
                s.getStatus(),
                s.getTitreTheseDefinitif(),
                s.getDateAutorisation(),
                s.getDatePlanifiee(),
                s.getSalle(),
                s.getLieu()
        );
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/jury")
    public ResponseEntity<List<JuryMemberDto>> getJury(@PathVariable String id) {
        List<JuryMember> members = juryMemberRepository.findAllBySoutenance_Id(id);
        List<JuryMemberDto> dtos = members.stream()
                .map(m -> new JuryMemberDto(
                        m.getNom(),
                        m.getPrenom(),
                        m.getAffiliation(),
                        m.getEmail(),
                        m.getTelephone(),
                        m.getRole()
                ))
                .toList();
        return ResponseEntity.ok(dtos);
    }
}
