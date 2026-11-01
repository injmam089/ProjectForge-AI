package com.projectforge.service;

import com.projectforge.dto.AuthResponse;
import com.projectforge.dto.LoginRequest;
import com.projectforge.dto.RegisterRequest;
import com.projectforge.dto.UserResponse;
import com.projectforge.entity.Role;
import com.projectforge.entity.User;
import com.projectforge.exception.EmailAlreadyExistsException;
import com.projectforge.exception.InvalidCredentialsException;
import com.projectforge.exception.ResourceNotFoundException;
import com.projectforge.repository.UserRepository;
import com.projectforge.security.JwtService;
import com.projectforge.security.SecurityUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service orchestrating user registration, credential verification, and JWT generation.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Registration rejected: email '{}' already registered", normalizedEmail);
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.name().trim(), normalizedEmail, passwordHash, Role.USER);
        User savedUser = userRepository.save(user);

        log.info("User registered successfully with id: {}", savedUser.getId());
        return AuthResponse.registerSuccess();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );

            SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
            User user = securityUser.getUser();

            String token = jwtService.generateToken(securityUser, user.getId(), user.getRole());

            log.info("User logged in successfully with id: {}", user.getId());
            return AuthResponse.loginSuccess(token, UserResponse.fromEntity(user));

        } catch (BadCredentialsException | InvalidCredentialsException e) {
            log.warn("Login failed: invalid credentials for email '{}'", normalizedEmail);
            throw new InvalidCredentialsException();
        } catch (AuthenticationException e) {
            log.warn("Authentication failed for email '{}': {}", normalizedEmail, e.getMessage());
            throw new InvalidCredentialsException();
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", normalizedEmail));
        return UserResponse.fromEntity(user);
    }
}
