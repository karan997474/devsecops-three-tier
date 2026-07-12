package com.example.tasks;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskService {
    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public List<Task> list() {
        return repository.findAll();
    }

    public Task create(TaskRequest request) {
        return repository.save(new Task(request.title().trim()));
    }

    public Task complete(String id) {
        Task task = repository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        task.setCompleted(true);
        return repository.save(task);
    }
}

