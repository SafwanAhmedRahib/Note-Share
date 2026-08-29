package com.noteshare.service;

import com.noteshare.model.AuthToken;
import com.noteshare.model.User;
import com.noteshare.repository.AuthTokenRepository;
import com.noteshare.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {
    private static final long TOKEN_TTL_HOURS = 24;

    private final UserRepository userRepository;
    private final AuthTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, AuthTokenRepository tokenRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String register(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }
        User user = new User(username, passwordEncoder.encode(password));
        userRepository.save(user);
        return issueToken(user.getId());
    }

    public String login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return issueToken(user.getId());
    }

    /** Tokens are stored in the DB (not memory), so a backend restart no longer logs everyone out. */
    public Long userIdForToken(String token) {
        if (token == null) return null;
        return tokenRepository.findById(token)
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(AuthToken::getUserId)
                .orElse(null);
    }

    public void invalidate(String token) {
        if (token != null) {
            tokenRepository.deleteById(token);
        }
    }

    private String issueToken(Long userId) {
        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.save(new AuthToken(token, userId, now, now.plusHours(TOKEN_TTL_HOURS)));
        return token;
    }
}
