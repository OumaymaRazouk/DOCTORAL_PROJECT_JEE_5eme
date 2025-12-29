package com.exemple.notificationservice.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "utilisateurs-service", path = "/users")
public interface UserClient {

    @GetMapping("/{id}")
    Map<String, Object> getById(@PathVariable("id") Long id);
}
