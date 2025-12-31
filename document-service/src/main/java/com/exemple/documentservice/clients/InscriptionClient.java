package com.exemple.documentservice.clients;

import com.exemple.documentservice.dto.InscriptionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "inscription-service", path = "/inscriptions")
public interface InscriptionClient {

    @GetMapping("/{id}")
    InscriptionDto getById(@PathVariable("id") String id);

    @GetMapping("/doctorants/{doctorantId}")
    List<InscriptionDto> listByDoctorant(@PathVariable("doctorantId") String doctorantId);
}
