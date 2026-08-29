package com.noteshare.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountVisibilityRequest(
        @NotBlank(message = "Visibility is required") String visibility
) {}
