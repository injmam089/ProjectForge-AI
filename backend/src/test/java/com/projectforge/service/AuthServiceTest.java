package com.projectforge.service;

import com.projectforge.dto.AuthResponse;
import com.projectforge.dto.LoginRequest;
import com.projectforge.dto.RegisterRequest;
import com.projectforge.dto.UserResponse;
import com.projectforge.entity.Role;
import com.projectforge.entity.User;
import com.projectforge.exception.EmailAlreadyExistsException;
import com.projectforge.exception.InvalidCredentialsException;
import com.projectforge.repository.UserRepository;
import com.projectforge.security.JwtService;
import com.projectforge.security.SecurityUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private InMemoryUserRepository userRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", 86400000);
        userRepository = new InMemoryUserRepository();

        AuthenticationManager testAuthManager = authentication -> {
            String email = authentication.getName();
            String rawPassword = authentication.getCredentials().toString();

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadCredentialsException("Bad credentials"));

            if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
                throw new BadCredentialsException("Bad credentials");
            }

            SecurityUser securityUser = new SecurityUser(user);
            return new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        };

        authService = new AuthService(userRepository, passwordEncoder, jwtService, testAuthManager);
    }

    @Test
    @DisplayName("register should hash password with BCrypt, normalize email, and persist user")
    void register_shouldHashPasswordAndPersistUser() {
        RegisterRequest request = new RegisterRequest("Jane Developer", " Jane.Dev@Example.com ", "SuperSecretPass123!");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertTrue(response.success());
        assertEquals("User registered successfully", response.message());

        // Verify persisted user in repository
        Optional<User> savedUserOpt = userRepository.findByEmail("jane.dev@example.com");
        assertTrue(savedUserOpt.isPresent());

        User savedUser = savedUserOpt.get();
        assertEquals("Jane Developer", savedUser.getName());
        assertEquals("jane.dev@example.com", savedUser.getEmail());
        assertEquals(Role.USER, savedUser.getRole());

        // Verify password is NOT plaintext
        assertFalse(savedUser.getPasswordHash().contains("SuperSecretPass123!"));
        assertTrue(passwordEncoder.matches("SuperSecretPass123!", savedUser.getPasswordHash()));
    }

    @Test
    @DisplayName("register with existing email should throw EmailAlreadyExistsException")
    void register_duplicateEmail_shouldThrowException() {
        RegisterRequest first = new RegisterRequest("John Doe", "john@example.com", "Password123!");
        authService.register(first);

        RegisterRequest duplicate = new RegisterRequest("John Duplicate", "JOHN@EXAMPLE.COM", "AnotherPassword123!");
        EmailAlreadyExistsException exception = assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(duplicate)
        );

        assertTrue(exception.getMessage().contains("john@example.com"));
    }

    @Test
    @DisplayName("login with valid credentials should return JWT token and UserResponse")
    void login_validCredentials_shouldReturnToken() {
        authService.register(new RegisterRequest("Bob Smith", "bob@example.com", "CorrectPassword123!"));

        LoginRequest loginRequest = new LoginRequest("bob@example.com", "CorrectPassword123!");
        AuthResponse loginResponse = authService.login(loginRequest);

        assertNotNull(loginResponse);
        assertTrue(loginResponse.success());
        assertEquals("Login successful", loginResponse.message());
        assertEquals("Bearer", loginResponse.tokenType());
        assertNotNull(loginResponse.token());

        // Validate returned JWT token
        String extractedUser = jwtService.extractUsername(loginResponse.token());
        assertEquals("bob@example.com", extractedUser);

        // Verify UserResponse does not leak credentials
        assertNotNull(loginResponse.user());
        assertEquals("Bob Smith", loginResponse.user().name());
        assertEquals("bob@example.com", loginResponse.user().email());
    }

    @Test
    @DisplayName("login with wrong password should throw InvalidCredentialsException")
    void login_wrongPassword_shouldThrowException() {
        authService.register(new RegisterRequest("Bob Smith", "bob@example.com", "CorrectPassword123!"));

        LoginRequest loginRequest = new LoginRequest("bob@example.com", "WrongPassword123!");
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    @DisplayName("login with unknown email should throw InvalidCredentialsException")
    void login_unknownEmail_shouldThrowException() {
        LoginRequest loginRequest = new LoginRequest("ghost@example.com", "AnyPassword123!");
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    @DisplayName("getCurrentUser should return sanitized UserResponse without password")
    void getCurrentUser_shouldReturnSanitizedResponse() {
        authService.register(new RegisterRequest("Alice Wonderland", "alice@example.com", "Password123!"));

        UserResponse userResponse = authService.getCurrentUser("alice@example.com");

        assertNotNull(userResponse);
        assertEquals("Alice Wonderland", userResponse.name());
        assertEquals("alice@example.com", userResponse.email());
        assertEquals(Role.USER, userResponse.role());
    }

    // Lightweight in-memory UserRepository implementation for fast, reliable unit testing
    private static class InMemoryUserRepository implements UserRepository {
        private final List<User> users = new ArrayList<>();
        private final AtomicLong idSequence = new AtomicLong(1);

        @Override
        public Optional<User> findByEmail(String email) {
            String target = email.trim().toLowerCase();
            return users.stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(target))
                    .findFirst();
        }

        @Override
        public boolean existsByEmail(String email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public <S extends User> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(idSequence.getAndIncrement());
                entity.setCreatedAt(Instant.now());
                entity.setUpdatedAt(Instant.now());
            } else {
                entity.setUpdatedAt(Instant.now());
                users.removeIf(u -> u.getId().equals(entity.getId()));
            }
            users.add(entity);
            return entity;
        }

        @Override public <S extends User> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public Optional<User> findById(Long aLong) { return users.stream().filter(u -> u.getId().equals(aLong)).findFirst(); }
        @Override public boolean existsById(Long aLong) { return findById(aLong).isPresent(); }
        @Override public List<User> findAll() { return new ArrayList<>(users); }
        @Override public List<User> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public long count() { return users.size(); }
        @Override public void deleteById(Long aLong) { users.removeIf(u -> u.getId().equals(aLong)); }
        @Override public void delete(User entity) { users.remove(entity); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends User> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { users.clear(); }
        @Override public void flush() {}
        @Override public <S extends User> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends User> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<User> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public User getOne(Long aLong) { return findById(aLong).orElse(null); }
        @Override public User getById(Long aLong) { return findById(aLong).orElse(null); }
        @Override public User getReferenceById(Long aLong) { return findById(aLong).orElse(null); }
        @Override public <S extends User> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> List<S> findAll(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> long count(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> boolean exists(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
        @Override public List<User> findAll(org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
        @Override public org.springframework.data.domain.Page<User> findAll(org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
    }
}
