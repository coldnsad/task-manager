package com.example.taskmanager.dto;

import java.util.List;

public record TaskFilteredDTO(
        List<TaskDTO> taskDTOList,
        int pageNumber,
        int elementsPerPage,
        int countPages,
        long countElements
) {
}
