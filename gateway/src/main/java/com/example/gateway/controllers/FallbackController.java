package com.example.gateway.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/task-manager")
    public ResponseEntity<String> taskManagerFallback() {
        System.out.println("Fallback task-manager");
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Task Service unavailable. Please try later.");
    }

}
