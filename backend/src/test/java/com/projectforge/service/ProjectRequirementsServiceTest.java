package com.projectforge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projectforge.ai.ClarificationEngine;
import com.projectforge.ai.PromptLoader;
import com.projectforge.ai.QualityEvaluator;
import com.projectforge.ai.RequirementAnalyzer;
import com.projectforge.ai.RequirementValidator;
import com.projectforge.dto.ApprovalRequest;
import com.projectforge.dto.ApprovalResponse;
import com.projectforge.dto.ClarificationAnswerDto;
import com.projectforge.dto.ClarificationRequest;
import com.projectforge.dto.ProjectReviewResponse;
import com.projectforge.dto.ProjectReviewUpdateRequest;
import com.projectforge.dto.RequirementsAnalysisResponse;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.User;
import com.projectforge.exception.InvalidProjectStateException;
import com.projectforge.exception.ProjectNotFoundException;
import com.projectforge.exception.ProjectValidationException;
import com.projectforge.repository.ProjectRepository;
import com.projectforge.repository.UserRepository;
import com.projectforge.testutil.TestSpecificationFactory;
import com.projectforge.testutil.TestSpecificationFactory.StubAIService;
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

    // ==========================================
    // Phase 5: Review & Approval Unit Tests
    // ==========================================

    @Test
    @DisplayName("GET review successfully returns specification, blocking questions, and approval state")
    void getReviewSuccess() {
        testProject.setProjectSpecification(TestSpecificationFactory.createValidSpecificationJson());
        testProject.setStatus(ProjectStatus.UNDER_REVIEW);
        projectRepository.save(testProject);

        ProjectReviewResponse response = service.getReview(testProject.getId(), testUser.getEmail());

        assertThat(response.projectId()).isEqualTo(testProject.getId());
        assertThat(response.status()).isEqualTo(ProjectStatus.UNDER_REVIEW);
        assertThat(response.qualityScore()).isGreaterThanOrEqualTo(80);
        assertThat(response.qualityDetails()).isNotNull();
        assertThat(response.openQuestions()).hasSize(1);
        assertThat(response.blockingQuestions()).hasSize(1);
        assertThat(response.canApprove()).isFalse();
        assertThat(response.approved()).isFalse();
        assertThat(response.message()).contains("blocking clarification question");
    }

    @Test
    @DisplayName("GET review without specification throws InvalidProjectStateException")
    void getReviewWithoutSpecThrows() {
        testProject.setProjectSpecification(null);
        projectRepository.save(testProject);

        assertThatThrownBy(() -> service.getReview(testProject.getId(), testUser.getEmail()))
                .isInstanceOf(InvalidProjectStateException.class)
                .hasMessageContaining("No requirements specification exists yet");
    }

    @Test
    @DisplayName("GET review with foreign project throws ProjectNotFoundException (anti-IDOR)")
    void getReviewAntiIdor() {
        assertThatThrownBy(() -> service.getReview(999L, testUser.getEmail()))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("PUT review with valid spec without blocking questions updates specification and sets REQUIREMENTS_READY")
    void updateReviewWithoutBlockingQuestionsSuccess() {
        testProject.setProjectSpecification(TestSpecificationFactory.createValidSpecificationJson());
        testProject.setStatus(ProjectStatus.UNDER_REVIEW);
        projectRepository.save(testProject);

        ObjectNode specNode = (ObjectNode) TestSpecificationFactory.createValidSpecificationNode();
        specNode.putArray("openQuestions"); // clear blocking questions

        ProjectReviewUpdateRequest updateReq = new ProjectReviewUpdateRequest(specNode);
        ProjectReviewResponse response = service.updateReview(testProject.getId(), updateReq, testUser.getEmail());

        assertThat(response.status()).isEqualTo(ProjectStatus.REQUIREMENTS_READY);
        assertThat(response.blockingQuestions()).isEmpty();
        assertThat(response.canApprove()).isTrue();
        assertThat(response.approved()).isFalse();

        Project saved = projectRepository.findById(testProject.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ProjectStatus.REQUIREMENTS_READY);
    }

    @Test
    @DisplayName("PUT review with invalid structure throws ProjectValidationException")
    void updateReviewInvalidStructureThrows() {
        testProject.setProjectSpecification(TestSpecificationFactory.createValidSpecificationJson());
        testProject.setStatus(ProjectStatus.UNDER_REVIEW);
        projectRepository.save(testProject);

        ObjectNode specNode = (ObjectNode) TestSpecificationFactory.createValidSpecificationNode();
        specNode.remove("features"); // missing required section

        ProjectReviewUpdateRequest updateReq = new ProjectReviewUpdateRequest(specNode);
        assertThatThrownBy(() -> service.updateReview(testProject.getId(), updateReq, testUser.getEmail()))
                .isInstanceOf(ProjectValidationException.class)
                .hasMessageContaining("features");
    }

    @Test
    @DisplayName("PUT review on APPROVED project throws InvalidProjectStateException (post-approval protection)")
    void updateReviewApprovedProjectThrows() {
        testProject.setProjectSpecification(TestSpecificationFactory.createValidSpecificationJson());
        testProject.setStatus(ProjectStatus.APPROVED);
        projectRepository.save(testProject);

        ProjectReviewUpdateRequest updateReq = new ProjectReviewUpdateRequest(TestSpecificationFactory.createValidSpecificationNode());
        assertThatThrownBy(() -> service.updateReview(testProject.getId(), updateReq, testUser.getEmail()))
                .isInstanceOf(InvalidProjectStateException.class)
                .hasMessageContaining("protected status APPROVED");
    }

    @Test
    @DisplayName("POST analyze on APPROVED project throws InvalidProjectStateException (post-approval protection)")
    void analyzeApprovedProjectThrows() {
        testProject.setProjectSpecification(TestSpecificationFactory.createValidSpecificationJson());
        testProject.setStatus(ProjectStatus.APPROVED);
        projectRepository.save(testProject);

        assertThatThrownBy(() -> service.analyzeProject(testProject.getId(), testUser.getEmail()))
                .isInstanceOf(InvalidProjectStateException.class)
                .hasMessageContaining("APPROVED");
    }

    @Test
    @DisplayName("POST clarifications on APPROVED project throws InvalidProjectStateException (post-approval protection)")
    void submitClarificationsApprovedProjectThrows() {
        testProject.setProjectSpecification(TestSpecificationFactory.createValidSpecificationJson());
        testProject.setStatus(ProjectStatus.APPROVED);
        projectRepository.save(testProject);

        ClarificationRequest request = new ClarificationRequest(List.of(new ClarificationAnswerDto("Q1", "Answer")));
        assertThatThrownBy(() -> service.submitClarifications(testProject.getId(), request, testUser.getEmail()))
                .isInstanceOf(InvalidProjectStateException.class)
                .hasMessageContaining("APPROVED");
    }

    @Test
    @DisplayName("POST approve successfully transitions REQUIREMENTS_READY project to APPROVED")
    void approveProjectSuccess() {
        testProject.setProjectSpecification(TestSpecificationFactory.createValidSpecificationJsonWithoutQuestions());
        testProject.setStatus(ProjectStatus.REQUIREMENTS_READY);
        projectRepository.save(testProject);

        ApprovalRequest request = new ApprovalRequest(true);
        ApprovalResponse response = service.approveProject(testProject.getId(), request, testUser.getEmail());

        assertThat(response.projectId()).isEqualTo(testProject.getId());
        assertThat(response.status()).isEqualTo(ProjectStatus.APPROVED);
        assertThat(response.approved()).isTrue();
        assertThat(response.qualityScore()).isGreaterThanOrEqualTo(80);
        assertThat(response.message()).contains("approved successfully");

        Project saved = projectRepository.findById(testProject.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ProjectStatus.APPROVED);
    }

    @Test
    @DisplayName("POST approve is idempotent: repeated approval returns 200 OK without side effects")
    void approveProjectIdempotent() {
        testProject.setProjectSpecification(TestSpecificationFactory.createValidSpecificationJsonWithoutQuestions());
        testProject.setStatus(ProjectStatus.APPROVED);
        projectRepository.save(testProject);

        ApprovalRequest request = new ApprovalRequest(true);
        ApprovalResponse response = service.approveProject(testProject.getId(), request, testUser.getEmail());

        assertThat(response.status()).isEqualTo(ProjectStatus.APPROVED);
        assertThat(response.approved()).isTrue();
        assertThat(response.message()).contains("already approved");

        Project saved = projectRepository.findById(testProject.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ProjectStatus.APPROVED);
    }

    @Test
    @DisplayName("POST approve rejects false or missing confirmation")
    void approveProjectConfirmationValidationFailure() {
        testProject.setProjectSpecification(TestSpecificationFactory.createValidSpecificationJsonWithoutQuestions());
        testProject.setStatus(ProjectStatus.REQUIREMENTS_READY);
        projectRepository.save(testProject);

        assertThatThrownBy(() -> service.approveProject(testProject.getId(), new ApprovalRequest(false), testUser.getEmail()))
                .isInstanceOf(ProjectValidationException.class)
                .hasMessageContaining("explicitly set to true");

        assertThatThrownBy(() -> service.approveProject(testProject.getId(), new ApprovalRequest(null), testUser.getEmail()))
                .isInstanceOf(ProjectValidationException.class);
    }

    @Test
    @DisplayName("POST approve rejects project with unresolved HIGH/CRITICAL blocking questions")
    void approveProjectWithBlockingQuestionsFails() {
        // Spec contains Q1 with importance: HIGH
        testProject.setProjectSpecification(TestSpecificationFactory.createValidSpecificationJson());
        testProject.setStatus(ProjectStatus.UNDER_REVIEW);
        projectRepository.save(testProject);

        ApprovalRequest request = new ApprovalRequest(true);
        assertThatThrownBy(() -> service.approveProject(testProject.getId(), request, testUser.getEmail()))
                .isInstanceOf(InvalidProjectStateException.class)
                .hasMessageContaining("unresolved critical or high clarification questions remain");

        Project saved = projectRepository.findById(testProject.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ProjectStatus.UNDER_REVIEW);
    }

    @Test
    @DisplayName("POST approve allows approval when only informational LOW questions remain")
    void approveProjectWithLowQuestionsSucceeds() {
        ObjectNode specNode = (ObjectNode) TestSpecificationFactory.createValidSpecificationNode();
        ArrayNode questions = specNode.putArray("openQuestions");
        ObjectNode q1 = questions.addObject();
        q1.put("id", "Q1");
        q1.put("question", "Preferred UI accent color?");
        q1.put("importance", "LOW");
        q1.put("reason", "Cosmetic only");

        try {
            testProject.setProjectSpecification(objectMapper.writeValueAsString(specNode));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        testProject.setStatus(ProjectStatus.REQUIREMENTS_READY);
        projectRepository.save(testProject);

        ApprovalRequest request = new ApprovalRequest(true);
        ApprovalResponse response = service.approveProject(testProject.getId(), request, testUser.getEmail());

        assertThat(response.status()).isEqualTo(ProjectStatus.APPROVED);
        assertThat(response.approved()).isTrue();

        Project saved = projectRepository.findById(testProject.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ProjectStatus.APPROVED);
    }

    @Test
    @DisplayName("POST approve rejects approval from illegal lifecycle state (DRAFT, ANALYZING)")
    void approveProjectIllegalStateFails() {
        testProject.setProjectSpecification(TestSpecificationFactory.createValidSpecificationJsonWithoutQuestions());
        testProject.setStatus(ProjectStatus.DRAFT);
        projectRepository.save(testProject);

        ApprovalRequest request = new ApprovalRequest(true);
        assertThatThrownBy(() -> service.approveProject(testProject.getId(), request, testUser.getEmail()))
                .isInstanceOf(InvalidProjectStateException.class)
                .hasMessageContaining("Cannot approve project from status: DRAFT");
    }

    @Test
    @DisplayName("POST approve on foreign project throws ProjectNotFoundException (anti-IDOR)")
    void approveProjectAntiIdor() {
        ApprovalRequest request = new ApprovalRequest(true);
        assertThatThrownBy(() -> service.approveProject(999L, request, testUser.getEmail()))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining("999");
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
        public Page<Project> findWithFilters(Long userId, ProjectStatus status, com.projectforge.entity.ProjectType type, com.projectforge.entity.ProjectDifficulty difficulty, String search, Pageable pageable) {
            List<Project> filtered = findWithFilters(userId, status, type, difficulty, search, pageable.getSort());
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filtered.size());
            List<Project> sub = start > filtered.size() ? List.of() : filtered.subList(start, end);
            return new PageImpl<>(sub, pageable, filtered.size());
        }

        @Override
        public List<Project> findWithFilters(Long userId, ProjectStatus status, com.projectforge.entity.ProjectType type, com.projectforge.entity.ProjectDifficulty difficulty, String search, Sort sort) {
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
