package com.noteshare.controller;

import com.noteshare.model.User;
import com.noteshare.repository.UserRepository;
import com.noteshare.security.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Now requires auth - previously anyone could enumerate all usernames with no token.
    @GetMapping("/users")
    public List<String> listUsernames(HttpServletRequest request) {
        AuthContext.requireUserId(request);
        return userRepository.findAll().stream().map(User::getUsername).toList();
    }
}
