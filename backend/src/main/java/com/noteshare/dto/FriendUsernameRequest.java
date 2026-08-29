package com.noteshare.dto;

import jakarta.validation.constraints.NotBlank;

public record FriendUsernameRequest(
        @NotBlank(message = "Username is required") String username
) {}
