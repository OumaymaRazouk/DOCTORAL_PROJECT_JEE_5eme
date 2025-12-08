package com.exemple.soutenanceservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class SoutenanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoutenanceServiceApplication.class, args);
    }
}
