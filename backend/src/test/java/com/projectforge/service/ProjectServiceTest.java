package com.projectforge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectforge.dto.ProjectCreateRequest;
import com.projectforge.dto.ProjectResponse;
import com.projectforge.dto.ProjectSpecificationResponse;
import com.projectforge.dto.ProjectSpecificationUpdateRequest;
import com.projectforge.dto.ProjectSummaryResponse;
import com.projectforge.dto.ProjectUpdateRequest;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.ProjectType;
import com.projectforge.entity.Role;
import com.projectforge.entity.User;
import com.projectforge.exception.InvalidProjectStateException;
import com.projectforge.exception.ProjectNotFoundException;
import com.projectforge.exception.ProjectValidationException;
import com.projectforge.repository.ProjectRepository;
import com.projectforge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectServiceTest {

    private InMemoryUserRepository userRepository;
    private InMemoryProjectRepository projectRepository;
    private ObjectMapper objectMapper;
    private ProjectService projectService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        projectRepository = new InMemoryProjectRepository();
        objectMapper = new ObjectMapper();
        projectService = new ProjectService(projectRepository, userRepository, objectMapper);

        alice = new User("Alice", "alice@example.com", "hashA", Role.USER);
        alice.setId(1L);
        alice.setCreatedAt(Instant.now());
        alice.setUpdatedAt(Instant.now());
        userRepository.save(alice);

        bob = new User("Bob", "bob@example.com", "hashB", Role.USER);
        bob.setId(2L);
        bob.setCreatedAt(Instant.now());
        bob.setUpdatedAt(Instant.now());
        userRepository.save(bob);
    }

    @Test
    @DisplayName("1. Create project assigns authenticated user as owner and initializes with DRAFT status")
    void createProject_shouldSucceed() {
        ProjectCreateRequest request = new ProjectCreateRequest(
                "CyberShield",
                "Real-time threat monitoring platform for enterprise infrastructure",
                "Organizations lack automated detection",
                "SOC teams, security analysts",
                ProjectType.CYBERSECURITY_PROJECT,
                ProjectDifficulty.ADVANCED,
                "Spring Boot, React, Kafka",
                "Compliance auditing"
        );

        ProjectResponse response = projectService.createProject(request, "alice@example.com");

        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals("CyberShield", response.name());
        assertEquals(ProjectStatus.DRAFT, response.status());
        assertEquals(ProjectType.CYBERSECURITY_PROJECT, response.projectType());
        assertEquals(ProjectDifficulty.ADVANCED, response.difficulty());
        assertEquals(1L, response.ownerId());
        assertEquals("Alice", response.ownerName());
    }

    @Test
    @DisplayName("2. List user's projects returns only projects owned by the authenticated user")
    void listProjects_shouldEnforceUserIsolation() {
        projectRepository.save(new Project(alice, "Alice Project 1", "Desc 1", null, null, ProjectType.WEB_APPLICATION, ProjectDifficulty.BEGINNER, null, null));
        projectRepository.save(new Project(alice, "Alice Project 2", "Desc 2", null, null, ProjectType.API_BACKEND, ProjectDifficulty.INTERMEDIATE, null, null));
        projectRepository.save(new Project(bob, "Bob Secret Project", "Desc Bob", null, null, ProjectType.CYBERSECURITY_PROJECT, ProjectDifficulty.EXPERT, null, null));

        List<ProjectSummaryResponse> aliceProjects = projectService.getUserProjects("alice@example.com");
        assertEquals(2, aliceProjects.size());
        assertTrue(aliceProjects.stream().allMatch(p -> p.name().startsWith("Alice")));

        List<ProjectSummaryResponse> bobProjects = projectService.getUserProjects("bob@example.com");
        assertEquals(1, bobProjects.size());
        assertEquals("Bob Secret Project", bobProjects.get(0).name());
    }

    @Test
    @DisplayName("3. Retrieve owned project succeeds for owner")
    void getProjectById_ownedProject_shouldReturnDetails() {
        Project p = new Project(alice, "Alice Core", "Detailed description", "Prob", "Users", ProjectType.FULL_STACK_APPLICATION, ProjectDifficulty.ADVANCED, "AWS", "None");
        Project saved = projectRepository.save(p);

        ProjectResponse response = projectService.getProjectById(saved.getId(), "alice@example.com");
        assertNotNull(response);
        assertEquals("Alice Core", response.name());
        assertEquals(1L, response.ownerId());
    }

    @Test
    @DisplayName("4. Reject another user's project by throwing ProjectNotFoundException (IDOR Defense)")
    void getProjectById_foreignProject_shouldThrowNotFound() {
        Project bobProject = projectRepository.save(new Project(bob, "Bob Confidential", "Secret ideas", null, null, ProjectType.DATA_ANALYTICS, ProjectDifficulty.EXPERT, null, null));

        assertThrows(ProjectNotFoundException.class, () ->
                projectService.getProjectById(bobProject.getId(), "alice@example.com")
        );
    }

    @Test
    @DisplayName("5. Update owned project modifies fields when in editable status")
    void updateProject_ownedProject_shouldSucceed() {
        Project saved = projectRepository.save(new Project(alice, "Original Name", "Original idea description", null, null, ProjectType.WEB_APPLICATION, ProjectDifficulty.BEGINNER, null, null));

        ProjectUpdateRequest updateReq = new ProjectUpdateRequest(
                "Updated Name",
                "Updated description with more details",
                "New Problem",
                "New Users",
                ProjectType.FULL_STACK_APPLICATION,
                ProjectDifficulty.INTERMEDIATE,
                "React, Java",
                "Strict audit logging"
        );

        ProjectResponse updated = projectService.updateProject(saved.getId(), updateReq, "alice@example.com");
        assertEquals("Updated Name", updated.name());
        assertEquals("Updated description with more details", updated.description());
        assertEquals(ProjectType.FULL_STACK_APPLICATION, updated.projectType());
        assertEquals(ProjectDifficulty.INTERMEDIATE, updated.difficulty());
    }

    @Test
    @DisplayName("6. Delete owned project succeeds and removes project from persistence")
    void deleteProject_ownedProject_shouldSucceed() {
        Project saved = projectRepository.save(new Project(alice, "To Delete", "Delete description", null, null, ProjectType.WEB_APPLICATION, ProjectDifficulty.BEGINNER, null, null));

        projectService.deleteProject(saved.getId(), "alice@example.com");

        assertTrue(projectRepository.findByIdAndUserId(saved.getId(), alice.getId()).isEmpty());
    }

    @Test
    @DisplayName("7. Prepare analysis validates requirements and transitions status to READY_FOR_ANALYSIS")
    void prepareForAnalysis_shouldTransitionStatus() {
        Project saved = projectRepository.save(new Project(
                alice,
                "Valid Project",
                "Detailed software project description meeting requirements",
                "Problem statement",
                "Developers",
                ProjectType.WEB_APPLICATION,
                ProjectDifficulty.INTERMEDIATE,
                "Spring Boot",
                "None"
        ));

        ProjectResponse response = projectService.prepareForAnalysis(saved.getId(), "alice@example.com");

        assertEquals(ProjectStatus.READY_FOR_ANALYSIS, response.status());
        assertEquals(ProjectStatus.READY_FOR_ANALYSIS, projectRepository.findById(saved.getId()).get().getStatus());
    }

    @Test
    @DisplayName("8. Reject invalid analysis preparation when description is too short")
    void prepareForAnalysis_whenDescriptionTooShort_shouldThrowValidationException() {
        Project saved = projectRepository.save(new Project(
                alice,
                "Short",
                "Too short",
                null,
                null,
                ProjectType.WEB_APPLICATION,
                ProjectDifficulty.BEGINNER,
                null,
                null
        ));

        assertThrows(ProjectValidationException.class, () ->
                projectService.prepareForAnalysis(saved.getId(), "alice@example.com")
        );
    }

    @Test
    @DisplayName("9. Verify status transitions: reject update when project is in a non-editable future status")
    void updateProject_whenStatusLocked_shouldThrowInvalidProjectStateException() {
        Project saved = projectRepository.save(new Project(
                alice,
                "Locked Project",
                "Locked project description",
                null,
                null,
                ProjectType.WEB_APPLICATION,
                ProjectDifficulty.BEGINNER,
                null,
                null
        ));
        saved.setStatus(ProjectStatus.GENERATED);
        projectRepository.save(saved);

        ProjectUpdateRequest updateReq = new ProjectUpdateRequest(
                "Attempt Name",
                "Attempt description update",
                null,
                null,
                ProjectType.WEB_APPLICATION,
                ProjectDifficulty.BEGINNER,
                null,
                null
        );

        assertThrows(InvalidProjectStateException.class, () ->
                projectService.updateProject(saved.getId(), updateReq, "alice@example.com")
        );
    }

    // In-memory repositories for fast, standalone unit execution

    private static class InMemoryUserRepository implements UserRepository {
        private final Map<Long, User> users = new HashMap<>();

        @Override
        public Optional<User> findByEmail(String email) {
            return users.values().stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(email))
                    .findFirst();
        }

        @Override
        public boolean existsByEmail(String email) {
            return users.values().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
        }

        @Override
        public <S extends User> S save(S entity) {
            users.put(entity.getId(), entity);
            return entity;
        }

        @Override public <S extends User> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public Optional<User> findById(Long aLong) { return Optional.ofNullable(users.get(aLong)); }
        @Override public boolean existsById(Long aLong) { return users.containsKey(aLong); }
        @Override public List<User> findAll() { return new ArrayList<>(users.values()); }
        @Override public List<User> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public long count() { return users.size(); }
        @Override public void deleteById(Long aLong) { users.remove(aLong); }
        @Override public void delete(User entity) { users.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends User> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { users.clear(); }
        @Override public void flush() {}
        @Override public <S extends User> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends User> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<User> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public User getOne(Long aLong) { return users.get(aLong); }
        @Override public User getById(Long aLong) { return users.get(aLong); }
        @Override public User getReferenceById(Long aLong) { return users.get(aLong); }
        @Override public <S extends User> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
        @Override public List<User> findAll(Sort sort) { return findAll(); }
        @Override public Page<User> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
    }

    private static class InMemoryProjectRepository implements ProjectRepository {
        private final Map<Long, Project> projects = new HashMap<>();
        private final AtomicLong idGen = new AtomicLong(1);

        @Override
        public List<Project> findAllByUserIdOrderByUpdatedAtDesc(Long userId) {
            return projects.values().stream()
                    .filter(p -> p.getUser().getId().equals(userId))
                    .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                    .toList();
        }

        @Override
        public Optional<Project> findByIdAndUserId(Long id, Long userId) {
            Project p = projects.get(id);
            if (p != null && p.getUser().getId().equals(userId)) {
                return Optional.of(p);
            }
            return Optional.empty();
        }

        @Override
        public boolean existsByIdAndUserId(Long id, Long userId) {
            return findByIdAndUserId(id, userId).isPresent();
        }

        @Override
        public void deleteByIdAndUserId(Long id, Long userId) {
            findByIdAndUserId(id, userId).ifPresent(p -> projects.remove(p.getId()));
        }

        @Override
        public long countByUserId(Long userId) {
            return projects.values().stream().filter(p -> p.getUser().getId().equals(userId)).count();
        }

        @Override
        public long countByUserIdAndStatus(Long userId, ProjectStatus status) {
            return projects.values().stream().filter(p -> p.getUser().getId().equals(userId) && p.getStatus() == status).count();
        }

        @Override
        public Page<Project> findWithFilters(Long userId, ProjectStatus status, ProjectType type, ProjectDifficulty difficulty, String search, Pageable pageable) {
            List<Project> filtered = findWithFilters(userId, status, type, difficulty, search, pageable.getSort());
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filtered.size());
            List<Project> sub = start > filtered.size() ? List.of() : filtered.subList(start, end);
            return new PageImpl<>(sub, pageable, filtered.size());
        }

        @Override
        public List<Project> findWithFilters(Long userId, ProjectStatus status, ProjectType type, ProjectDifficulty difficulty, String search, Sort sort) {
            return projects.values().stream()
                    .filter(p -> p.getUser().getId().equals(userId))
                    .filter(p -> status == null || p.getStatus() == status)
                    .filter(p -> type == null || p.getProjectType() == type)
                    .filter(p -> difficulty == null || p.getDifficulty() == difficulty)
                    .filter(p -> search == null || search.isBlank() || p.getName().toLowerCase().contains(search.toLowerCase()) || (p.getDescription() != null && p.getDescription().toLowerCase().contains(search.toLowerCase())))
                    .toList();
        }

        @Override
        public <S extends Project> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(idGen.getAndIncrement());
            }
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(Instant.now());
            }
            entity.setUpdatedAt(Instant.now());
            projects.put(entity.getId(), entity);
            return entity;
        }

        @Override public <S extends Project> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public Optional<Project> findById(Long aLong) { return Optional.ofNullable(projects.get(aLong)); }
        @Override public boolean existsById(Long aLong) { return projects.containsKey(aLong); }
        @Override public List<Project> findAll() { return new ArrayList<>(projects.values()); }
        @Override public List<Project> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public long count() { return projects.size(); }
        @Override public void deleteById(Long aLong) { projects.remove(aLong); }
        @Override public void delete(Project entity) { projects.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends Project> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { projects.clear(); }
        @Override public void flush() {}
        @Override public <S extends Project> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Project> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<Project> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public Project getOne(Long aLong) { return projects.get(aLong); }
        @Override public Project getById(Long aLong) { return projects.get(aLong); }
        @Override public Project getReferenceById(Long aLong) { return projects.get(aLong); }
        @Override public <S extends Project> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Project> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Project> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends Project> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends Project> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Project> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Project, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
        @Override public List<Project> findAll(Sort sort) { return findAll(); }
        @Override public Page<Project> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
    }
}
