package com.noteshare.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {
    @GetMapping("/")
    public Map<String, String> status() {
        return Map.of("status", "ok", "service", "noteshare-backend");
    }
}
