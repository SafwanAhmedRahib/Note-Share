package com.noteshare.service;

import com.noteshare.model.AccountVisibility;
import com.noteshare.model.Note;
import com.noteshare.model.User;
import com.noteshare.repository.AuthTokenRepository;
import com.noteshare.repository.FriendRequestRepository;
import com.noteshare.repository.NoteRepository;
import com.noteshare.repository.NoteShareRepository;
import com.noteshare.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountService {
    private final UserRepository userRepository;
    private final NoteRepository noteRepository;
    private final NoteShareRepository shareRepository;
    private final AuthTokenRepository tokenRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository userRepository,
                          NoteRepository noteRepository,
                          NoteShareRepository shareRepository,
                          AuthTokenRepository tokenRepository,
                          FriendRequestRepository friendRequestRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.noteRepository = noteRepository;
        this.shareRepository = shareRepository;
        this.tokenRepository = tokenRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Deletes the account and everything owned by it: the user's own notes
     * (and any shares of those notes to other people), the user's shares of
     * other people's notes, all of the user's login sessions, and finally
     * the user row itself. Requires the current password as confirmation
     * since a valid token alone (e.g. left open on a shared computer)
     * shouldn't be enough to destroy an account.
     */
    @Transactional
    public void deleteAccount(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect password");
        }

        List<Note> ownedNotes = noteRepository.findByOwnerId(userId);
        List<Long> ownedNoteIds = ownedNotes.stream().map(Note::getId).toList();
        if (!ownedNoteIds.isEmpty()) {
            shareRepository.deleteByNoteIdIn(ownedNoteIds);
        }
        shareRepository.deleteByUserId(userId);
        noteRepository.deleteAll(ownedNotes);
        tokenRepository.deleteByUserId(userId);
        friendRequestRepository.deleteByRequesterIdOrAddresseeId(userId, userId);
        userRepository.delete(user);
    }

    public AccountVisibility getVisibility(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"))
                .getVisibility();
    }

    /**
     * PRIVATE: your notes (public or private) are only ever visible to your friends in
     * the feed, regardless of each note's own visibility flag - the account setting is
     * the outer gate. PUBLIC: notes you've individually marked Public can reach anyone;
     * notes you've marked Private are still friends-only either way.
     */
    public void setVisibility(Long userId, String visibility) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        user.setVisibility(parseVisibility(visibility));
        userRepository.save(user);
    }

    private AccountVisibility parseVisibility(String value) {
        try {
            return AccountVisibility.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Visibility must be PUBLIC or PRIVATE");
        }
    }
}
