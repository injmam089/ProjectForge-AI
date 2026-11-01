package com.projectforge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectforge.ai.ClarificationEngine;
import com.projectforge.ai.PromptLoader;
import com.projectforge.ai.QualityEvaluator;
import com.projectforge.ai.RequirementAnalyzer;
import com.projectforge.ai.RequirementValidator;
import com.projectforge.dto.ClarificationAnswerDto;
import com.projectforge.dto.ClarificationRequest;
import com.projectforge.dto.RequirementsAnalysisResponse;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.User;
import com.projectforge.exception.InvalidProjectStateException;
import com.projectforge.exception.ProjectNotFoundException;
import com.projectforge.repository.ProjectRepository;
import com.projectforge.repository.UserRepository;
import com.projectforge.testutil.TestSpecificationFactory;
import com.projectforge.testutil.TestSpecificationFactory.StubAIService;
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

class ProjectRequirementsServiceTest {

    private InMemoryProjectRepository projectRepository;
    private InMemoryUserRepository userRepository;
    private StubAIService stubAiService;
    private ObjectMapper objectMapper;
    private ProjectRequirementsService service;

    private User testUser;
    private Project testProject;

    @BeforeEach
    void setUp() {
        projectRepository = new InMemoryProjectRepository();
        userRepository = new InMemoryUserRepository();
        objectMapper = new ObjectMapper();

        String validJson = TestSpecificationFactory.createValidSpecificationJson();
        stubAiService = new StubAIService(validJson);
        PromptLoader promptLoader = new PromptLoader();
        RequirementValidator validator = new RequirementValidator();
        QualityEvaluator qualityEvaluator = new QualityEvaluator();
        RequirementAnalyzer analyzer = new RequirementAnalyzer(stubAiService, promptLoader, validator, qualityEvaluator, objectMapper);
        ClarificationEngine clarificationEngine = new ClarificationEngine(stubAiService, promptLoader, validator, qualityEvaluator, analyzer, objectMapper);

        service = new ProjectRequirementsService(
                projectRepository, userRepository, analyzer, clarificationEngine, objectMapper
        );

        testUser = userRepository.save(TestSpecificationFactory.createTestUser());
        testProject = projectRepository.save(TestSpecificationFactory.createTestProject(testUser));
    }

    @Test
    @DisplayName("Successfully analyzes project with HIGH question, transitions to UNDER_REVIEW and stores specification")
    void analyzeProjectWithHighQuestionTransitionsToUnderReview() {
        RequirementsAnalysisResponse response = service.analyzeProject(testProject.getId(), testUser.getEmail());

        assertThat(response.projectId()).isEqualTo(testProject.getId());
        assertThat(response.status()).isEqualTo(ProjectStatus.UNDER_REVIEW);
        assertThat(response.qualityScore()).isGreaterThanOrEqualTo(80);
        assertThat(response.openQuestions()).hasSize(1);
        assertThat(response.openQuestions().get(0).importance()).isEqualTo("HIGH");

        Project saved = projectRepository.findById(testProject.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ProjectStatus.UNDER_REVIEW);
        assertThat(saved.getProjectSpecification()).isNotNull();
    }

    @Test
    @DisplayName("Successfully analyzes project without blocking questions, transitions to REQUIREMENTS_READY")
    void analyzeProjectWithoutBlockingQuestionsTransitionsToRequirementsReady() {
        stubAiService.setResponse(new com.projectforge.ai.AIResponse(
                TestSpecificationFactory.createValidSpecificationJsonWithoutQuestions(), 200, 100, "gpt-4o"
        ));

        RequirementsAnalysisResponse response = service.analyzeProject(testProject.getId(), testUser.getEmail());

        assertThat(response.projectId()).isEqualTo(testProject.getId());
        assertThat(response.status()).isEqualTo(ProjectStatus.REQUIREMENTS_READY);
        assertThat(response.qualityScore()).isGreaterThanOrEqualTo(80);
        assertThat(response.openQuestions()).isEmpty();

        Project saved = projectRepository.findById(testProject.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ProjectStatus.REQUIREMENTS_READY);
        assertThat(saved.getProjectSpecification()).isNotNull();
    }

    @Test
    @DisplayName("Throws ProjectNotFoundException when project does not belong to authenticated user (anti-IDOR)")
    void analyzeProjectOwnershipFailure() {
        assertThatThrownBy(() -> service.analyzeProject(999L, testUser.getEmail()))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Throws InvalidProjectStateException when analysis is already running")
    void analyzeProjectAlreadyAnalyzingThrows() {
        testProject.setStatus(ProjectStatus.ANALYZING);
        projectRepository.save(testProject);

        assertThatThrownBy(() -> service.analyzeProject(testProject.getId(), testUser.getEmail()))
                .isInstanceOf(InvalidProjectStateException.class)
                .hasMessageContaining("already currently executing");
    }

    @Test
    @DisplayName("Reverts project status to READY_FOR_ANALYSIS when AI analysis throws runtime exception")
    void analyzeProjectFailureRevertsState() {
        stubAiService.setExceptionToThrow(new RuntimeException("OpenAI connection timed out"));

        assertThatThrownBy(() -> service.analyzeProject(testProject.getId(), testUser.getEmail()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("OpenAI connection timed out");

        Project saved = projectRepository.findById(testProject.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ProjectStatus.READY_FOR_ANALYSIS);
    }

    @Test
    @DisplayName("Retrieves existing requirements and open questions successfully")
    void getRequirementsSuccess() {
        testProject.setProjectSpecification(TestSpecificationFactory.createValidSpecificationJson());
        testProject.setStatus(ProjectStatus.REQUIREMENTS_READY);
        projectRepository.save(testProject);

        RequirementsAnalysisResponse response = service.getRequirements(testProject.getId(), testUser.getEmail());

        assertThat(response.projectId()).isEqualTo(testProject.getId());
        assertThat(response.qualityScore()).isGreaterThanOrEqualTo(80);
        assertThat(response.openQuestions()).hasSize(1);
    }

    @Test
    @DisplayName("Throws InvalidProjectStateException when getting requirements for project without specification")
    void getRequirementsWithoutSpecThrows() {
        testProject.setProjectSpecification(null);
        projectRepository.save(testProject);

        assertThatThrownBy(() -> service.getRequirements(testProject.getId(), testUser.getEmail()))
                .isInstanceOf(InvalidProjectStateException.class)
                .hasMessageContaining("No requirements analysis has been generated");
    }

    @Test
    @DisplayName("Submits clarifications resolving questions, transitions to REQUIREMENTS_READY")
    void submitClarificationsSuccess() {
        testProject.setProjectSpecification(TestSpecificationFactory.createValidSpecificationJson());
        testProject.setStatus(ProjectStatus.UNDER_REVIEW);
        projectRepository.save(testProject);

        stubAiService.setResponse(new com.projectforge.ai.AIResponse(
                TestSpecificationFactory.createValidSpecificationJsonWithoutQuestions(), 200, 100, "gpt-4o"
        ));

        ClarificationRequest request = new ClarificationRequest(List.of(
                new ClarificationAnswerDto("Q1", "Use Zstandard compression")
        ));

        RequirementsAnalysisResponse response = service.submitClarifications(testProject.getId(), request, testUser.getEmail());

        assertThat(response.status()).isEqualTo(ProjectStatus.REQUIREMENTS_READY);
        assertThat(response.qualityScore()).isGreaterThanOrEqualTo(80);
        assertThat(response.openQuestions()).isEmpty();

        Project saved = projectRepository.findById(testProject.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ProjectStatus.REQUIREMENTS_READY);
    }

    @Test
    @DisplayName("Submits clarifications but unresolved HIGH questions keep project in UNDER_REVIEW")
    void submitClarificationsRemainingQuestionsKeepsUnderReview() {
        testProject.setProjectSpecification(TestSpecificationFactory.createValidSpecificationJson());
        testProject.setStatus(ProjectStatus.UNDER_REVIEW);
        projectRepository.save(testProject);

        ClarificationRequest request = new ClarificationRequest(List.of(
                new ClarificationAnswerDto("Q1", "Use Zstandard compression")
        ));

        RequirementsAnalysisResponse response = service.submitClarifications(testProject.getId(), request, testUser.getEmail());

        assertThat(response.status()).isEqualTo(ProjectStatus.UNDER_REVIEW);
        assertThat(response.openQuestions()).hasSize(1);

        Project saved = projectRepository.findById(testProject.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ProjectStatus.UNDER_REVIEW);
    }

    @Test
    @DisplayName("Clarification failure preserves existing stored specification in database")
    void submitClarificationsFailurePreservesStoredSpecification() {
        String originalSpec = TestSpecificationFactory.createValidSpecificationJson();
        testProject.setProjectSpecification(originalSpec);
        testProject.setStatus(ProjectStatus.UNDER_REVIEW);
        projectRepository.save(testProject);

        stubAiService.setExceptionToThrow(new RuntimeException("OpenAI connection reset"));

        ClarificationRequest request = new ClarificationRequest(List.of(
                new ClarificationAnswerDto("Q1", "Answer")
        ));

        assertThatThrownBy(() -> service.submitClarifications(testProject.getId(), request, testUser.getEmail()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("OpenAI connection reset");

        Project saved = projectRepository.findById(testProject.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ProjectStatus.UNDER_REVIEW);
        assertThat(saved.getProjectSpecification()).isEqualTo(originalSpec);
    }

    private static class InMemoryUserRepository implements UserRepository {
        private final Map<Long, User> users = new HashMap<>();
        private final AtomicLong idGen = new AtomicLong(1);

        @Override
        public Optional<User> findByEmail(String email) {
            return users.values().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
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
