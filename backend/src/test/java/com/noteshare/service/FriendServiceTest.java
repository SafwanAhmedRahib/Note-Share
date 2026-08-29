package com.noteshare.service;

import com.noteshare.dto.FriendRequestsResponse;
import com.noteshare.model.FriendRequest;
import com.noteshare.model.FriendStatus;
import com.noteshare.model.User;
import com.noteshare.repository.FriendRequestRepository;
import com.noteshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class FriendServiceTest {

    @Mock private FriendRequestRepository requestRepository;
    @Mock private UserRepository userRepository;

    private FriendService friendService;

    @BeforeEach
    void setUp() {
        friendService = new FriendService(requestRepository, userRepository);
    }

    private User user(Long id, String name) {
        User u = new User(name, "hash");
        u.setId(id);
        return u;
    }

    @Test
    void sendRequestRejectsSelf() {
        User self = user(1L, "maya");
        when(userRepository.findByUsername("maya")).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> friendService.sendRequest(1L, "maya"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yourself");
    }

    @Test
    void sendRequestCreatesPendingRow() {
        User target = user(2L, "sam");
        when(userRepository.findByUsername("sam")).thenReturn(Optional.of(target));
        when(requestRepository.findByRequesterIdAndAddresseeId(2L, 1L)).thenReturn(Optional.empty());
        when(requestRepository.findByRequesterIdAndAddresseeId(1L, 2L)).thenReturn(Optional.empty());

        friendService.sendRequest(1L, "sam");

        ArgumentCaptor<FriendRequest> captor = ArgumentCaptor.forClass(FriendRequest.class);
        verify(requestRepository).save(captor.capture());
        assertThat(captor.getValue().getRequesterId()).isEqualTo(1L);
        assertThat(captor.getValue().getAddresseeId()).isEqualTo(2L);
        assertThat(captor.getValue().getStatus()).isEqualTo(FriendStatus.PENDING);
    }

    @Test
    void sendRequestAutoAcceptsWhenReverseRequestIsPending() {
        User target = user(2L, "sam");
        FriendRequest reverse = new FriendRequest(2L, 1L, FriendStatus.PENDING, LocalDateTime.now());
        when(userRepository.findByUsername("sam")).thenReturn(Optional.of(target));
        when(requestRepository.findByRequesterIdAndAddresseeId(2L, 1L)).thenReturn(Optional.of(reverse));

        friendService.sendRequest(1L, "sam");

        assertThat(reverse.getStatus()).isEqualTo(FriendStatus.ACCEPTED);
        verify(requestRepository).save(reverse);
    }

    @Test
    void sendRequestRejectsWhenAlreadyFriends() {
        User target = user(2L, "sam");
        FriendRequest reverse = new FriendRequest(2L, 1L, FriendStatus.ACCEPTED, LocalDateTime.now());
        when(userRepository.findByUsername("sam")).thenReturn(Optional.of(target));
        when(requestRepository.findByRequesterIdAndAddresseeId(2L, 1L)).thenReturn(Optional.of(reverse));

        assertThatThrownBy(() -> friendService.sendRequest(1L, "sam"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already friends");
    }

    @Test
    void acceptRequestRequiresPendingIncomingRequest() {
        User requester = user(2L, "sam");
        when(userRepository.findByUsername("sam")).thenReturn(Optional.of(requester));
        when(requestRepository.findByRequesterIdAndAddresseeId(2L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendService.acceptRequest(1L, "sam"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No pending request");
    }

    @Test
    void acceptRequestMarksAccepted() {
        User requester = user(2L, "sam");
        FriendRequest pending = new FriendRequest(2L, 1L, FriendStatus.PENDING, LocalDateTime.now());
        when(userRepository.findByUsername("sam")).thenReturn(Optional.of(requester));
        when(requestRepository.findByRequesterIdAndAddresseeId(2L, 1L)).thenReturn(Optional.of(pending));

        friendService.acceptRequest(1L, "sam");

        assertThat(pending.getStatus()).isEqualTo(FriendStatus.ACCEPTED);
    }

    @Test
    void removeFriendDeletesAcceptedRowRegardlessOfDirection() {
        User friend = user(2L, "sam");
        when(userRepository.findByUsername("sam")).thenReturn(Optional.of(friend));
        when(requestRepository.findByRequesterIdAndAddresseeId(1L, 2L)).thenReturn(Optional.empty());
        FriendRequest accepted = new FriendRequest(2L, 1L, FriendStatus.ACCEPTED, LocalDateTime.now());
        when(requestRepository.findByRequesterIdAndAddresseeId(2L, 1L)).thenReturn(Optional.of(accepted));

        friendService.removeFriend(1L, "sam");

        verify(requestRepository).delete(accepted);
    }

    @Test
    void friendUsernamesResolvesTheOtherPersonInEachPair() {
        FriendRequest asRequester = new FriendRequest(1L, 2L, FriendStatus.ACCEPTED, LocalDateTime.now());
        FriendRequest asAddressee = new FriendRequest(3L, 1L, FriendStatus.ACCEPTED, LocalDateTime.now());
        when(requestRepository.findByStatusAndUserInvolved(FriendStatus.ACCEPTED, 1L))
                .thenReturn(List.of(asRequester, asAddressee));
        when(userRepository.findAllById(List.of(2L, 3L)))
                .thenReturn(List.of(user(2L, "sam"), user(3L, "jo")));

        List<String> friends = friendService.friendUsernames(1L);

        assertThat(friends).containsExactlyInAnyOrder("sam", "jo");
    }

    @Test
    void pendingRequestsSeparatesIncomingAndOutgoing() {
        when(requestRepository.findByAddresseeIdAndStatus(1L, FriendStatus.PENDING))
                .thenReturn(List.of(new FriendRequest(2L, 1L, FriendStatus.PENDING, LocalDateTime.now())));
        when(requestRepository.findByRequesterIdAndStatus(1L, FriendStatus.PENDING))
                .thenReturn(List.of(new FriendRequest(1L, 3L, FriendStatus.PENDING, LocalDateTime.now())));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "sam")));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user(3L, "jo")));

        FriendRequestsResponse response = friendService.pendingRequests(1L);

        assertThat(response.incoming()).containsExactly("sam");
        assertThat(response.outgoing()).containsExactly("jo");
    }

    @Test
    void areFriendsChecksBothDirections() {
        when(requestRepository.findByRequesterIdAndAddresseeId(1L, 2L)).thenReturn(Optional.empty());
        when(requestRepository.findByRequesterIdAndAddresseeId(2L, 1L))
                .thenReturn(Optional.of(new FriendRequest(2L, 1L, FriendStatus.ACCEPTED, LocalDateTime.now())));

        assertThat(friendService.areFriends(1L, 2L)).isTrue();
    }
}
