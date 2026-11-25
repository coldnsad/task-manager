package com.example.taskmanager.dto;

public record ListTaskFilterDTO(
        String title,
        String description,
        Boolean completed
) {
}
