package com.exemple.inscriptionservice.dto;

public record EmailNotificationRequest(
        String to,
        String subject,
        String body
) {
}
