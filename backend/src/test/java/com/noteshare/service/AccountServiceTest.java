package com.noteshare.service;

import com.noteshare.model.AccountVisibility;
import com.noteshare.model.Note;
import com.noteshare.model.User;
import com.noteshare.repository.AuthTokenRepository;
import com.noteshare.repository.FriendRequestRepository;
import com.noteshare.repository.NoteRepository;
import com.noteshare.repository.NoteShareRepository;
import com.noteshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private NoteRepository noteRepository;
    @Mock private NoteShareRepository shareRepository;
    @Mock private AuthTokenRepository tokenRepository;
    @Mock private FriendRequestRepository friendRequestRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(userRepository, noteRepository, shareRepository, tokenRepository, friendRequestRepository, passwordEncoder);
    }

    private Note note(Long id, Long ownerId) {
        Note n = new Note();
        n.setId(id);
        n.setOwnerId(ownerId);
        return n;
    }

    @Test
    void rejectsWrongPassword() {
        User user = new User("maya", passwordEncoder.encode("correct-password"));
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> accountService.deleteAccount(1L, "wrong-password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Incorrect password");

        verify(userRepository, never()).delete(any());
    }

    @Test
    void deletesOwnedNotesTheirSharesAndTokensThenTheUser() {
        User user = new User("maya", passwordEncoder.encode("correct-password"));
        user.setId(1L);
        List<Note> owned = List.of(note(10L, 1L), note(11L, 1L));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(noteRepository.findByOwnerId(1L)).thenReturn(owned);

        accountService.deleteAccount(1L, "correct-password");

        verify(shareRepository).deleteByNoteIdIn(List.of(10L, 11L));
        verify(shareRepository).deleteByUserId(1L);
        verify(noteRepository).deleteAll(owned);
        verify(tokenRepository).deleteByUserId(1L);
        verify(friendRequestRepository).deleteByRequesterIdOrAddresseeId(1L, 1L);
        verify(userRepository).delete(user);
    }

    @Test
    void skipsNoteShareCleanupWhenUserOwnsNoNotes() {
        User user = new User("maya", passwordEncoder.encode("correct-password"));
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(noteRepository.findByOwnerId(1L)).thenReturn(List.of());

        accountService.deleteAccount(1L, "correct-password");

        verify(shareRepository, never()).deleteByNoteIdIn(any());
        verify(shareRepository).deleteByUserId(1L);
        verify(userRepository).delete(user);
    }

    @Test
    void getVisibilityReturnsCurrentSetting() {
        User user = new User("maya", "hash");
        user.setId(1L);
        user.setVisibility(AccountVisibility.PRIVATE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(accountService.getVisibility(1L)).isEqualTo(AccountVisibility.PRIVATE);
    }

    @Test
    void setVisibilitySavesTheNewValue() {
        User user = new User("maya", "hash");
        user.setId(1L);
        user.setVisibility(AccountVisibility.PUBLIC);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        accountService.setVisibility(1L, "private");

        assertThat(user.getVisibility()).isEqualTo(AccountVisibility.PRIVATE);
        verify(userRepository).save(user);
    }

    @Test
    void setVisibilityRejectsInvalidValue() {
        User user = new User("maya", "hash");
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> accountService.setVisibility(1L, "sorta-private"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PUBLIC or PRIVATE");
    }
}
