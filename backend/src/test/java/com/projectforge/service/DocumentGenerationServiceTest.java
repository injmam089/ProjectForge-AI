package com.projectforge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectforge.dto.DocumentGenerationResponse;
import com.projectforge.dto.DocumentResponse;
import com.projectforge.dto.DocumentSummaryResponse;
import com.projectforge.entity.Document;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.ProjectType;
import com.projectforge.entity.Role;
import com.projectforge.entity.User;
import com.projectforge.exception.InvalidProjectStateException;
import com.projectforge.exception.ProjectNotFoundException;
import com.projectforge.exception.ResourceNotFoundException;
import com.projectforge.markdown.DocumentContentGenerator;
import com.projectforge.markdown.DocumentPlanner;
import com.projectforge.markdown.MarkdownGenerator;
import com.projectforge.markdown.MarkdownValidator;
import com.projectforge.markdown.generator.AiBuildInstructionsGenerator;
import com.projectforge.markdown.generator.ApiSpecificationGenerator;
import com.projectforge.markdown.generator.ArchitectureGenerator;
import com.projectforge.markdown.generator.DatabaseGenerator;
import com.projectforge.markdown.generator.DeploymentGenerator;
import com.projectforge.markdown.generator.DevelopmentPlanGenerator;
import com.projectforge.markdown.generator.FeaturesGenerator;
import com.projectforge.markdown.generator.ProjectGoalsGenerator;
import com.projectforge.markdown.generator.ProjectRequirementsGenerator;
import com.projectforge.markdown.generator.ReadmeGenerator;
import com.projectforge.markdown.generator.SecurityGenerator;
import com.projectforge.markdown.generator.SpecializedDocumentGenerators;
import com.projectforge.markdown.generator.TechStackGenerator;
import com.projectforge.markdown.generator.TestingGenerator;
import com.projectforge.markdown.generator.UiUxRequirementsGenerator;
import com.projectforge.repository.DocumentRepository;
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

class DocumentGenerationServiceTest {

    private InMemoryUserRepository userRepository;
    private InMemoryProjectRepository projectRepository;
    private InMemoryDocumentRepository documentRepository;
    private DocumentGenerationService service;

    private User testUser;
    private User otherUser;
    private Project testProject;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        projectRepository = new InMemoryProjectRepository();
        documentRepository = new InMemoryDocumentRepository();

        testUser = new User("Alpha User", "alpha@example.com", "hash", Role.USER);
        testUser = userRepository.save(testUser);

        otherUser = new User("Beta User", "beta@example.com", "hash", Role.USER);
        otherUser = userRepository.save(otherUser);

        testProject = new Project(
                testUser,
                "CloudPulse Telemetry",
                "Observability collector",
                "Latency spikes",
                "DevOps Engineers",
                ProjectType.API_BACKEND,
                ProjectDifficulty.ADVANCED,
                "Spring Boot, React, PostgreSQL",
                null
        );
        testProject.setProjectSpecification(TestSpecificationFactory.createValidSpecificationJsonWithoutQuestions());
        testProject.setStatus(ProjectStatus.APPROVED);
        testProject = projectRepository.save(testProject);

        List<DocumentContentGenerator> generators = List.of(
                new ReadmeGenerator(),
                new ProjectRequirementsGenerator(),
                new ProjectGoalsGenerator(),
                new FeaturesGenerator(),
                new TechStackGenerator(),
                new ArchitectureGenerator(),
                new DatabaseGenerator(),
                new ApiSpecificationGenerator(),
                new SecurityGenerator(),
                new UiUxRequirementsGenerator(),
                new DevelopmentPlanGenerator(),
                new TestingGenerator(),
                new DeploymentGenerator(),
                new AiBuildInstructionsGenerator(),
                new SpecializedDocumentGenerators.ModelRequirementsGenerator(),
                new SpecializedDocumentGenerators.DatasetGenerator(),
                new SpecializedDocumentGenerators.MlPipelineGenerator(),
                new SpecializedDocumentGenerators.HardwareRequirementsGenerator(),
                new SpecializedDocumentGenerators.SensorSpecificationGenerator(),
                new SpecializedDocumentGenerators.IotProtocolsGenerator(),
                new SpecializedDocumentGenerators.ThreatModelGenerator(),
                new SpecializedDocumentGenerators.SecurityTestingGenerator(),
                new SpecializedDocumentGenerators.MobileRequirementsGenerator()
        );

        MarkdownGenerator markdownGenerator = new MarkdownGenerator(generators);
        DocumentPlanner documentPlanner = new DocumentPlanner();
        MarkdownValidator markdownValidator = new MarkdownValidator();
        ObjectMapper objectMapper = new ObjectMapper();

        service = new DocumentGenerationService(
                projectRepository,
                userRepository,
                documentRepository,
                documentPlanner,
                markdownGenerator,
                markdownValidator,
                objectMapper
        );
    }

    @Test
    @DisplayName("Generation from APPROVED project generates documents and transitions to GENERATED")
    void generateDocumentationSuccess() {
        DocumentGenerationResponse response = service.generateDocumentation(testProject.getId(), testUser.getEmail());

        assertThat(response.projectId()).isEqualTo(testProject.getId());
        assertThat(response.status()).isEqualTo(ProjectStatus.GENERATED);
        assertThat(response.documentsCount()).isGreaterThanOrEqualTo(10);
        assertThat(response.documents()).isNotEmpty();

        Project updated = projectRepository.findById(testProject.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ProjectStatus.GENERATED);

        List<Document> docs = documentRepository.findAllByProjectIdOrderByFilenameAsc(testProject.getId());
        assertThat(docs).hasSize(response.documentsCount());
    }

    @Test
    @DisplayName("Re-generation from GENERATED project replaces existing documents deterministically")
    void regenerateDocumentationSuccess() {
        // Initial generation
        service.generateDocumentation(testProject.getId(), testUser.getEmail());
        long firstCount = documentRepository.countByProjectId(testProject.getId());

        // Re-generate
        DocumentGenerationResponse reResponse = service.generateDocumentation(testProject.getId(), testUser.getEmail());

        assertThat(reResponse.status()).isEqualTo(ProjectStatus.GENERATED);
        long secondCount = documentRepository.countByProjectId(testProject.getId());
        assertThat(secondCount).isEqualTo(firstCount);
    }

    @Test
    @DisplayName("Generation fails when project is not in APPROVED or GENERATED status")
    void generateFromUnapprovedStateFails() {
        testProject.setStatus(ProjectStatus.DRAFT);
        projectRepository.save(testProject);

        assertThatThrownBy(() -> service.generateDocumentation(testProject.getId(), testUser.getEmail()))
                .isInstanceOf(InvalidProjectStateException.class)
                .hasMessageContaining("DRAFT");

        testProject.setStatus(ProjectStatus.REQUIREMENTS_READY);
        projectRepository.save(testProject);

        assertThatThrownBy(() -> service.generateDocumentation(testProject.getId(), testUser.getEmail()))
                .isInstanceOf(InvalidProjectStateException.class)
                .hasMessageContaining("REQUIREMENTS_READY");

        testProject.setStatus(ProjectStatus.UNDER_REVIEW);
        projectRepository.save(testProject);

        assertThatThrownBy(() -> service.generateDocumentation(testProject.getId(), testUser.getEmail()))
                .isInstanceOf(InvalidProjectStateException.class)
                .hasMessageContaining("UNDER_REVIEW");
    }

    @Test
    @DisplayName("Generation fails when approved specification is null or empty")
    void generateMissingSpecFails() {
        testProject.setProjectSpecification("");
        projectRepository.save(testProject);

        assertThatThrownBy(() -> service.generateDocumentation(testProject.getId(), testUser.getEmail()))
                .isInstanceOf(InvalidProjectStateException.class)
                .hasMessageContaining("missing or empty");
    }

    @Test
    @DisplayName("Generation failure safely recovers project state back to APPROVED")
    void generateFailureRollsBackStatus() {
        testProject.setProjectSpecification("{ broken invalid json");
        projectRepository.save(testProject);

        assertThatThrownBy(() -> service.generateDocumentation(testProject.getId(), testUser.getEmail()))
                .isInstanceOf(com.projectforge.exception.DocumentGenerationException.class);

        Project recovered = projectRepository.findById(testProject.getId()).orElseThrow();
        assertThat(recovered.getStatus()).isEqualTo(ProjectStatus.APPROVED);
    }

    @Test
    @DisplayName("Anti-IDOR: Other user attempting to generate documentation receives ProjectNotFoundException (404)")
    void generateAntiIdor() {
        assertThatThrownBy(() -> service.generateDocumentation(testProject.getId(), otherUser.getEmail()))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining(testProject.getId().toString());
    }

    @Test
    @DisplayName("Anti-IDOR: Other user attempting to list project documents receives ProjectNotFoundException (404)")
    void getProjectDocumentsAntiIdor() {
        assertThatThrownBy(() -> service.getProjectDocuments(testProject.getId(), otherUser.getEmail()))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    @DisplayName("Anti-IDOR: Other user attempting to get single document receives ResourceNotFoundException (404)")
    void getDocumentAntiIdor() {
        service.generateDocumentation(testProject.getId(), testUser.getEmail());
        Document doc = documentRepository.findAllByProjectIdOrderByFilenameAsc(testProject.getId()).get(0);

        // Owner can access
        DocumentResponse docRes = service.getDocument(doc.getId(), testUser.getEmail());
        assertThat(docRes.filename()).isEqualTo(doc.getFilename());

        // Other user rejected with 404
        assertThatThrownBy(() -> service.getDocument(doc.getId(), otherUser.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- In-Memory Repository Fakes ---

    private static class InMemoryUserRepository implements UserRepository {
        private final Map<Long, User> users = new HashMap<>();
        private final AtomicLong idGen = new AtomicLong(1);

        @Override
        public Optional<User> findByEmail(String email) {
            return users.values().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
        }

        @Override public boolean existsByEmail(String email) { return findByEmail(email).isPresent(); }
        @Override public <S extends User> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen.getAndIncrement());
            users.put(entity.getId(), entity);
            return entity;
        }
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
        private final AtomicLong idGen = new AtomicLong(1);

        @Override
        public List<Project> findAllByUserIdOrderByUpdatedAtDesc(Long userId) {
            return projects.values().stream()
                    .filter(p -> p.getUser().getId().equals(userId))
                    .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                    .toList();
        }

        @Override
        public Optional<Project> findByIdAndUserId(Long projectId, Long userId) {
            return projects.values().stream()
                    .filter(p -> p.getId().equals(projectId) && p.getUser().getId().equals(userId))
                    .findFirst();
        }

        @Override
        public long countByUserIdAndStatus(Long userId, ProjectStatus status) {
            return projects.values().stream()
                    .filter(p -> p.getUser().getId().equals(userId) && p.getStatus() == status)
                    .count();
        }

        @Override
        public void deleteByIdAndUserId(Long id, Long userId) {
            projects.entrySet().removeIf(entry -> entry.getValue().getId().equals(id) && entry.getValue().getUser().getId().equals(userId));
        }

        @Override
        public long countByUserId(Long userId) {
            return projects.values().stream().filter(p -> p.getUser().getId().equals(userId)).count();
        }

        @Override public boolean existsByIdAndUserId(Long projectId, Long userId) { return findByIdAndUserId(projectId, userId).isPresent(); }
        @Override public <S extends Project> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen.getAndIncrement());
            if (entity.getCreatedAt() == null) entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
            projects.put(entity.getId(), entity);
            return entity;
        }
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
        private final AtomicLong idGen = new AtomicLong(1);

        @Override
        public List<Document> findAllByProjectIdOrderByFilenameAsc(Long projectId) {
            return documents.values().stream()
                    .filter(d -> d.getProject().getId().equals(projectId))
                    .sorted((a, b) -> a.getFilename().compareToIgnoreCase(b.getFilename()))
                    .toList();
        }

        @Override
        public Optional<Document> findByIdAndProjectUserId(Long documentId, Long userId) {
            return documents.values().stream()
                    .filter(d -> d.getId().equals(documentId) && d.getProject().getUser().getId().equals(userId))
                    .findFirst();
        }

        @Override
        public Optional<Document> findByIdWithLock(Long id) {
            return findById(id);
        }

        @Override
        public Optional<Document> findByProjectIdAndFilename(Long projectId, String filename) {
            return documents.values().stream()
                    .filter(d -> d.getProject().getId().equals(projectId) && d.getFilename().equalsIgnoreCase(filename))
                    .findFirst();
        }

        @Override
        public void deleteAllByProjectId(Long projectId) {
            documents.entrySet().removeIf(entry -> entry.getValue().getProject().getId().equals(projectId));
        }

        @Override
        public long countByProjectId(Long projectId) {
            return documents.values().stream().filter(d -> d.getProject().getId().equals(projectId)).count();
        }

        @Override
        public <S extends Document> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen.getAndIncrement());
            if (entity.getCreatedAt() == null) entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
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
}
