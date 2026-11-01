package com.projectforge.controller;

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
import com.projectforge.service.AuthService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthControllerTest {

    private Validator validator;
    private AuthService authService;
    private AuthController authController;
    private TestUserRepository userRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        userRepository = new TestUserRepository();
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", 86400000);

        AuthenticationManager authManager = authentication -> {
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

        authService = new AuthService(userRepository, passwordEncoder, jwtService, authManager);
        authController = new AuthController(authService);
    }

    @Test
    @DisplayName("POST /api/auth/register with valid input should return 201 Created")
    void register_validInput_shouldReturn201() {
        RegisterRequest request = new RegisterRequest("Sarah Connor", "sarah@resistance.org", "Terminator123!");

        // Validate bean validation constraints
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());

        ResponseEntity<AuthResponse> response = authController.register(request);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("User registered successfully", response.getBody().message());
    }

    @Test
    @DisplayName("RegisterRequest validation should catch missing name, invalid email, and short password")
    void register_invalidInput_shouldFailValidation() {
        RegisterRequest missingName = new RegisterRequest("", "valid@example.com", "Password123!");
        assertFalse(validator.validate(missingName).isEmpty());

        RegisterRequest invalidEmail = new RegisterRequest("Valid Name", "not-a-valid-email", "Password123!");
        assertFalse(validator.validate(invalidEmail).isEmpty());

        RegisterRequest shortPassword = new RegisterRequest("Valid Name", "valid@example.com", "short");
        assertFalse(validator.validate(shortPassword).isEmpty());
    }

    @Test
    @DisplayName("POST /api/auth/register with duplicate email should throw EmailAlreadyExistsException")
    void register_duplicateEmail_shouldThrowException() {
        RegisterRequest request = new RegisterRequest("Original", "unique@example.com", "Password123!");
        authController.register(request);

        RegisterRequest duplicate = new RegisterRequest("Duplicate", "unique@example.com", "Password123!");
        assertThrows(EmailAlreadyExistsException.class, () -> authController.register(duplicate));
    }

    @Test
    @DisplayName("POST /api/auth/login with valid credentials should return 200 OK with Bearer token")
    void login_validCredentials_shouldReturn200WithToken() {
        authController.register(new RegisterRequest("Kyle Reese", "kyle@resistance.org", "SaveSarah123!"));

        LoginRequest loginRequest = new LoginRequest("kyle@resistance.org", "SaveSarah123!");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);
        assertTrue(violations.isEmpty());

        ResponseEntity<AuthResponse> response = authController.login(loginRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("Bearer", response.getBody().tokenType());
        assertNotNull(response.getBody().token());
    }

    @Test
    @DisplayName("POST /api/auth/login with wrong password should throw InvalidCredentialsException")
    void login_wrongPassword_shouldThrowException() {
        authController.register(new RegisterRequest("Kyle Reese", "kyle@resistance.org", "SaveSarah123!"));

        LoginRequest wrongPass = new LoginRequest("kyle@resistance.org", "IncorrectPassword123!");
        assertThrows(InvalidCredentialsException.class, () -> authController.login(wrongPass));
    }

    @Test
    @DisplayName("GET /api/auth/me should return current user info without password hash")
    void getCurrentUser_shouldReturnUserInfo() {
        authController.register(new RegisterRequest("John Connor", "john@leader.org", "LeadTheWay123!"));

        User user = userRepository.findByEmail("john@leader.org").orElseThrow();
        SecurityUser securityUser = new SecurityUser(user);

        ResponseEntity<UserResponse> response = authController.getCurrentUser(securityUser);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("John Connor", response.getBody().name());
        assertEquals("john@leader.org", response.getBody().email());
        assertEquals(Role.USER, response.getBody().role());
    }

    private static class TestUserRepository implements UserRepository {
        private final List<User> users = new ArrayList<>();
        private final AtomicLong idGen = new AtomicLong(1);

        @Override
        public Optional<User> findByEmail(String email) {
            String target = email.trim().toLowerCase();
            return users.stream().filter(u -> u.getEmail().equalsIgnoreCase(target)).findFirst();
        }

        @Override
        public boolean existsByEmail(String email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public <S extends User> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(idGen.getAndIncrement());
            }
            users.removeIf(u -> u.getId().equals(entity.getId()));
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
