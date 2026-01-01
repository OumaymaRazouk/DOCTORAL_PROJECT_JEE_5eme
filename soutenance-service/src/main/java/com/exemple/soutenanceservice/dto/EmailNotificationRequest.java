package com.exemple.soutenanceservice.dto;

public record EmailNotificationRequest(
        String to,
        String subject,
        String body
) {
}
