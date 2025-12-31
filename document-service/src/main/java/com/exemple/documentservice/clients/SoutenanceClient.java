package com.exemple.documentservice.clients;

import com.exemple.documentservice.dto.JuryMemberDto;
import com.exemple.documentservice.dto.SoutenanceDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "soutenance-service", path = "/internal/soutenances")
public interface SoutenanceClient {

    @GetMapping("/{id}")
    SoutenanceDto getById(@PathVariable("id") String id);

    @GetMapping("/{id}/jury")
    List<JuryMemberDto> getJury(@PathVariable("id") String id);
}
