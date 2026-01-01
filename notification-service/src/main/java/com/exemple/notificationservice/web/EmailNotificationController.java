package com.exemple.notificationservice.web;

import com.exemple.notificationservice.dto.EmailNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class EmailNotificationController {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationController.class);

    private final JavaMailSender mailSender;

    @Value("${app.notifications.email.from:chtiouimariam745@gmail.com}")
    private String defaultFrom;

    @Value("${app.notifications.email.to.fallback:oumaymarazouk195@gmail.com}")
    private String defaultTo;

    public EmailNotificationController(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostMapping("/email")
    public ResponseEntity<Void> sendEmail(@RequestBody EmailNotificationRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }

        String to = request.to();
        if (to == null || to.isBlank()) {
            to = defaultTo;
        }
        if (to == null || to.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String subject = request.subject();
        if (subject == null || subject.isBlank()) {
            subject = "Notification";
        }

        String body = request.body();
        if (body == null) {
            body = "";
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (defaultFrom != null && !defaultFrom.isBlank()) {
                message.setFrom(defaultFrom);
            }
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.warn("Echec envoi email notification a {}: {}", to, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
