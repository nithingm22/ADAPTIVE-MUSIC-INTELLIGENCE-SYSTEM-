package com.amis.service.impl;

import com.amis.config.JwtUtil;
import com.amis.dto.request.LoginRequest;
import com.amis.dto.request.RegisterRequest;
import com.amis.dto.response.AuthResponse;
import com.amis.model.User;
import com.amis.repository.UserRepository;
import com.amis.service.AuthService;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * AuthServiceImpl — handles user registration and login.
 *
 * FIXES:
 *  1. Accepts and stores subscriptionTier during registration.
 *  2. Returns subscriptionTier in every AuthResponse so the frontend
 *     can show the correct offline quota immediately after login.
 *  3. @PostConstruct seeds 3 ready-to-use demo accounts so you never
 *     need to run SQL to get an admin or premium user.
 *
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * DEMO ACCOUNTS (auto-created on first startup):
 *
 *   Email                  Password    Role    Tier
 *   ─────────────────────  ──────────  ──────  ───────
 *   admin@amis.com         admin123    ADMIN   FREE
 *   user1@amis.com         user123     USER    FREE
 *   user2@amis.com         user123     USER    PREMIUM
 *
 * These are safe for demo and testing. Each user gets their own
 * playlist, history, and offline quota.
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil         jwtUtil;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil         = jwtUtil;
    }

    // ── Demo account seeding ──────────────────────────────────────────────

    /**
     * Runs once when the Spring context starts.
     * Creates the three demo users if they don't already exist,
     * so the app is immediately usable without any SQL setup.
     */
    @PostConstruct
    public void seedDemoAccounts() {
        createIfAbsent("Admin User",    "admin@amis.com",  "admin123", User.Role.ADMIN, "FREE");
        createIfAbsent("Alice (Free)",  "user1@amis.com",  "user123",  User.Role.USER,  "FREE");
        createIfAbsent("Bob (Premium)", "user2@amis.com",  "user123",  User.Role.USER,  "PREMIUM");
    }

    private void createIfAbsent(String name, String email, String rawPassword,
                                 User.Role role, String tier) {
        if (!userRepository.existsByEmail(email)) {
            User u = new User(name, email, passwordEncoder.encode(rawPassword), role, tier);
            userRepository.save(u);
            System.out.printf("[AMIS] Demo account created → %s (%s / %s)%n", email, role, tier);
        }
    }

    // ── Registration ──────────────────────────────────────────────────────

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        // Resolve subscription tier — default to FREE if absent or unrecognised
        String tier = resolvedTier(request.getSubscriptionTier());

        User user = new User(
            request.getName(),
            request.getEmail(),
            passwordEncoder.encode(request.getPassword()),
            User.Role.USER,
            tier
        );
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getName(), user.getEmail(),
                                user.getRole().name(), user.getSubscriptionTier());
    }

    // ── Login ─────────────────────────────────────────────────────────────

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException(
                        "No account found for: " + request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Incorrect password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getName(), user.getEmail(),
                                user.getRole().name(), user.getSubscriptionTier());
    }

    // ── Helper ────────────────────────────────────────────────────────────

    /** Validates and normalises the subscription tier string. */
    private String resolvedTier(String raw) {
        if (raw == null) return "FREE";
        switch (raw.toUpperCase()) {
            case "PREMIUM": return "PREMIUM";
            case "FAMILY":  return "FAMILY";
            default:        return "FREE";
        }
    }
}
