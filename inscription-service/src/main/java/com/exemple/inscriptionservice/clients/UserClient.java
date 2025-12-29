package com.exemple.inscriptionservice.clients;

import com.exemple.inscriptionservice.config.FeignAuthConfig;
import com.exemple.inscriptionservice.dto.UserResponseWrapper;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "utilisateurs-service", path = "/users", configuration = FeignAuthConfig.class)
public interface UserClient {
    @GetMapping("/{id}")
    UserResponseWrapper getUserById(@PathVariable("id") String id);

    @PostMapping("/{id}/grant-doctorant")
    void grantDoctorantRole(@PathVariable("id") String id);
}
