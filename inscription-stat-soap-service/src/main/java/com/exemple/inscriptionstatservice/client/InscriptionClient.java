package com.exemple.inscriptionstatservice.client;

import com.exemple.inscriptionstatservice.model.InscriptionView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "inscription-service", path = "/inscriptions")
public interface InscriptionClient {

    @GetMapping
    List<InscriptionView> listAll();
}
