package com.exemple.inscriptionservice.repositories;


import com.exemple.inscriptionservice.entities.Doctorant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorantRepository extends JpaRepository<Doctorant, Long> {
}
