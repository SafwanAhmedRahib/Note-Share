package com.noteshare.service;

import com.noteshare.dto.NoteResponse;
import com.noteshare.model.AccountVisibility;
import com.noteshare.model.Note;
import com.noteshare.model.NoteVisibility;
import com.noteshare.model.User;
import com.noteshare.repository.NoteRepository;
import com.noteshare.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The feed is deliberately broader-access than the explicit note-sharing
 * system: once two people are friends, ALL of each other's notes appear in
 * the feed (public or private) - not just notes explicitly shared via
 * NoteService.share(). A note only reaches a stranger (non-friend, non-owner)
 * when BOTH the note itself is marked PUBLIC and the owner's account-level
 * visibility is PUBLIC - the account setting is a master gate on top of the
 * per-note flag, matching how a private social account hides everything
 * from non-followers regardless of individual post settings.
 *
 * Note: this scans every note in the table and filters in memory, which is
 * fine at the scale this app is meant for (a household/small office on one
 * Wi-Fi network) but wouldn't scale to a large user base without pagination
 * and a proper query.
 */
@Service
public class FeedService {
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final NoteService noteService;
    private final FriendService friendService;

    public FeedService(NoteRepository noteRepository,
                       UserRepository userRepository,
                       NoteService noteService,
                       FriendService friendService) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.noteService = noteService;
        this.friendService = friendService;
    }

    public List<NoteResponse> feedFor(Long userId) {
        Set<Long> friendIds = new HashSet<>(friendService.friendUserIds(userId));
        return noteRepository.findAll().stream()
                .filter(n -> isVisibleTo(n, userId, friendIds))
                .sorted(Comparator.comparing(Note::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(n -> noteService.toResponse(n, userId))
                .toList();
    }

    private boolean isVisibleTo(Note note, Long viewerId, Set<Long> friendIds) {
        if (note.getOwnerId().equals(viewerId)) return true;
        if (friendIds.contains(note.getOwnerId())) return true;
        if (note.getVisibility() != NoteVisibility.PUBLIC) return false;
        return userRepository.findById(note.getOwnerId())
                .map(User::getVisibility)
                .map(visibility -> visibility == AccountVisibility.PUBLIC)
                .orElse(false);
    }
}
