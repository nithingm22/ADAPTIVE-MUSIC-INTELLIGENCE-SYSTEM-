package com.amis.service;

import com.amis.dto.request.LoginRequest;
import com.amis.dto.request.RegisterRequest;
import com.amis.dto.response.AuthResponse;

/**
 * AuthService - Interface for user registration and login.
 */
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
