package com.projectforge.service;

import com.projectforge.dto.PagedResponse;
import com.projectforge.dto.ProjectHistoryResponse;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectHistory;
import com.projectforge.entity.ProjectHistoryEventType;
import com.projectforge.entity.Role;
import com.projectforge.entity.User;
import com.projectforge.exception.ProjectNotFoundException;
import com.projectforge.repository.ProjectHistoryRepository;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectHistoryServiceTest {

    private ProjectHistoryService projectHistoryService;
    private InMemoryProjectHistoryRepository historyRepository;
    private InMemoryProjectRepository projectRepository;
    private InMemoryUserRepository userRepository;

    private User owner;
    private User otherUser;
    private Project project;

    @BeforeEach
    void setUp() {
        historyRepository = new InMemoryProjectHistoryRepository();
        projectRepository = new InMemoryProjectRepository();
        userRepository = new InMemoryUserRepository();

        projectHistoryService = new ProjectHistoryService(historyRepository, projectRepository, userRepository);

        owner = new User("Owner User", "owner@example.com", "hash", Role.USER);
        owner.setId(1L);
        userRepository.save(owner);

        otherUser = new User("Other User", "other@example.com", "hash", Role.USER);
        otherUser.setId(2L);
        userRepository.save(otherUser);

        project = new Project();
        project.setId(10L);
        project.setUser(owner);
        project.setName("Observability Platform");
        project.setDescription("Telemetry metrics aggregator");
        project.setCreatedAt(Instant.now().minusSeconds(3600));
        projectRepository.save(project);
    }

    @Test
    @DisplayName("1. Record event saves event with sanitized description and metadata")
    void recordEvent_success() {
        ProjectHistory history = projectHistoryService.recordEvent(
                project,
                ProjectHistoryEventType.PROJECT_CREATED,
                "Project 'Observability Platform' created",
                "{\"source\": \"web\"}",
                owner.getEmail()
        );

        assertThat(history).isNotNull();
        assertThat(history.getEventType()).isEqualTo(ProjectHistoryEventType.PROJECT_CREATED);
        assertThat(history.getDescription()).isEqualTo("Project 'Observability Platform' created");
        assertThat(history.getMetadata()).isEqualTo("{\"source\": \"web\"}");
        assertThat(history.getCreatedBy()).isEqualTo(owner.getEmail());
    }

    @Test
    @DisplayName("2. Record event truncates oversized metadata to 4000 chars maximum")
    void recordEvent_truncatesOversizedMetadata() {
        String hugeMetadata = "a".repeat(5000);
        ProjectHistory history = projectHistoryService.recordEvent(
                project,
                ProjectHistoryEventType.DOCUMENTS_GENERATED,
                "Documents generated",
                hugeMetadata,
                owner.getEmail()
        );

        assertThat(history).isNotNull();
        assertThat(history.getMetadata().length()).isEqualTo(4000);
        assertThat(history.getMetadata()).endsWith("...");
    }

    @Test
    @DisplayName("3. Get project history returns newest events first")
    void getProjectHistory_newestFirst() {
        ProjectHistory h1 = projectHistoryService.recordEvent(project, ProjectHistoryEventType.PROJECT_CREATED, "Created", null, owner.getEmail());
        h1.setCreatedAt(Instant.now().minusSeconds(20));
        ProjectHistory h2 = projectHistoryService.recordEvent(project, ProjectHistoryEventType.ANALYSIS_STARTED, "Analysis started", null, owner.getEmail());
        h2.setCreatedAt(Instant.now().minusSeconds(10));
        ProjectHistory h3 = projectHistoryService.recordEvent(project, ProjectHistoryEventType.PROJECT_APPROVED, "Approved", null, owner.getEmail());
        h3.setCreatedAt(Instant.now());

        PagedResponse<ProjectHistoryResponse> response = projectHistoryService.getProjectHistory(
                project.getId(), owner.getEmail(), 0, 10
        );

        assertThat(response.content()).hasSize(3);
        assertThat(response.content().get(0).eventType()).isEqualTo(ProjectHistoryEventType.PROJECT_APPROVED);
        assertThat(response.content().get(1).eventType()).isEqualTo(ProjectHistoryEventType.ANALYSIS_STARTED);
        assertThat(response.content().get(2).eventType()).isEqualTo(ProjectHistoryEventType.PROJECT_CREATED);
    }

    @Test
    @DisplayName("4. Get project history for project with 0 events seeds baseline creation event")
    void getProjectHistory_seedsBaselineIfEmpty() {
        PagedResponse<ProjectHistoryResponse> response = projectHistoryService.getProjectHistory(
                project.getId(), owner.getEmail(), 0, 10
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).eventType()).isEqualTo(ProjectHistoryEventType.PROJECT_CREATED);
        assertThat(response.content().get(0).description()).contains("Observability Platform");
    }

    @Test
    @DisplayName("5. Anti-IDOR: Cross-tenant access to history returns 404 ProjectNotFoundException")
    void getProjectHistory_crossTenant_returns404() {
        assertThatThrownBy(() -> projectHistoryService.getProjectHistory(project.getId(), otherUser.getEmail(), 0, 10))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    // --- In-Memory Repositories ---

    private static class InMemoryProjectHistoryRepository implements ProjectHistoryRepository {
        private final Map<Long, ProjectHistory> storage = new HashMap<>();
        private final AtomicLong idGen = new AtomicLong(1);

        @Override
        public Page<ProjectHistory> findAllByProjectIdOrderByCreatedAtDesc(Long projectId, Pageable pageable) {
            List<ProjectHistory> list = findAllByProjectIdOrderByCreatedAtDesc(projectId);
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), list.size());
            List<ProjectHistory> sub = start > list.size() ? List.of() : list.subList(start, end);
            return new PageImpl<>(sub, pageable, list.size());
        }

        @Override
        public List<ProjectHistory> findAllByProjectIdOrderByCreatedAtDesc(Long projectId) {
            return storage.values().stream()
                    .filter(h -> h.getProject().getId().equals(projectId))
                    .sorted((a, b) -> {
                        int c = b.getCreatedAt().compareTo(a.getCreatedAt());
                        if (c != 0) return c;
                        Long idA = a.getId() != null ? a.getId() : 0L;
                        Long idB = b.getId() != null ? b.getId() : 0L;
                        return idB.compareTo(idA);
                    })
                    .toList();
        }

        @Override
        public long countByProjectId(Long projectId) {
            return storage.values().stream().filter(h -> h.getProject().getId().equals(projectId)).count();
        }

        @Override
        public void deleteAllByProjectId(Long projectId) {
            storage.entrySet().removeIf(e -> e.getValue().getProject().getId().equals(projectId));
        }

        @Override
        public <S extends ProjectHistory> S save(S entity) {
            if (entity.getId() == null) {
                try {
                    var idField = ProjectHistory.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(entity, idGen.getAndIncrement());
                } catch (Exception ignored) {}
            }
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(Instant.now());
            }
            storage.put(entity.getId(), entity);
            return entity;
        }

        @Override public void flush() {}
        @Override public <S extends ProjectHistory> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends ProjectHistory> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<ProjectHistory> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public ProjectHistory getOne(Long aLong) { return storage.get(aLong); }
        @Override public ProjectHistory getById(Long aLong) { return storage.get(aLong); }
        @Override public ProjectHistory getReferenceById(Long aLong) { return storage.get(aLong); }
        @Override public <S extends ProjectHistory> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends ProjectHistory> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends ProjectHistory> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends ProjectHistory> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends ProjectHistory> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends ProjectHistory> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends ProjectHistory, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
        @Override public <S extends ProjectHistory> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public List<ProjectHistory> findAll() { return new ArrayList<>(storage.values()); }
        @Override public List<ProjectHistory> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public Optional<ProjectHistory> findById(Long aLong) { return Optional.ofNullable(storage.get(aLong)); }
        @Override public boolean existsById(Long aLong) { return storage.containsKey(aLong); }
        @Override public long count() { return storage.size(); }
        @Override public void deleteById(Long aLong) { storage.remove(aLong); }
        @Override public void delete(ProjectHistory entity) { storage.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends ProjectHistory> entities) {}
        @Override public void deleteAll() { storage.clear(); }
        @Override public List<ProjectHistory> findAll(Sort sort) { return findAll(); }
        @Override public Page<ProjectHistory> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
    }

    private static class InMemoryProjectRepository implements ProjectRepository {
        private final Map<Long, Project> projects = new HashMap<>();

        @Override public Optional<Project> findByIdAndUserId(Long id, Long userId) {
            Project p = projects.get(id);
            return (p != null && p.getUser().getId().equals(userId)) ? Optional.of(p) : Optional.empty();
        }
        @Override public <S extends Project> S save(S entity) {
            projects.put(entity.getId(), entity);
            return entity;
        }
        @Override public List<Project> findAllByUserIdOrderByUpdatedAtDesc(Long userId) { return projects.values().stream().filter(p -> p.getUser().getId().equals(userId)).toList(); }
        @Override public boolean existsByIdAndUserId(Long id, Long userId) { return findByIdAndUserId(id, userId).isPresent(); }
        @Override public void deleteByIdAndUserId(Long id, Long userId) { findByIdAndUserId(id, userId).ifPresent(p -> projects.remove(p.getId())); }
        @Override public long countByUserId(Long userId) { return projects.values().stream().filter(p -> p.getUser().getId().equals(userId)).count(); }
        @Override public long countByUserIdAndStatus(Long userId, com.projectforge.entity.ProjectStatus status) { return projects.values().stream().filter(p -> p.getUser().getId().equals(userId) && p.getStatus() == status).count(); }
        @Override public Page<Project> findWithFilters(Long userId, com.projectforge.entity.ProjectStatus status, com.projectforge.entity.ProjectType type, com.projectforge.entity.ProjectDifficulty difficulty, String search, Pageable pageable) { return Page.empty(); }
        @Override public List<Project> findWithFilters(Long userId, com.projectforge.entity.ProjectStatus status, com.projectforge.entity.ProjectType type, com.projectforge.entity.ProjectDifficulty difficulty, String search, Sort sort) { return List.of(); }
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
        @Override public <S extends Project> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public List<Project> findAll() { return new ArrayList<>(projects.values()); }
        @Override public List<Project> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public Optional<Project> findById(Long aLong) { return Optional.ofNullable(projects.get(aLong)); }
        @Override public boolean existsById(Long aLong) { return projects.containsKey(aLong); }
        @Override public long count() { return projects.size(); }
        @Override public void deleteById(Long aLong) { projects.remove(aLong); }
        @Override public void delete(Project entity) { projects.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends Project> entities) {}
        @Override public void deleteAll() { projects.clear(); }
        @Override public List<Project> findAll(Sort sort) { return findAll(); }
        @Override public Page<Project> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
    }

    private static class InMemoryUserRepository implements UserRepository {
        private final Map<Long, User> users = new HashMap<>();

        @Override public Optional<User> findByEmail(String email) {
            return users.values().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
        }
        @Override public boolean existsByEmail(String email) { return findByEmail(email).isPresent(); }
        @Override public <S extends User> S save(S entity) {
            users.put(entity.getId(), entity);
            return entity;
        }
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
        @Override public <S extends User> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public List<User> findAll() { return new ArrayList<>(users.values()); }
        @Override public List<User> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public Optional<User> findById(Long aLong) { return Optional.ofNullable(users.get(aLong)); }
        @Override public boolean existsById(Long aLong) { return users.containsKey(aLong); }
        @Override public long count() { return users.size(); }
        @Override public void deleteById(Long aLong) { users.remove(aLong); }
        @Override public void delete(User entity) { users.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends User> entities) {}
        @Override public void deleteAll() { users.clear(); }
        @Override public List<User> findAll(Sort sort) { return findAll(); }
        @Override public Page<User> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
    }
}
