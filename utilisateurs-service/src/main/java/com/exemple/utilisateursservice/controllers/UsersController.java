package com.exemple.utilisateursservice.controllers;

import com.exemple.utilisateursservice.entities.User;
import com.exemple.utilisateursservice.model.Role;
import com.exemple.utilisateursservice.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UsersController {

    private final UserRepository users;

    public UsersController(UserRepository users) { this.users = users; }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        String email = auth.getName();
        User u = users.findByEmail(email).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of(
                "id", String.valueOf(u.getId()),
                "email", u.getEmail(),
                "firstName", u.getFirstName(),
                "lastName", u.getLastName(),
                "roles", u.getRoles()
        ));
    }

    @GetMapping("/encadrants")
    public ResponseEntity<List<User>> listEncadrants() {
        var encadrants = users.findAllByRole(Role.ENCADRANT);
        return ResponseEntity.ok(encadrants);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @GetMapping("/admins")
    public ResponseEntity<List<User>> listAdmins() {
        var admins = users.findAllByRole(Role.ADMIN);
        var supers = users.findAllByRole(Role.SUPER_ADMIN);
        admins.addAll(supers);
        return ResponseEntity.ok(admins);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        var u = users.findById(id).orElse(null);
        if (u == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "User not found",
                    "data", null
            ));
        }
        var dto = Map.of(
                "id", String.valueOf(u.getId()),
                "email", u.getEmail(),
                "firstName", u.getFirstName(),
                "lastName", u.getLastName(),
                "userRole", String.join(",", u.getRoles().stream().map(Enum::name).toList())
        );
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OK",
                "data", dto
        ));
    }
}

