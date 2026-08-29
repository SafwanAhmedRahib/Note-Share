package com.noteshare.service;

import com.noteshare.dto.NoteResponse;
import com.noteshare.model.AccountVisibility;
import com.noteshare.model.Note;
import com.noteshare.model.NoteVisibility;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock private NoteRepository noteRepository;
    @Mock private NoteShareRepository shareRepository; // unused directly, but NoteService needs it
    @Mock private UserRepository userRepository;
    @Mock private FriendService friendService;

    private FeedService feedService;

    @BeforeEach
    void setUp() {
        NoteService noteService = new NoteService(noteRepository, shareRepository, userRepository, friendService);
        feedService = new FeedService(noteRepository, userRepository, noteService, friendService);
    }

    private Note note(Long id, Long ownerId, NoteVisibility visibility) {
        Note n = new Note();
        n.setId(id);
        n.setOwnerId(ownerId);
        n.setTitle("Title " + id);
        n.setContent("Body " + id);
        n.setVisibility(visibility);
        n.setUpdatedAt(LocalDateTime.now());
        return n;
    }

    private User user(String name, AccountVisibility visibility) {
        User u = new User(name, "hash");
        u.setVisibility(visibility);
        return u;
    }

    @Test
    void feedIncludesOwnNotesRegardlessOfVisibility() {
        Note ownPrivate = note(1L, 1L, NoteVisibility.PRIVATE);
        when(noteRepository.findAll()).thenReturn(List.of(ownPrivate));
        when(friendService.friendUserIds(1L)).thenReturn(List.of());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user("maya", AccountVisibility.PUBLIC)));

        List<NoteResponse> feed = feedService.feedFor(1L);

        assertThat(feed).extracting(NoteResponse::id).containsExactly(1L);
    }

    @Test
    void feedIncludesFriendsPrivateNotesRegardlessOfAccountVisibility() {
        Note friendPrivate = note(2L, 5L, NoteVisibility.PRIVATE);
        when(noteRepository.findAll()).thenReturn(List.of(friendPrivate));
        when(friendService.friendUserIds(1L)).thenReturn(List.of(5L));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user("sam", AccountVisibility.PRIVATE)));

        List<NoteResponse> feed = feedService.feedFor(1L);

        assertThat(feed).extracting(NoteResponse::id).containsExactly(2L);
        assertThat(feed.get(0).owner()).isFalse();
    }

    @Test
    void feedIncludesPublicNotesFromStrangersWithPublicAccounts() {
        Note strangerPublic = note(3L, 9L, NoteVisibility.PUBLIC);
        when(noteRepository.findAll()).thenReturn(List.of(strangerPublic));
        when(friendService.friendUserIds(1L)).thenReturn(List.of());
        when(userRepository.findById(9L)).thenReturn(Optional.of(user("stranger", AccountVisibility.PUBLIC)));

        List<NoteResponse> feed = feedService.feedFor(1L);

        assertThat(feed).extracting(NoteResponse::id).containsExactly(3L);
    }

    @Test
    void feedExcludesPublicNotesFromStrangersWithPrivateAccounts() {
        Note strangerPublicNote = note(4L, 9L, NoteVisibility.PUBLIC);
        when(noteRepository.findAll()).thenReturn(List.of(strangerPublicNote));
        when(friendService.friendUserIds(1L)).thenReturn(List.of());
        when(userRepository.findById(9L)).thenReturn(Optional.of(user("stranger", AccountVisibility.PRIVATE)));

        List<NoteResponse> feed = feedService.feedFor(1L);

        assertThat(feed).isEmpty();
    }

    @Test
    void feedExcludesStrangersPrivateNotes() {
        Note strangerPrivate = note(5L, 9L, NoteVisibility.PRIVATE);
        when(noteRepository.findAll()).thenReturn(List.of(strangerPrivate));
        when(friendService.friendUserIds(1L)).thenReturn(List.of());

        List<NoteResponse> feed = feedService.feedFor(1L);

        assertThat(feed).isEmpty();
    }
}
