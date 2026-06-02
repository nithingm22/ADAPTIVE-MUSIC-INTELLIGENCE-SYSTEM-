package com.amis.model;

import jakarta.persistence.*;

/**
 * User entity — represents a registered user in AMIS.
 *
 * Roles:
 *   ADMIN → can manage songs and view all users
 *   USER  → can listen, create playlists, download offline
 *
 * Subscription tiers (used by BL7 Offline Download Manager):
 *   FREE    → 500 MB offline storage
 *   PREMIUM → 2048 MB (2 GB) offline storage
 *   FAMILY  → 5120 MB (5 GB) offline storage
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    /** ADMIN or USER */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    /**
     * Subscription tier that controls offline storage quota (BL7).
     * Defaults to FREE. Can be set to PREMIUM or FAMILY.
     *
     * FIX: Previously this field was missing entirely, so OfflineDownloadServiceImpl
     *      always defaulted to FREE regardless of the user's actual plan.
     *      Now the service reads this field to look up the correct quota.
     */
    @Column(name = "subscription_tier", nullable = false)
    private String subscriptionTier = "FREE";

    public enum Role {
        ADMIN, USER
    }

    // ── Constructors ──────────────────────────────────────────────────────

    public User() {}

    public User(String name, String email, String password, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.subscriptionTier = "FREE";
    }

    public User(String name, String email, String password, Role role, String subscriptionTier) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.subscriptionTier = subscriptionTier;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getSubscriptionTier() { return subscriptionTier; }
    public void setSubscriptionTier(String subscriptionTier) { this.subscriptionTier = subscriptionTier; }
}
