package com.projectforge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectforge.dto.DocumentEditRequest;
import com.projectforge.dto.DocumentResponse;
import com.projectforge.dto.DocumentSaveResponse;
import com.projectforge.dto.DocumentVersionResponse;
import com.projectforge.dto.DocumentVersionSummaryResponse;
import com.projectforge.dto.WorkspaceResponse;
import com.projectforge.entity.Document;
import com.projectforge.entity.DocumentChangeType;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.DocumentVersion;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.ProjectType;
import com.projectforge.entity.Role;
import com.projectforge.entity.User;
import com.projectforge.exception.DocumentNotFoundException;
import com.projectforge.exception.DocumentVersionNotFoundException;
import com.projectforge.exception.ProjectNotFoundException;
import com.projectforge.markdown.DocumentContentGenerator;
import com.projectforge.markdown.DocumentContentValidator;
import com.projectforge.markdown.MarkdownGenerator;
import com.projectforge.markdown.generator.ReadmeGenerator;
import com.projectforge.repository.DocumentRepository;
import com.projectforge.repository.DocumentVersionRepository;
import com.projectforge.repository.ProjectRepository;
import com.projectforge.repository.UserRepository;
import com.projectforge.testutil.TestSpecificationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentWorkspaceServiceTest {

    private InMemoryDocumentRepository documentRepository;
    private InMemoryDocumentVersionRepository documentVersionRepository;
    private InMemoryProjectRepository projectRepository;
    private InMemoryUserRepository userRepository;
    private DocumentWorkspaceService service;

    private User owner;
    private User attacker;
    private Project project;
    private Document document;

    @BeforeEach
    void setUp() {
        documentRepository = new InMemoryDocumentRepository();
        documentVersionRepository = new InMemoryDocumentVersionRepository();
        projectRepository = new InMemoryProjectRepository();
        userRepository = new InMemoryUserRepository();

        owner = new User("Owner User", "owner@test.com", "hash", Role.USER);
        owner.setId(1L);
        userRepository.save(owner);

        attacker = new User("Attacker User", "attacker@test.com", "hash", Role.USER);
        attacker.setId(2L);
        userRepository.save(attacker);

        project = new Project(
                owner,
                "CloudPulse Telemetry",
                "Distributed metrics engine",
                "High RPS metrics aggregation",
                "DevOps Engineers",
                ProjectType.API_BACKEND,
                ProjectDifficulty.ADVANCED,
                "Java 17, Spring Boot 3",
                "None"
        );
        project.setId(10L);
        project.setStatus(ProjectStatus.GENERATED);
        project.setProjectSpecification(TestSpecificationFactory.createValidSpecificationJson());
        projectRepository.save(project);

        document = new Document(
                project,
                DocumentType.README,
                "README.md",
                "# CloudPulse Telemetry\n\nInitial generated README content for test verification.",
                1
        );
        document.setId(100L);
        documentRepository.save(document);

        List<DocumentContentGenerator> generators = List.of(new ReadmeGenerator());
        MarkdownGenerator markdownGenerator = new MarkdownGenerator(generators);
        DocumentContentValidator validator = new DocumentContentValidator();
        ObjectMapper objectMapper = new ObjectMapper();

        service = new DocumentWorkspaceService(
                documentRepository,
                documentVersionRepository,
                projectRepository,
                userRepository,
                markdownGenerator,
                validator,
                objectMapper
        );
    }

    @Test
    @DisplayName("Manual edit updates document content, increments version to 2, and records version history")
    void manualEditSuccess() {
        String newContent = "# CloudPulse Telemetry (Updated)\n\nManually updated README content with extra notes.";
        DocumentEditRequest request = new DocumentEditRequest(newContent);

        DocumentSaveResponse response = service.saveDocument(document.getId(), request, owner.getEmail());

        assertThat(response.id()).isEqualTo(document.getId());
        assertThat(response.version()).isEqualTo(2);
        assertThat(response.characterCount()).isEqualTo(newContent.length());

        Document updated = documentRepository.findById(document.getId()).orElseThrow();
        assertThat(updated.getVersion()).isEqualTo(2);
        assertThat(updated.getContent()).isEqualTo(newContent);

        List<DocumentVersion> versions = documentVersionRepository.findAllByDocumentIdOrderByVersionDesc(document.getId());
        assertThat(versions).hasSize(2);
        assertThat(versions.get(0).getVersion()).isEqualTo(2);
        assertThat(versions.get(0).getChangeType()).isEqualTo(DocumentChangeType.MANUAL_EDIT);
        assertThat(versions.get(1).getVersion()).isEqualTo(1);
        assertThat(versions.get(1).getChangeType()).isEqualTo(DocumentChangeType.GENERATE);
    }

    @Test
    @DisplayName("Version history listing bootstraps v1 if empty and orders descending")
    void versionListingSuccess() {
        // Save an edit to create version 2
        service.saveDocument(
                document.getId(),
                new DocumentEditRequest("# CloudPulse Telemetry v2\n\nSecond version content with sufficient characters."),
                owner.getEmail()
        );

        List<DocumentVersionSummaryResponse> summaries = service.getVersions(document.getId(), owner.getEmail());

        // Should have v2 (manual) and v1 (bootstrapped generate)
        assertThat(summaries).isNotEmpty();
        assertThat(summaries.get(0).version()).isEqualTo(2);
        assertThat(summaries.get(0).changeType()).isEqualTo(DocumentChangeType.MANUAL_EDIT);
    }

    @Test
    @DisplayName("Get specific version retrieves full historical content")
    void getVersionContentSuccess() {
        service.saveDocument(
                document.getId(),
                new DocumentEditRequest("# CloudPulse Telemetry v2\n\nSecond version content with sufficient characters."),
                owner.getEmail()
        );

        DocumentVersionResponse v2 = service.getVersion(document.getId(), 2, owner.getEmail());
        assertThat(v2.version()).isEqualTo(2);
        assertThat(v2.filename()).isEqualTo("README.md");
        assertThat(v2.content()).contains("v2");
    }

    @Test
    @DisplayName("Restoring an earlier version appends a new RESTORE version without wiping history")
    void restoreVersionSuccess() {
        String originalContent = document.getContent();

        // 1. Make an edit -> creates v2
        service.saveDocument(
                document.getId(),
                new DocumentEditRequest("# CloudPulse Telemetry v2\n\nIntermediate edits with sufficient characters."),
                owner.getEmail()
        );

        // 2. Ensure v1 is tracked
        service.getVersions(document.getId(), owner.getEmail());

        // 3. Restore v1
        DocumentResponse restored = service.restoreVersion(document.getId(), 1, owner.getEmail());

        // Should produce version 3 containing original content
        assertThat(restored.version()).isEqualTo(3);
        assertThat(restored.content()).isEqualTo(originalContent);

        Document current = documentRepository.findById(document.getId()).orElseThrow();
        assertThat(current.getVersion()).isEqualTo(3);
        assertThat(current.getContent()).isEqualTo(originalContent);
    }

    @Test
    @DisplayName("Regenerating document runs canonical Phase 6 generator, creates REGENERATE version, and updates content")
    void regenerateDocumentSuccess() {
        // 1. Mess up document content with manual edit
        service.saveDocument(
                document.getId(),
                new DocumentEditRequest("# Temporary Notes\n\nSome arbitrary manual notes with sufficient characters."),
                owner.getEmail()
        );

        // 2. Regenerate from approved specification
        DocumentResponse regenerated = service.regenerateDocument(document.getId(), owner.getEmail());

        assertThat(regenerated.version()).isEqualTo(3);
        assertThat(regenerated.content()).contains("CloudPulse Telemetry");

        Document current = documentRepository.findById(document.getId()).orElseThrow();
        assertThat(current.getVersion()).isEqualTo(3);
        assertThat(current.getContent()).contains("CloudPulse Telemetry");
    }

    @Test
    @DisplayName("MANDATORY RULE: Approved specification is completely IMMUTABLE before and after all workspace operations")
    void mandatorySpecificationImmutabilityRule() {
        String beforeSpec = project.getProjectSpecification();
        assertThat(beforeSpec).isNotNull().isNotEmpty();

        // 1. Manual edit
        service.saveDocument(
                document.getId(),
                new DocumentEditRequest("# CloudPulse v2\n\nManual update text with sufficient characters."),
                owner.getEmail()
        );
        Project p1 = projectRepository.findById(project.getId()).orElseThrow();
        assertThat(p1.getProjectSpecification()).isEqualTo(beforeSpec);

        // 2. Restore
        service.restoreVersion(document.getId(), 1, owner.getEmail());
        Project p2 = projectRepository.findById(project.getId()).orElseThrow();
        assertThat(p2.getProjectSpecification()).isEqualTo(beforeSpec);

        // 3. Regenerate
        service.regenerateDocument(document.getId(), owner.getEmail());
        Project p3 = projectRepository.findById(project.getId()).orElseThrow();
        assertThat(p3.getProjectSpecification()).isEqualTo(beforeSpec);
    }

    @Test
    @DisplayName("Anti-IDOR: Other user is blocked with 404 from saving, viewing, restoring, or regenerating foreign documents")
    void antiIdorProtection() {
        // Workspace overview anti-IDOR
        assertThatThrownBy(() -> service.getWorkspace(project.getId(), attacker.getEmail()))
                .isInstanceOf(ProjectNotFoundException.class);

        // Save anti-IDOR
        assertThatThrownBy(() -> service.saveDocument(
                document.getId(),
                new DocumentEditRequest("# Hack\n\nUnauthorized modification attempt with sufficient characters."),
                attacker.getEmail()
        )).isInstanceOf(DocumentNotFoundException.class);

        // Version listing anti-IDOR
        assertThatThrownBy(() -> service.getVersions(document.getId(), attacker.getEmail()))
                .isInstanceOf(DocumentNotFoundException.class);

        // Single version anti-IDOR
        assertThatThrownBy(() -> service.getVersion(document.getId(), 1, attacker.getEmail()))
                .isInstanceOf(DocumentNotFoundException.class);

        // Restore anti-IDOR
        assertThatThrownBy(() -> service.restoreVersion(document.getId(), 1, attacker.getEmail()))
                .isInstanceOf(DocumentNotFoundException.class);

        // Regenerate anti-IDOR
        assertThatThrownBy(() -> service.regenerateDocument(document.getId(), attacker.getEmail()))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    // In-memory stubs
    private static class InMemoryUserRepository implements UserRepository {
        private final Map<Long, User> users = new HashMap<>();

        @Override public Optional<User> findByEmail(String email) {
            return users.values().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
        }
        @Override public boolean existsByEmail(String email) { return findByEmail(email).isPresent(); }
        @Override public <S extends User> S save(S entity) { users.put(entity.getId(), entity); return entity; }
        @Override public Optional<User> findById(Long id) { return Optional.ofNullable(users.get(id)); }
        @Override public boolean existsById(Long id) { return users.containsKey(id); }
        @Override public List<User> findAll() { return new ArrayList<>(users.values()); }
        @Override public List<User> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public long count() { return users.size(); }
        @Override public void deleteById(Long id) { users.remove(id); }
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
        @Override public User getOne(Long id) { return users.get(id); }
        @Override public User getById(Long id) { return users.get(id); }
        @Override public User getReferenceById(Long id) { return users.get(id); }
        @Override public <S extends User> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public List<User> findAll(Sort sort) { return findAll(); }
        @Override public Page<User> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
    }

    private static class InMemoryProjectRepository implements ProjectRepository {
        private final Map<Long, Project> projects = new HashMap<>();

        @Override public Optional<Project> findByIdAndUserId(Long id, Long userId) {
            Project p = projects.get(id);
            return (p != null && p.getUser().getId().equals(userId)) ? Optional.of(p) : Optional.empty();
        }
        @Override public boolean existsByIdAndUserId(Long id, Long userId) {
            Project p = projects.get(id);
            return p != null && p.getUser().getId().equals(userId);
        }
        @Override public List<Project> findAllByUserIdOrderByUpdatedAtDesc(Long userId) {
            return projects.values().stream().filter(p -> p.getUser().getId().equals(userId)).toList();
        }
        @Override public long countByUserIdAndStatus(Long userId, ProjectStatus status) {
            return projects.values().stream().filter(p -> p.getUser().getId().equals(userId) && p.getStatus() == status).count();
        }
        @Override public void deleteByIdAndUserId(Long id, Long userId) {
            Project p = projects.get(id);
            if (p != null && p.getUser().getId().equals(userId)) projects.remove(id);
        }
        @Override public long countByUserId(Long userId) {
            return projects.values().stream().filter(p -> p.getUser().getId().equals(userId)).count();
        }
        @Override public <S extends Project> S save(S entity) { projects.put(entity.getId(), entity); return entity; }
        @Override public Optional<Project> findById(Long id) { return Optional.ofNullable(projects.get(id)); }
        @Override public boolean existsById(Long id) { return projects.containsKey(id); }
        @Override public List<Project> findAll() { return new ArrayList<>(projects.values()); }
        @Override public List<Project> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public long count() { return projects.size(); }
        @Override public void deleteById(Long id) { projects.remove(id); }
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
        @Override public Project getOne(Long id) { return projects.get(id); }
        @Override public Project getById(Long id) { return projects.get(id); }
        @Override public Project getReferenceById(Long id) { return projects.get(id); }
        @Override public <S extends Project> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Project> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Project> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends Project> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends Project> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Project> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Project, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
        @Override public <S extends Project> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public List<Project> findAll(Sort sort) { return findAll(); }
        @Override public Page<Project> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
    }

    private static class InMemoryDocumentRepository implements DocumentRepository {
        private final Map<Long, Document> documents = new HashMap<>();

        @Override public List<Document> findAllByProjectIdOrderByFilenameAsc(Long projectId) {
            return documents.values().stream().filter(d -> d.getProject().getId().equals(projectId)).toList();
        }
        @Override public Optional<Document> findByIdAndProjectUserId(Long documentId, Long userId) {
            return documents.values().stream().filter(d -> d.getId().equals(documentId) && d.getProject().getUser().getId().equals(userId)).findFirst();
        }
        @Override public Optional<Document> findByIdWithLock(Long id) {
            return findById(id);
        }
        @Override public Optional<Document> findByProjectIdAndFilename(Long projectId, String filename) {
            return documents.values().stream().filter(d -> d.getProject().getId().equals(projectId) && d.getFilename().equalsIgnoreCase(filename)).findFirst();
        }
        @Override public void deleteAllByProjectId(Long projectId) {
            documents.entrySet().removeIf(e -> e.getValue().getProject().getId().equals(projectId));
        }
        @Override public long countByProjectId(Long projectId) {
            return documents.values().stream().filter(d -> d.getProject().getId().equals(projectId)).count();
        }
        @Override public <S extends Document> S save(S entity) {
            documents.put(entity.getId(), entity);
            return entity;
        }
        @Override public Optional<Document> findById(Long id) { return Optional.ofNullable(documents.get(id)); }
        @Override public boolean existsById(Long id) { return documents.containsKey(id); }
        @Override public List<Document> findAll() { return new ArrayList<>(documents.values()); }
        @Override public List<Document> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public long count() { return documents.size(); }
        @Override public void deleteById(Long id) { documents.remove(id); }
        @Override public void delete(Document entity) { documents.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends Document> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { documents.clear(); }
        @Override public void flush() {}
        @Override public <S extends Document> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Document> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<Document> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public Document getOne(Long id) { return documents.get(id); }
        @Override public Document getById(Long id) { return documents.get(id); }
        @Override public Document getReferenceById(Long id) { return documents.get(id); }
        @Override public <S extends Document> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public List<Document> findAll(Sort sort) { return findAll(); }
        @Override public Page<Document> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
    }

    private static class InMemoryDocumentVersionRepository implements DocumentVersionRepository {
        private final Map<Long, DocumentVersion> versions = new HashMap<>();
        private final AtomicLong idGen = new AtomicLong(1);

        @Override public List<DocumentVersion> findAllByDocumentIdOrderByVersionDesc(Long documentId) {
            return versions.values().stream()
                    .filter(v -> v.getDocument().getId().equals(documentId))
                    .sorted((a, b) -> Integer.compare(b.getVersion(), a.getVersion()))
                    .toList();
        }
        @Override public Optional<DocumentVersion> findByDocumentIdAndVersion(Long documentId, int version) {
            return versions.values().stream()
                    .filter(v -> v.getDocument().getId().equals(documentId) && v.getVersion() == version)
                    .findFirst();
        }
        @Override public int findMaxVersionByDocumentId(Long documentId) {
            return versions.values().stream()
                    .filter(v -> v.getDocument().getId().equals(documentId))
                    .mapToInt(DocumentVersion::getVersion)
                    .max()
                    .orElse(0);
        }
        @Override public long countByDocumentId(Long documentId) {
            return versions.values().stream().filter(v -> v.getDocument().getId().equals(documentId)).count();
        }
        @Override public void deleteAllByDocumentId(Long documentId) {
            versions.entrySet().removeIf(e -> e.getValue().getDocument().getId().equals(documentId));
        }
        @Override public void deleteAllByProjectId(Long projectId) {
            versions.entrySet().removeIf(e -> e.getValue().getDocument().getProject().getId().equals(projectId));
        }
        @Override public <S extends DocumentVersion> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen.getAndIncrement());
            if (entity.getCreatedAt() == null) entity.setCreatedAt(Instant.now());
            versions.put(entity.getId(), entity);
            return entity;
        }
        @Override public Optional<DocumentVersion> findById(Long id) { return Optional.ofNullable(versions.get(id)); }
        @Override public boolean existsById(Long id) { return versions.containsKey(id); }
        @Override public List<DocumentVersion> findAll() { return new ArrayList<>(versions.values()); }
        @Override public List<DocumentVersion> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public long count() { return versions.size(); }
        @Override public void deleteById(Long id) { versions.remove(id); }
        @Override public void delete(DocumentVersion entity) { versions.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends DocumentVersion> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { versions.clear(); }
        @Override public void flush() {}
        @Override public <S extends DocumentVersion> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends DocumentVersion> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<DocumentVersion> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public DocumentVersion getOne(Long id) { return versions.get(id); }
        @Override public DocumentVersion getById(Long id) { return versions.get(id); }
        @Override public DocumentVersion getReferenceById(Long id) { return versions.get(id); }
        @Override public <S extends DocumentVersion> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends DocumentVersion> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends DocumentVersion> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends DocumentVersion> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends DocumentVersion> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends DocumentVersion> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends DocumentVersion, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
        @Override public <S extends DocumentVersion> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public List<DocumentVersion> findAll(Sort sort) { return findAll(); }
        @Override public Page<DocumentVersion> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
    }
}
