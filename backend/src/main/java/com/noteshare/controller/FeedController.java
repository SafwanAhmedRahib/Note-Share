package com.noteshare.controller;

import com.noteshare.dto.NoteResponse;
import com.noteshare.security.AuthContext;
import com.noteshare.service.FeedService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FeedController {
    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping("/feed")
    public List<NoteResponse> feed(HttpServletRequest request) {
        return feedService.feedFor(AuthContext.requireUserId(request));
    }
}
