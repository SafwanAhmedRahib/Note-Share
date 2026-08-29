package com.noteshare.service;

import com.noteshare.model.AuthToken;
import com.noteshare.model.User;
import com.noteshare.repository.AuthTokenRepository;
import com.noteshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthTokenRepository tokenRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, tokenRepository, passwordEncoder);
    }

    @Test
    void registerHashesPasswordAndIssuesToken() {
        when(userRepository.findByUsername("maya")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        String token = authService.register("maya", "secret123");

        assertThat(token).isNotBlank();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        // password must not be stored in plain text or as bare SHA-256
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", captor.getValue().getPasswordHash())).isTrue();
        verify(tokenRepository).save(any(AuthToken.class));
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userRepository.findByUsername("maya")).thenReturn(Optional.of(new User("maya", "hash")));

        assertThatThrownBy(() -> authService.register("maya", "secret123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already taken");
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = new User("maya", passwordEncoder.encode("correct-password"));
        user.setId(1L);
        when(userRepository.findByUsername("maya")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("maya", "wrong-password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void loginSucceedsAndIssuesTokenOnCorrectPassword() {
        User user = new User("maya", passwordEncoder.encode("correct-password"));
        user.setId(1L);
        when(userRepository.findByUsername("maya")).thenReturn(Optional.of(user));

        String token = authService.login("maya", "correct-password");

        assertThat(token).isNotBlank();
        verify(tokenRepository).save(any(AuthToken.class));
    }

    @Test
    void userIdForTokenReturnsNullWhenExpired() {
        AuthToken expired = new AuthToken("tok-1", 1L, LocalDateTime.now().minusHours(30), LocalDateTime.now().minusHours(6));
        when(tokenRepository.findById("tok-1")).thenReturn(Optional.of(expired));

        assertThat(authService.userIdForToken("tok-1")).isNull();
    }

    @Test
    void userIdForTokenReturnsUserIdWhenValid() {
        AuthToken valid = new AuthToken("tok-1", 42L, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        when(tokenRepository.findById("tok-1")).thenReturn(Optional.of(valid));

        assertThat(authService.userIdForToken("tok-1")).isEqualTo(42L);
    }

    @Test
    void invalidateRemovesToken() {
        authService.invalidate("tok-1");
        verify(tokenRepository).deleteById("tok-1");
    }
}
