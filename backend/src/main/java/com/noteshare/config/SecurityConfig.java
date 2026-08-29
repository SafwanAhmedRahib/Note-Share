package com.noteshare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Only brings in BCryptPasswordEncoder from spring-security-crypto.
 * We deliberately do NOT add spring-boot-starter-security here, since that
 * would auto-configure a full security filter chain (default login page,
 * CSRF, etc.) that this app's hand-rolled token auth isn't built around.
 */
@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
