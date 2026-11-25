package com.example.taskmanager.services;

import com.example.taskmanager.dto.RegistrationRequestDTO;

public interface UserService {
    void createUser(RegistrationRequestDTO request);
}
