package com.exemple.utilisateursservice.repositories;

import com.exemple.utilisateursservice.entities.User;
import com.exemple.utilisateursservice.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("select u from User u join u.roles r where r = :role")
    List<User> findAllByRole(@Param("role") Role role);

    Optional<User> findByActivationToken(String activationToken);
}
