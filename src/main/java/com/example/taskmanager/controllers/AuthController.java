package com.example.taskmanager.controllers;

import com.example.taskmanager.dto.LoginRequestDTO;
import com.example.taskmanager.dto.RegistrationRequestDTO;
import com.example.taskmanager.services.AuthenticationService;
import com.example.taskmanager.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationService authenticationService;

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody RegistrationRequestDTO request) {
        userService.createUser(request);
        return ResponseEntity.ok("User registered");
    }

    @GetMapping("/signin")
    public ResponseEntity<String> signIn(@RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authenticationService.signIn(request));
    }
}
