package com.projectforge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectforge.dto.PagedResponse;
import com.projectforge.dto.ProjectStatsResponse;
import com.projectforge.dto.ProjectSummaryResponse;
import com.projectforge.entity.Document;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectHistory;
import com.projectforge.entity.ProjectHistoryEventType;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.ProjectType;
import com.projectforge.entity.Role;
import com.projectforge.entity.User;
import com.projectforge.exception.ProjectNotFoundException;
import com.projectforge.repository.DocumentRepository;
import com.projectforge.repository.DocumentVersionRepository;
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

class ProjectDashboardServiceTest {

    private ProjectService projectService;
    private InMemoryProjectRepository projectRepository;
    private InMemoryUserRepository userRepository;
    private InMemoryDocumentRepository documentRepository;
    private InMemoryDocumentVersionRepository documentVersionRepository;
    private InMemoryProjectHistoryRepository projectHistoryRepository;

    private User userAlpha;
    private User userBeta;

    @BeforeEach
    void setUp() {
        projectRepository = new InMemoryProjectRepository();
        userRepository = new InMemoryUserRepository();
        documentRepository = new InMemoryDocumentRepository();
        documentVersionRepository = new InMemoryDocumentVersionRepository();
        projectHistoryRepository = new InMemoryProjectHistoryRepository();

        userAlpha = new User("User Alpha", "alpha@test.com", "hash", Role.USER);
        userAlpha.setId(1L);
        userRepository.save(userAlpha);

        userBeta = new User("User Beta", "beta@test.com", "hash", Role.USER);
        userBeta.setId(2L);
        userRepository.save(userBeta);

        projectService = new ProjectService(
                projectRepository,
                userRepository,
                new ObjectMapper(),
                documentRepository,
                documentVersionRepository,
                projectHistoryRepository,
                null
        );

        // Seed Alpha projects
        createProject(1L, userAlpha, "CloudPulse Telemetry", "Metrics ingestion platform", ProjectType.API_BACKEND, ProjectDifficulty.ADVANCED, ProjectStatus.GENERATED, Instant.now().minusSeconds(100));
        createProject(2L, userAlpha, "CyberShield Guard", "Zero-trust auth service", ProjectType.CYBERSECURITY_PROJECT, ProjectDifficulty.INTERMEDIATE, ProjectStatus.APPROVED, Instant.now().minusSeconds(200));
        createProject(3L, userAlpha, "DataLens Analytics", "Business intelligence dashboard", ProjectType.DATA_ANALYTICS, ProjectDifficulty.BEGINNER, ProjectStatus.DRAFT, Instant.now().minusSeconds(300));

        // Seed Beta project (for isolation checks)
        createProject(4L, userBeta, "Beta Secret App", "Confidential beta tool", ProjectType.MOBILE_APPLICATION, ProjectDifficulty.EXPERT, ProjectStatus.DRAFT, Instant.now().minusSeconds(50));
    }

    private void createProject(Long id, User user, String name, String desc, ProjectType type, ProjectDifficulty diff, ProjectStatus status, Instant updatedAt) {
        Project p = new Project();
        p.setId(id);
        p.setUser(user);
        p.setName(name);
        p.setDescription(desc);
        p.setProjectType(type);
        p.setDifficulty(diff);
        p.setStatus(status);
        p.setCreatedAt(updatedAt.minusSeconds(1000));
        p.setUpdatedAt(updatedAt);
        p.setProjectSpecification("{\"qualityScore\": 92}");
        projectRepository.save(p);
    }

    @Test
    @DisplayName("1. User-scoped search: Alpha only sees Alpha projects, not Beta's")
    @SuppressWarnings("unchecked")
    void search_userScoped() {
        List<ProjectSummaryResponse> results = (List<ProjectSummaryResponse>) projectService.searchAndFilterProjects(
                userAlpha.getEmail(), null, null, null, null, "updatedAt", "desc", null, null
        );

        assertThat(results).hasSize(3);
        assertThat(results).extracting(ProjectSummaryResponse::name)
                .containsExactly("CloudPulse Telemetry", "CyberShield Guard", "DataLens Analytics");
    }

    @Test
    @DisplayName("2. Keyword search in name or description")
    @SuppressWarnings("unchecked")
    void search_byKeyword() {
        List<ProjectSummaryResponse> results = (List<ProjectSummaryResponse>) projectService.searchAndFilterProjects(
                userAlpha.getEmail(), "auth", null, null, null, "updatedAt", "desc", null, null
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("CyberShield Guard");
    }

    @Test
    @DisplayName("3. Filter by ProjectStatus")
    @SuppressWarnings("unchecked")
    void filter_byStatus() {
        List<ProjectSummaryResponse> results = (List<ProjectSummaryResponse>) projectService.searchAndFilterProjects(
                userAlpha.getEmail(), null, ProjectStatus.GENERATED, null, null, "updatedAt", "desc", null, null
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("CloudPulse Telemetry");
    }

    @Test
    @DisplayName("4. Filter by ProjectType and Difficulty")
    @SuppressWarnings("unchecked")
    void filter_byTypeAndDifficulty() {
        List<ProjectSummaryResponse> results = (List<ProjectSummaryResponse>) projectService.searchAndFilterProjects(
                userAlpha.getEmail(), null, null, ProjectType.DATA_ANALYTICS, ProjectDifficulty.BEGINNER, "updatedAt", "desc", null, null
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("DataLens Analytics");
    }

    @Test
    @DisplayName("5. Server-side pagination returns PagedResponse")
    @SuppressWarnings("unchecked")
    void pagination_returnsPagedResponse() {
        PagedResponse<ProjectSummaryResponse> page = (PagedResponse<ProjectSummaryResponse>) projectService.searchAndFilterProjects(
                userAlpha.getEmail(), null, null, null, null, "updatedAt", "desc", 0, 2
        );

        assertThat(page.page()).isEqualTo(0);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.content()).hasSize(2);
        assertThat(page.first()).isTrue();
        assertThat(page.last()).isFalse();
    }

    @Test
    @DisplayName("6. Unsafe sort field falls back safely to updatedAt (no SQL injection)")
    @SuppressWarnings("unchecked")
    void sorting_safeAllowlistFallback() {
        List<ProjectSummaryResponse> results = (List<ProjectSummaryResponse>) projectService.searchAndFilterProjects(
                userAlpha.getEmail(), null, null, null, null, "'; DROP TABLE projects; --", "desc", null, null
        );

        assertThat(results).hasSize(3);
        assertThat(results.get(0).name()).isEqualTo("CloudPulse Telemetry");
    }

    @Test
    @DisplayName("7. Project statistics counts only user's projects and statuses")
    void getProjectStats_userScoped() {
        ProjectStatsResponse stats = projectService.getProjectStats(userAlpha.getEmail());

        assertThat(stats.totalProjects()).isEqualTo(3);
        assertThat(stats.draft()).isEqualTo(1);
        assertThat(stats.approved()).isEqualTo(1);
        assertThat(stats.generated()).isEqualTo(1);
        assertThat(stats.exported()).isEqualTo(0);
    }

    @Test
    @DisplayName("8. Cascade delete cleanly removes child documents, versions, and history")
    void deleteProject_cascadeClean() {
        Project p = projectRepository.findById(1L).get();
        Document doc = new Document(p, DocumentType.README, "README.md", "# Test");
        doc.setId(100L);
        documentRepository.save(doc);

        ProjectHistory hist = new ProjectHistory(p, ProjectHistoryEventType.PROJECT_CREATED, "Created", null, "alpha@test.com");
        hist.setCreatedAt(Instant.now());
        projectHistoryRepository.save(hist);

        projectService.deleteProject(1L, userAlpha.getEmail());

        assertThat(projectRepository.findById(1L)).isEmpty();
        assertThat(documentRepository.findAllByProjectIdOrderByFilenameAsc(1L)).isEmpty();
        assertThat(projectHistoryRepository.findAllByProjectIdOrderByCreatedAtDesc(1L)).isEmpty();
    }

    @Test
    @DisplayName("9. Anti-IDOR: Deleting another user's project throws 404")
    void deleteProject_crossTenant_returns404() {
        assertThatThrownBy(() -> projectService.deleteProject(4L, userAlpha.getEmail()))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    // --- In-Memory Repositories ---

    private static class InMemoryProjectRepository implements ProjectRepository {
        private final Map<Long, Project> projects = new HashMap<>();

        @Override public Optional<Project> findById(Long id) { return Optional.ofNullable(projects.get(id)); }
        @Override public Optional<Project> findByIdAndUserId(Long id, Long userId) {
            Project p = projects.get(id);
            return (p != null && p.getUser().getId().equals(userId)) ? Optional.of(p) : Optional.empty();
        }
        @Override public <S extends Project> S save(S entity) {
            projects.put(entity.getId(), entity);
            return entity;
        }
        @Override public void delete(Project entity) { projects.remove(entity.getId()); }
        @Override public List<Project> findAllByUserIdOrderByUpdatedAtDesc(Long userId) {
            return projects.values().stream()
                    .filter(p -> p.getUser().getId().equals(userId))
                    .sorted(Comparator.comparing(Project::getUpdatedAt).reversed())
                    .toList();
        }
        @Override public boolean existsByIdAndUserId(Long id, Long userId) { return findByIdAndUserId(id, userId).isPresent(); }
        @Override public void deleteByIdAndUserId(Long id, Long userId) { findByIdAndUserId(id, userId).ifPresent(p -> projects.remove(p.getId())); }
        @Override public long countByUserId(Long userId) { return projects.values().stream().filter(p -> p.getUser().getId().equals(userId)).count(); }
        @Override public long countByUserIdAndStatus(Long userId, ProjectStatus status) { return projects.values().stream().filter(p -> p.getUser().getId().equals(userId) && p.getStatus() == status).count(); }

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
                    .sorted((a, b) -> {
                        Sort.Order order = sort.iterator().hasNext() ? sort.iterator().next() : null;
                        if (order == null || order.getProperty().equals("updatedAt")) {
                            int cmp = a.getUpdatedAt().compareTo(b.getUpdatedAt());
                            return order != null && order.isAscending() ? cmp : -cmp;
                        } else if (order.getProperty().equals("createdAt")) {
                            int cmp = a.getCreatedAt().compareTo(b.getCreatedAt());
                            return order.isAscending() ? cmp : -cmp;
                        } else if (order.getProperty().equals("name")) {
                            int cmp = a.getName().compareToIgnoreCase(b.getName());
                            return order.isAscending() ? cmp : -cmp;
                        } else if (order.getProperty().equals("status")) {
                            int cmp = a.getStatus().name().compareTo(b.getStatus().name());
                            return order.isAscending() ? cmp : -cmp;
                        }
                        return 0;
                    })
                    .toList();
        }

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
        @Override public boolean existsById(Long aLong) { return projects.containsKey(aLong); }
        @Override public long count() { return projects.size(); }
        @Override public void deleteById(Long aLong) { projects.remove(aLong); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends Project> entities) {}
        @Override public void deleteAll() { projects.clear(); }
        @Override public List<Project> findAll(Sort sort) { return findAll(); }
        @Override public Page<Project> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
    }

    private static class InMemoryDocumentRepository implements DocumentRepository {
        private final Map<Long, Document> storage = new HashMap<>();

        @Override public List<Document> findAllByProjectIdOrderByFilenameAsc(Long projectId) {
            return storage.values().stream().filter(d -> d.getProject().getId().equals(projectId)).toList();
        }
        @Override public void deleteAllByProjectId(Long projectId) {
            storage.entrySet().removeIf(e -> e.getValue().getProject().getId().equals(projectId));
        }
        @Override public long countByProjectId(Long projectId) {
            return storage.values().stream().filter(d -> d.getProject().getId().equals(projectId)).count();
        }
        @Override public List<Object[]> countByProjectIds(List<Long> projectIds) {
            Map<Long, Long> counts = storage.values().stream()
                    .filter(d -> projectIds.contains(d.getProject().getId()))
                    .collect(java.util.stream.Collectors.groupingBy(d -> d.getProject().getId(), java.util.stream.Collectors.counting()));
            return counts.entrySet().stream()
                    .map(e -> new Object[]{ e.getKey(), e.getValue() })
                    .toList();
        }
        @Override public <S extends Document> S save(S entity) {
            storage.put(entity.getId(), entity);
            return entity;
        }
        @Override public Optional<Document> findByIdAndProjectUserId(Long documentId, Long userId) { return Optional.empty(); }
        @Override public Optional<Document> findByIdWithLock(Long id) { return Optional.empty(); }
        @Override public Optional<Document> findByProjectIdAndFilename(Long projectId, String filename) { return Optional.empty(); }
        @Override public void flush() {}
        @Override public <S extends Document> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Document> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<Document> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public Document getOne(Long aLong) { return storage.get(aLong); }
        @Override public Document getById(Long aLong) { return storage.get(aLong); }
        @Override public Document getReferenceById(Long aLong) { return storage.get(aLong); }
        @Override public <S extends Document> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public List<Document> findAll() { return new ArrayList<>(storage.values()); }
        @Override public List<Document> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public Optional<Document> findById(Long aLong) { return Optional.ofNullable(storage.get(aLong)); }
        @Override public boolean existsById(Long aLong) { return storage.containsKey(aLong); }
        @Override public long count() { return storage.size(); }
        @Override public void deleteById(Long aLong) { storage.remove(aLong); }
        @Override public void delete(Document entity) { storage.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends Document> entities) {}
        @Override public void deleteAll() { storage.clear(); }
        @Override public List<Document> findAll(Sort sort) { return findAll(); }
        @Override public Page<Document> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
    }

    private static class InMemoryDocumentVersionRepository implements DocumentVersionRepository {
        @Override public int findMaxVersionByDocumentId(Long documentId) { return 0; }
        @Override public long countByDocumentId(Long documentId) { return 0; }
        @Override public void deleteAllByProjectId(Long projectId) {}
        @Override public void deleteAllByDocumentId(Long documentId) {}
        @Override public List<com.projectforge.entity.DocumentVersion> findAllByDocumentIdOrderByVersionDesc(Long documentId) { return List.of(); }
        @Override public Optional<com.projectforge.entity.DocumentVersion> findByDocumentIdAndVersion(Long documentId, int version) { return Optional.empty(); }
        @Override public <S extends com.projectforge.entity.DocumentVersion> S save(S entity) { return entity; }
        @Override public void flush() {}
        @Override public <S extends com.projectforge.entity.DocumentVersion> S saveAndFlush(S entity) { return entity; }
        @Override public <S extends com.projectforge.entity.DocumentVersion> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<com.projectforge.entity.DocumentVersion> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public com.projectforge.entity.DocumentVersion getOne(Long aLong) { return null; }
        @Override public com.projectforge.entity.DocumentVersion getById(Long aLong) { return null; }
        @Override public com.projectforge.entity.DocumentVersion getReferenceById(Long aLong) { return null; }
        @Override public <S extends com.projectforge.entity.DocumentVersion> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends com.projectforge.entity.DocumentVersion> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends com.projectforge.entity.DocumentVersion> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends com.projectforge.entity.DocumentVersion> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends com.projectforge.entity.DocumentVersion> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends com.projectforge.entity.DocumentVersion> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends com.projectforge.entity.DocumentVersion, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
        @Override public <S extends com.projectforge.entity.DocumentVersion> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public List<com.projectforge.entity.DocumentVersion> findAll() { return List.of(); }
        @Override public List<com.projectforge.entity.DocumentVersion> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public Optional<com.projectforge.entity.DocumentVersion> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(com.projectforge.entity.DocumentVersion entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends com.projectforge.entity.DocumentVersion> entities) {}
        @Override public void deleteAll() {}
        @Override public List<com.projectforge.entity.DocumentVersion> findAll(Sort sort) { return List.of(); }
        @Override public Page<com.projectforge.entity.DocumentVersion> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
    }

    private static class InMemoryProjectHistoryRepository implements ProjectHistoryRepository {
        private final Map<Long, ProjectHistory> storage = new HashMap<>();
        private final AtomicLong idGen = new AtomicLong(1);

        @Override public void deleteAllByProjectId(Long projectId) {
            storage.entrySet().removeIf(e -> e.getValue().getProject().getId().equals(projectId));
        }
        @Override public List<ProjectHistory> findAllByProjectIdOrderByCreatedAtDesc(Long projectId) {
            return storage.values().stream().filter(h -> h.getProject().getId().equals(projectId)).toList();
        }
        @Override public Page<ProjectHistory> findAllByProjectIdOrderByCreatedAtDesc(Long projectId, Pageable pageable) { return Page.empty(); }
        @Override public long countByProjectId(Long projectId) {
            return storage.values().stream().filter(h -> h.getProject().getId().equals(projectId)).count();
        }
        @Override public <S extends ProjectHistory> S save(S entity) {
            if (entity.getId() == null) {
                try {
                    var f = ProjectHistory.class.getDeclaredField("id");
                    f.setAccessible(true);
                    f.set(entity, idGen.getAndIncrement());
                } catch (Exception ignored) {}
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
        @Override public ProjectHistory getOne(Long aLong) { return null; }
        @Override public ProjectHistory getById(Long aLong) { return null; }
        @Override public ProjectHistory getReferenceById(Long aLong) { return null; }
        @Override public <S extends ProjectHistory> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends ProjectHistory> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends ProjectHistory> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends ProjectHistory> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends ProjectHistory> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends ProjectHistory> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends ProjectHistory, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
        @Override public <S extends ProjectHistory> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public List<ProjectHistory> findAll() { return List.of(); }
        @Override public List<ProjectHistory> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public Optional<ProjectHistory> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(ProjectHistory entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends ProjectHistory> entities) {}
        @Override public void deleteAll() {}
        @Override public List<ProjectHistory> findAll(Sort sort) { return List.of(); }
        @Override public Page<ProjectHistory> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
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
        @Override public User getOne(Long aLong) { return null; }
        @Override public User getById(Long aLong) { return null; }
        @Override public User getReferenceById(Long aLong) { return null; }
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
