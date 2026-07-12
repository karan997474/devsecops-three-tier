package com.example.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock private TaskRepository repository;
    @InjectMocks private TaskService service;

    @Test
    void trimsTitleBeforeSaving() {
        when(repository.save(any(Task.class))).thenAnswer(call -> call.getArgument(0));

        Task created = service.create(new TaskRequest("  Secure build  "));

        assertEquals("Secure build", created.getTitle());
    }
}

