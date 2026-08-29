package com.noteshare.service;

import com.noteshare.dto.FriendRequestsResponse;
import com.noteshare.model.FriendRequest;
import com.noteshare.model.FriendStatus;
import com.noteshare.model.User;
import com.noteshare.repository.FriendRequestRepository;
import com.noteshare.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FriendService {
    private final FriendRequestRepository requestRepository;
    private final UserRepository userRepository;

    public FriendService(FriendRequestRepository requestRepository, UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    public void sendRequest(Long userId, String username) {
        User target = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (target.getId().equals(userId)) {
            throw new IllegalArgumentException("You can't add yourself as a friend");
        }

        // If they already asked us first, accept instead of creating a duplicate/competing request.
        Optional<FriendRequest> reverse = requestRepository.findByRequesterIdAndAddresseeId(target.getId(), userId);
        if (reverse.isPresent()) {
            FriendRequest r = reverse.get();
            if (r.getStatus() == FriendStatus.ACCEPTED) {
                throw new IllegalArgumentException("You're already friends");
            }
            r.setStatus(FriendStatus.ACCEPTED);
            requestRepository.save(r);
            return;
        }

        Optional<FriendRequest> existing = requestRepository.findByRequesterIdAndAddresseeId(userId, target.getId());
        if (existing.isPresent()) {
            throw new IllegalArgumentException(existing.get().getStatus() == FriendStatus.ACCEPTED
                    ? "You're already friends" : "Request already sent");
        }

        requestRepository.save(new FriendRequest(userId, target.getId(), FriendStatus.PENDING, LocalDateTime.now()));
    }

    public void acceptRequest(Long userId, String requesterUsername) {
        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        FriendRequest request = requestRepository.findByRequesterIdAndAddresseeId(requester.getId(), userId)
                .filter(r -> r.getStatus() == FriendStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("No pending request from that user"));
        request.setStatus(FriendStatus.ACCEPTED);
        requestRepository.save(request);
    }

    /** Cancels a request you sent, or declines one sent to you - whichever pending request exists between the two of you. */
    public void cancelOrDeclineRequest(Long userId, String otherUsername) {
        User other = userRepository.findByUsername(otherUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        FriendRequest target = requestRepository.findByRequesterIdAndAddresseeId(userId, other.getId())
                .filter(r -> r.getStatus() == FriendStatus.PENDING)
                .or(() -> requestRepository.findByRequesterIdAndAddresseeId(other.getId(), userId)
                        .filter(r -> r.getStatus() == FriendStatus.PENDING))
                .orElseThrow(() -> new IllegalArgumentException("No pending request with that user"));
        requestRepository.delete(target);
    }

    public void removeFriend(Long userId, String friendUsername) {
        User friend = userRepository.findByUsername(friendUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        requestRepository.findByRequesterIdAndAddresseeId(userId, friend.getId())
                .filter(r -> r.getStatus() == FriendStatus.ACCEPTED)
                .ifPresent(requestRepository::delete);
        requestRepository.findByRequesterIdAndAddresseeId(friend.getId(), userId)
                .filter(r -> r.getStatus() == FriendStatus.ACCEPTED)
                .ifPresent(requestRepository::delete);
    }

    public List<Long> friendUserIds(Long userId) {
        List<FriendRequest> accepted = requestRepository.findByStatusAndUserInvolved(FriendStatus.ACCEPTED, userId);
        return accepted.stream()
                .map(r -> r.getRequesterId().equals(userId) ? r.getAddresseeId() : r.getRequesterId())
                .toList();
    }

    public List<String> friendUsernames(Long userId) {
        return userRepository.findAllById(friendUserIds(userId)).stream().map(User::getUsername).toList();
    }

    public FriendRequestsResponse pendingRequests(Long userId) {
        List<String> incoming = requestRepository.findByAddresseeIdAndStatus(userId, FriendStatus.PENDING).stream()
                .map(r -> usernameOf(r.getRequesterId())).toList();
        List<String> outgoing = requestRepository.findByRequesterIdAndStatus(userId, FriendStatus.PENDING).stream()
                .map(r -> usernameOf(r.getAddresseeId())).toList();
        return new FriendRequestsResponse(incoming, outgoing);
    }

    public boolean areFriends(Long userA, Long userB) {
        return requestRepository.findByRequesterIdAndAddresseeId(userA, userB)
                .filter(r -> r.getStatus() == FriendStatus.ACCEPTED).isPresent()
                || requestRepository.findByRequesterIdAndAddresseeId(userB, userA)
                .filter(r -> r.getStatus() == FriendStatus.ACCEPTED).isPresent();
    }

    private String usernameOf(Long userId) {
        return userRepository.findById(userId).map(User::getUsername).orElse("unknown");
    }
}
