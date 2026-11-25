package com.example.taskmanager.services;

import com.example.taskmanager.dto.RegistrationRequestDTO;
import com.example.taskmanager.models.Role;
import com.example.taskmanager.models.User;
import com.example.taskmanager.repositories.RoleRepository;
import com.example.taskmanager.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    public void createUser(RegistrationRequestDTO request) {
        User newUser = new User();
        newUser.setName(request.getUserName());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));

        if (!Objects.isNull(request.getRoleName())) {
            Role role = roleRepository.findByName(request.getRoleName())
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            newUser.getRoles().add(role);
        }

        userRepository.save(newUser);
    }

}
