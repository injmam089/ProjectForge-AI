package com.projectforge.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectforge.dto.ErrorResponse;
import com.projectforge.entity.Role;
import com.projectforge.entity.User;
import com.projectforge.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityFilterTest {

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private JwtService jwtService;
    private JwtAuthenticationFilter filter;
    private JwtAuthenticationEntryPoint entryPoint;
    private User testUser;
    private SecurityUser securityUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtService = new JwtService(SECRET, 3600000);

        testUser = new User("Dev User", "dev@example.com", "$2a$10$hashedpassword", Role.USER);
        testUser.setId(10L);
        securityUser = new SecurityUser(testUser);

        CustomUserDetailsService userDetailsService = new CustomUserDetailsService(new DummyUserRepository(testUser));
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        entryPoint = new JwtAuthenticationEntryPoint();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Request with valid JWT should authenticate user in SecurityContext")
    void filter_validToken_shouldAuthenticate() throws ServletException, IOException {
        String token = jwtService.generateToken(securityUser, 10L, Role.USER);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("dev@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("Request without Authorization header should not authenticate")
    void filter_noToken_shouldNotAuthenticate() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Request with malformed token should not authenticate and not throw exception")
    void filter_malformedToken_shouldNotAuthenticate() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader("Authorization", "Bearer invalid.malformed.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Request with expired token should not authenticate")
    void filter_expiredToken_shouldNotAuthenticate() throws ServletException, IOException {
        JwtService expiredJwtService = new JwtService(SECRET, -1000);
        String expiredToken = expiredJwtService.generateToken(securityUser, 10L, Role.USER);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader("Authorization", "Bearer " + expiredToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("JwtAuthenticationEntryPoint should return 401 with standard ErrorResponse JSON")
    void entryPoint_shouldReturn401Json() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/protected/resource");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Unauthorized access"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        ErrorResponse errorResponse = mapper.readValue(response.getContentAsString(), ErrorResponse.class);

        assertEquals(401, errorResponse.status());
        assertEquals("Unauthorized", errorResponse.error());
        assertEquals("Full authentication is required to access this resource", errorResponse.message());
        assertEquals("/api/protected/resource", errorResponse.path());
        assertNotNull(errorResponse.timestamp());
    }

    private static class DummyUserRepository implements UserRepository {
        private final User user;

        DummyUserRepository(User user) {
            this.user = user;
        }

        @Override
        public Optional<User> findByEmail(String email) {
            if (user != null && user.getEmail().equalsIgnoreCase(email.trim())) {
                return Optional.of(user);
            }
            return Optional.empty();
        }

        @Override public boolean existsByEmail(String email) { return findByEmail(email).isPresent(); }
        @Override public <S extends User> S save(S entity) { return entity; }
        @Override public <S extends User> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public Optional<User> findById(Long aLong) { return Optional.ofNullable(user); }
        @Override public boolean existsById(Long aLong) { return user != null; }
        @Override public List<User> findAll() { return List.of(user); }
        @Override public List<User> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public long count() { return 1; }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(User entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends User> entities) {}
        @Override public void deleteAll() {}
        @Override public void flush() {}
        @Override public <S extends User> S saveAndFlush(S entity) { return entity; }
        @Override public <S extends User> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<User> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public User getOne(Long aLong) { return user; }
        @Override public User getById(Long aLong) { return user; }
        @Override public User getReferenceById(Long aLong) { return user; }
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
