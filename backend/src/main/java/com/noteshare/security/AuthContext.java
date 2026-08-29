package com.noteshare.security;

import com.noteshare.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;

public class AuthContext {
    private AuthContext() {}

    public static Long requireUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("Not authenticated");
        }
        return (Long) userId;
    }
}
