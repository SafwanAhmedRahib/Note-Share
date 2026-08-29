package com.noteshare.service;

import com.noteshare.dto.NoteResponse;
import com.noteshare.model.Note;
import com.noteshare.model.NoteShare;
import com.noteshare.model.User;
import com.noteshare.repository.NoteRepository;
import com.noteshare.repository.NoteShareRepository;
import com.noteshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock private NoteRepository noteRepository;
    @Mock private NoteShareRepository shareRepository;
    @Mock private UserRepository userRepository;
    @Mock private FriendService friendService;

    private NoteService noteService;

    @BeforeEach
    void setUp() {
        noteService = new NoteService(noteRepository, shareRepository, userRepository, friendService);
    }

    private Note note(Long id, Long ownerId, String title, String content, LocalDateTime updatedAt) {
        Note n = new Note();
        n.setId(id);
        n.setOwnerId(ownerId);
        n.setTitle(title);
        n.setContent(content);
        n.setCreatedAt(updatedAt);
        n.setUpdatedAt(updatedAt);
        return n;
    }

    @Test
    void createDefaultsToPrivateWhenVisibilityOmitted() {
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Note created = noteService.create(1L, "Title", "Body", null);

        assertThat(created.getVisibility()).isEqualTo(com.noteshare.model.NoteVisibility.PRIVATE);
    }

    @Test
    void createHonorsExplicitPublicVisibility() {
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Note created = noteService.create(1L, "Title", "Body", "public");

        assertThat(created.getVisibility()).isEqualTo(com.noteshare.model.NoteVisibility.PUBLIC);
    }

    @Test
    void createRejectsInvalidVisibility() {
        assertThatThrownBy(() -> noteService.create(1L, "Title", "Body", "sorta-public"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PUBLIC or PRIVATE");
    }

    @Test
    void updateLeavesVisibilityUnchangedWhenNotProvided() {
        Note existing = note(1L, 1L, "Title", "Body", LocalDateTime.now());
        existing.setVisibility(com.noteshare.model.NoteVisibility.PUBLIC);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Note updated = noteService.update(1L, 1L, "New title", "New body", null);

        assertThat(updated.getVisibility()).isEqualTo(com.noteshare.model.NoteVisibility.PUBLIC);
    }

    @Test
    void updateRejectsNonOwner() {
        Note existing = note(1L, 99L, "Title", "Body", LocalDateTime.now());
        when(noteRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> noteService.update(1L, 1L, "New title", "New body", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not your note");
    }

    @Test
    void deleteRejectsNonOwner() {
        Note existing = note(1L, 99L, "Title", "Body", LocalDateTime.now());
        when(noteRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> noteService.delete(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not your note");
    }

    @Test
    void shareRejectsUnknownUsername() {
        Note existing = note(1L, 1L, "Title", "Body", LocalDateTime.now());
        when(noteRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.share(1L, 1L, "ghost"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void shareRejectsSharingWithSelf() {
        Note existing = note(1L, 1L, "Title", "Body", LocalDateTime.now());
        User owner = new User("maya", "hash");
        owner.setId(1L);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("maya")).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> noteService.share(1L, 1L, "maya"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already own");
    }

    @Test
    void shareRejectsWhenNotFriends() {
        Note existing = note(1L, 1L, "Title", "Body", LocalDateTime.now());
        User target = new User("sam", "hash");
        target.setId(2L);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("sam")).thenReturn(Optional.of(target));
        when(friendService.areFriends(1L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> noteService.share(1L, 1L, "sam"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("friend");

        verify(shareRepository, never()).save(any(NoteShare.class));
    }

    @Test
    void shareSkipsDuplicateShare() {
        Note existing = note(1L, 1L, "Title", "Body", LocalDateTime.now());
        User target = new User("sam", "hash");
        target.setId(2L);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("sam")).thenReturn(Optional.of(target));
        when(friendService.areFriends(1L, 2L)).thenReturn(true);
        when(shareRepository.existsByNoteIdAndUserId(1L, 2L)).thenReturn(true);

        noteService.share(1L, 1L, "sam");

        verify(shareRepository, never()).save(any(NoteShare.class));
    }

    @Test
    void unshareRemovesShareRow() {
        Note existing = note(1L, 1L, "Title", "Body", LocalDateTime.now());
        User target = new User("sam", "hash");
        target.setId(2L);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("sam")).thenReturn(Optional.of(target));

        noteService.unshare(1L, 1L, "sam");

        verify(shareRepository).deleteByNoteIdAndUserId(1L, 2L);
    }

    @Test
    void notesForMergesOwnedAndSharedNotesAndMarksOwnership() {
        Note owned = note(1L, 1L, "Mine", "own content", LocalDateTime.now().minusHours(1));
        Note shared = note(2L, 5L, "Theirs", "shared content", LocalDateTime.now());
        User otherOwner = new User("sam", "hash");
        otherOwner.setId(5L);

        when(noteRepository.findByOwnerId(1L)).thenReturn(List.of(owned));
        when(shareRepository.findByUserId(1L)).thenReturn(List.of(new NoteShare(2L, 1L)));
        when(noteRepository.findById(2L)).thenReturn(Optional.of(shared));
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User("maya", "hash")));
        when(userRepository.findById(5L)).thenReturn(Optional.of(otherOwner));

        List<NoteResponse> result = noteService.notesFor(1L, null);

        assertThat(result).hasSize(2);
        // most recently updated first
        assertThat(result.get(0).title()).isEqualTo("Theirs");
        assertThat(result.get(0).owner()).isFalse();
        assertThat(result.get(0).ownerUsername()).isEqualTo("sam");
        assertThat(result.get(1).owner()).isTrue();
    }

    @Test
    void notesForFiltersBySearchQuery() {
        Note match = note(1L, 1L, "Groceries", "buy milk", LocalDateTime.now());
        Note noMatch = note(2L, 1L, "Taxes", "file by april", LocalDateTime.now());

        when(noteRepository.findByOwnerId(1L)).thenReturn(List.of(match, noMatch));
        when(shareRepository.findByUserId(1L)).thenReturn(List.of());
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User("maya", "hash")));

        List<NoteResponse> result = noteService.notesFor(1L, "milk");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Groceries");
    }
}
