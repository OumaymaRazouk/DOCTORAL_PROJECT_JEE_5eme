package com.exemple.inscriptionservice.dto;

import lombok.Data;

@Data
public class UserResponseWrapper {
    private boolean success;
    private String message;
    private UserResponse data;
}
