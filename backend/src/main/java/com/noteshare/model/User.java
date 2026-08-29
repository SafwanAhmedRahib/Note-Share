package com.noteshare.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    // Defaults to PUBLIC so a note's own "public" flag keeps working the way it already did
    // for existing accounts - switching to PRIVATE is an opt-in lockdown, not a silent one.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountVisibility visibility = AccountVisibility.PUBLIC;

    public User() {}
    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public AccountVisibility getVisibility() { return visibility; }
    public void setVisibility(AccountVisibility visibility) { this.visibility = visibility; }
}
