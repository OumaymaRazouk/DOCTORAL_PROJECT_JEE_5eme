package com.exemple.inscriptionservice.clients;

import com.exemple.inscriptionservice.dto.EmailNotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", url = "${app.notification-service.url:http://localhost:8083}")
public interface NotificationClient {

    @PostMapping("/api/notifications/email")
    ResponseEntity<Void> sendEmail(@RequestBody EmailNotificationRequest request);
}
