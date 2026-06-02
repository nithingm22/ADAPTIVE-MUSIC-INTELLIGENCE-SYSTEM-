package com.amis.dto.response;

/**
 * AuthResponse — returned after a successful login or registration.
 *
 * FIX: Added subscriptionTier so the frontend (BL7 Offline page) knows
 *      whether to show the FREE 500 MB gauge or PREMIUM 2 GB gauge
 *      without making an extra API call.
 */
public class AuthResponse {

    private String token;
    private String name;
    private String email;
    private String role;
    private String subscriptionTier;

    public AuthResponse() {}

    public AuthResponse(String token, String name, String email,
                        String role, String subscriptionTier) {
        this.token            = token;
        this.name             = name;
        this.email            = email;
        this.role             = role;
        this.subscriptionTier = subscriptionTier;
    }

    // Getters and Setters
    public String getToken()            { return token; }
    public void setToken(String t)      { this.token = t; }

    public String getName()             { return name; }
    public void setName(String n)       { this.name = n; }

    public String getEmail()            { return email; }
    public void setEmail(String e)      { this.email = e; }

    public String getRole()             { return role; }
    public void setRole(String r)       { this.role = r; }

    public String getSubscriptionTier() { return subscriptionTier; }
    public void setSubscriptionTier(String t) { this.subscriptionTier = t; }
}
