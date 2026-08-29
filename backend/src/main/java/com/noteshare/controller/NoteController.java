package com.noteshare.controller;

import com.noteshare.dto.NoteRequest;
import com.noteshare.dto.NoteResponse;
import com.noteshare.dto.ShareRequest;
import com.noteshare.model.Note;
import com.noteshare.security.AuthContext;
import com.noteshare.service.NoteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NoteController {
    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping("/notes")
    public List<NoteResponse> list(HttpServletRequest request, @RequestParam(required = false) String q) {
        Long userId = AuthContext.requireUserId(request);
        return noteService.notesFor(userId, q);
    }

    @PostMapping("/notes")
    public Note create(HttpServletRequest request, @Valid @RequestBody NoteRequest body) {
        Long userId = AuthContext.requireUserId(request);
        return noteService.create(userId, body.title(), body.content(), body.visibility());
    }

    @PutMapping("/notes/{id}")
    public Note update(HttpServletRequest request, @PathVariable Long id, @Valid @RequestBody NoteRequest body) {
        Long userId = AuthContext.requireUserId(request);
        return noteService.update(userId, id, body.title(), body.content(), body.visibility());
    }

    @DeleteMapping("/notes/{id}")
    public Map<String, Boolean> delete(HttpServletRequest request, @PathVariable Long id) {
        Long userId = AuthContext.requireUserId(request);
        noteService.delete(userId, id);
        return Map.of("ok", true);
    }

    @PostMapping("/notes/{id}/share")
    public Map<String, Boolean> share(HttpServletRequest request, @PathVariable Long id, @Valid @RequestBody ShareRequest body) {
        Long userId = AuthContext.requireUserId(request);
        noteService.share(userId, id, body.username());
        return Map.of("ok", true);
    }

    @GetMapping("/notes/{id}/shares")
    public List<String> shares(HttpServletRequest request, @PathVariable Long id) {
        Long userId = AuthContext.requireUserId(request);
        return noteService.sharedWith(userId, id);
    }

    @DeleteMapping("/notes/{id}/share/{username}")
    public Map<String, Boolean> unshare(HttpServletRequest request, @PathVariable Long id, @PathVariable String username) {
        Long userId = AuthContext.requireUserId(request);
        noteService.unshare(userId, id, username);
        return Map.of("ok", true);
    }
}
