package com.noteshare.dto;

import java.time.LocalDateTime;

public record NoteResponse(
        Long id,
        String title,
        String content,
        Long ownerId,
        String ownerUsername,
        boolean owner,
        String visibility,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
