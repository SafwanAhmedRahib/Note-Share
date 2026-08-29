package com.noteshare.dto;

import jakarta.validation.constraints.NotBlank;

public record ShareRequest(
        @NotBlank(message = "Username is required") String username
) {}
