package com.noteshare.controller;

import com.noteshare.dto.FriendRequestsResponse;
import com.noteshare.dto.FriendUsernameRequest;
import com.noteshare.security.AuthContext;
import com.noteshare.service.FriendService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
public class FriendController {
    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping
    public List<String> friends(HttpServletRequest request) {
        return friendService.friendUsernames(AuthContext.requireUserId(request));
    }

    @GetMapping("/requests")
    public FriendRequestsResponse requests(HttpServletRequest request) {
        return friendService.pendingRequests(AuthContext.requireUserId(request));
    }

    @PostMapping("/requests")
    public Map<String, Boolean> send(HttpServletRequest request, @Valid @RequestBody FriendUsernameRequest body) {
        friendService.sendRequest(AuthContext.requireUserId(request), body.username());
        return Map.of("ok", true);
    }

    @PostMapping("/requests/{username}/accept")
    public Map<String, Boolean> accept(HttpServletRequest request, @PathVariable String username) {
        friendService.acceptRequest(AuthContext.requireUserId(request), username);
        return Map.of("ok", true);
    }

    @DeleteMapping("/requests/{username}")
    public Map<String, Boolean> cancelOrDecline(HttpServletRequest request, @PathVariable String username) {
        friendService.cancelOrDeclineRequest(AuthContext.requireUserId(request), username);
        return Map.of("ok", true);
    }

    @DeleteMapping("/{username}")
    public Map<String, Boolean> unfriend(HttpServletRequest request, @PathVariable String username) {
        friendService.removeFriend(AuthContext.requireUserId(request), username);
        return Map.of("ok", true);
    }
}
