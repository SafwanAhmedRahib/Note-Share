package com.noteshare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 120, message = "Title must be 120 characters or fewer") String title,
        @NotBlank(message = "Content is required") String content,
        // "PUBLIC" or "PRIVATE". Optional - null on update means "leave visibility unchanged",
        // null on create defaults to PRIVATE.
        String visibility
) {}
