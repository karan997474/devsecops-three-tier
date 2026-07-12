package com.example.tasks;

import jakarta.validation.constraints.NotBlank;

public record TaskRequest(@NotBlank(message = "title is required") String title) { }

