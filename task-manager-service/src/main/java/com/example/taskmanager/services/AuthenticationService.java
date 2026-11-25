package com.example.taskmanager.services;

import com.example.taskmanager.dto.LoginRequestDTO;

public interface AuthenticationService {
    String signIn(LoginRequestDTO request);
}
