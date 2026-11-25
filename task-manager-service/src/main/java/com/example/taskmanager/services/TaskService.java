package com.example.taskmanager.services;

import com.example.taskmanager.dto.ListTaskFilterDTO;
import com.example.taskmanager.dto.TaskDTO;
import com.example.taskmanager.dto.TaskFilteredDTO;
import org.springframework.data.domain.Pageable;

public interface TaskService {
    TaskDTO createTask(TaskDTO taskDTO);

    TaskDTO getTaskById(Long id);

    TaskFilteredDTO getAllTasks(ListTaskFilterDTO filters, Pageable pageable);

    TaskDTO updateTask(Long id, TaskDTO updatedTask);

    void deleteTaskById(Long id);
}
