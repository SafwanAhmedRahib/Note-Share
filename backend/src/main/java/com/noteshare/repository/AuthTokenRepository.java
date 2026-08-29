package com.noteshare.repository;

import com.noteshare.model.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {
    void deleteByExpiresAtBefore(LocalDateTime cutoff);
    void deleteByUserId(Long userId);
}
