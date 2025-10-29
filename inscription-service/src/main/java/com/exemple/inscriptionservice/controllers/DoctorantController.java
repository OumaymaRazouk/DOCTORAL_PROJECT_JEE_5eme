package com.exemple.inscriptionservice.controllers;


import com.exemple.inscriptionservice.entities.Doctorant;
import com.exemple.inscriptionservice.repositories.DoctorantRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctorants")
public class DoctorantController {

    private final DoctorantRepository doctorantRepository;

    public DoctorantController(DoctorantRepository doctorantRepository) {
        this.doctorantRepository = doctorantRepository;
    }

    @GetMapping
    public List<Doctorant> getAllDoctorants() {
        return doctorantRepository.findAll();
    }

    @PostMapping
    public Doctorant addDoctorant(@RequestBody Doctorant d) {
        return doctorantRepository.save(d);
    }

    @GetMapping("/{id}")
    public Doctorant getDoctorant(@PathVariable Long id) {
        return doctorantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctorant introuvable"));
    }

    @DeleteMapping("/{id}")
    public void deleteDoctorant(@PathVariable Long id) {
        doctorantRepository.deleteById(id);
    }
}
