package com.noteshare.service;

import com.noteshare.dto.NoteResponse;
import com.noteshare.model.Note;
import com.noteshare.model.NoteShare;
import com.noteshare.model.NoteVisibility;
import com.noteshare.model.User;
import com.noteshare.repository.NoteRepository;
import com.noteshare.repository.NoteShareRepository;
import com.noteshare.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class NoteService {
    private final NoteRepository noteRepository;
    private final NoteShareRepository shareRepository;
    private final UserRepository userRepository;
    private final FriendService friendService;

    public NoteService(NoteRepository noteRepository,
                       NoteShareRepository shareRepository,
                       UserRepository userRepository,
                       FriendService friendService) {
        this.noteRepository = noteRepository;
        this.shareRepository = shareRepository;
        this.userRepository = userRepository;
        this.friendService = friendService;
    }

    /** Returns the caller's own notes plus notes shared with them, most-recently-updated first,
     *  optionally filtered by a case-insensitive match against title or content. */
    public List<NoteResponse> notesFor(Long userId, String query) {
        List<Note> notes = new ArrayList<>(noteRepository.findByOwnerId(userId));
        for (NoteShare share : shareRepository.findByUserId(userId)) {
            noteRepository.findById(share.getNoteId()).ifPresent(n -> {
                if (!notes.contains(n)) notes.add(n);
            });
        }
        notes.sort(Comparator.comparing(Note::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        String q = (query == null) ? null : query.trim().toLowerCase();
        return notes.stream()
                .filter(n -> q == null || q.isBlank()
                        || (n.getTitle() != null && n.getTitle().toLowerCase().contains(q))
                        || (n.getContent() != null && n.getContent().toLowerCase().contains(q)))
                .map(n -> toResponse(n, userId))
                .toList();
    }

    public Note create(Long userId, String title, String content, String visibility) {
        Note note = new Note();
        note.setTitle(title);
        note.setContent(content);
        note.setOwnerId(userId);
        note.setVisibility(visibility == null || visibility.isBlank() ? NoteVisibility.PRIVATE : parseVisibility(visibility));
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        return noteRepository.save(note);
    }

    public Note update(Long userId, Long noteId, String title, String content, String visibility) {
        Note note = getOwnedNote(userId, noteId);
        if (title != null) note.setTitle(title);
        if (content != null) note.setContent(content);
        if (visibility != null) note.setVisibility(parseVisibility(visibility));
        note.setUpdatedAt(LocalDateTime.now());
        return noteRepository.save(note);
    }

    public void delete(Long userId, Long noteId) {
        Note note = getOwnedNote(userId, noteId);
        shareRepository.findByNoteId(noteId).forEach(shareRepository::delete);
        noteRepository.delete(note);
    }

    public void share(Long userId, Long noteId, String username) {
        Note note = getOwnedNote(userId, noteId);
        User target = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (target.getId().equals(userId)) {
            throw new IllegalArgumentException("You already own this note");
        }
        if (!friendService.areFriends(userId, target.getId())) {
            throw new IllegalArgumentException("Add them as a friend before sharing");
        }
        if (!shareRepository.existsByNoteIdAndUserId(noteId, target.getId())) {
            shareRepository.save(new NoteShare(noteId, target.getId()));
        }
    }

    public void unshare(Long userId, Long noteId, String username) {
        getOwnedNote(userId, noteId);
        User target = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        shareRepository.deleteByNoteIdAndUserId(noteId, target.getId());
    }

    /** Usernames a note is currently shared with. Owner-only. */
    public List<String> sharedWith(Long userId, Long noteId) {
        getOwnedNote(userId, noteId);
        List<Long> sharedUserIds = shareRepository.findByNoteId(noteId).stream()
                .map(NoteShare::getUserId)
                .toList();
        return userRepository.findAllById(sharedUserIds).stream()
                .map(User::getUsername)
                .toList();
    }

    private Note getOwnedNote(Long userId, Long noteId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found"));
        if (!note.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("Not your note");
        }
        return note;
    }

    private NoteVisibility parseVisibility(String value) {
        try {
            return NoteVisibility.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Visibility must be PUBLIC or PRIVATE");
        }
    }

    /** Public so FeedService (and anything else assembling a note list) can reuse the same mapping. */
    public NoteResponse toResponse(Note note, Long viewerId) {
        String ownerUsername = userRepository.findById(note.getOwnerId())
                .map(User::getUsername)
                .orElse("unknown");
        return new NoteResponse(
                note.getId(), note.getTitle(), note.getContent(), note.getOwnerId(),
                ownerUsername, note.getOwnerId().equals(viewerId), note.getVisibility().name(),
                note.getCreatedAt(), note.getUpdatedAt());
    }
}
