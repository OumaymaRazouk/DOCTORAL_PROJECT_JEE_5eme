package com.exemple.utilisateursservice.config;

import com.exemple.utilisateursservice.entities.User;
import com.exemple.utilisateursservice.model.Role;
import com.exemple.utilisateursservice.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedSuperAdmin(UserRepository users,
                                     PasswordEncoder encoder,
                                     @Value("${seed.superadmin.email}") String email,
                                     @Value("${seed.superadmin.password}") String password) {
        return args -> {
            if (!users.existsByEmail(email)) {
                var u = User.builder()
                        .firstName("Super")
                        .lastName("Admin")
                        .email(email)
                        .password(encoder.encode(password))
                        .roles(Set.of(Role.SUPER_ADMIN))
                        .enabled(true)
                        .build();
                users.save(u);
            }
        };
    }
}
