package com.noteshare.repository;

import com.noteshare.model.FriendRequest;
import com.noteshare.model.FriendStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    Optional<FriendRequest> findByRequesterIdAndAddresseeId(Long requesterId, Long addresseeId);
    List<FriendRequest> findByAddresseeIdAndStatus(Long addresseeId, FriendStatus status);
    List<FriendRequest> findByRequesterIdAndStatus(Long requesterId, FriendStatus status);

    @Query("SELECT f FROM FriendRequest f WHERE f.status = :status AND (f.requesterId = :userId OR f.addresseeId = :userId)")
    List<FriendRequest> findByStatusAndUserInvolved(@Param("status") FriendStatus status, @Param("userId") Long userId);

    void deleteByRequesterIdOrAddresseeId(Long requesterId, Long addresseeId);
}
