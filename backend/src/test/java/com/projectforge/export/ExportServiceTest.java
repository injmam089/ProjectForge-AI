package com.projectforge.export;

import com.projectforge.dto.ExportResponse;
import com.projectforge.entity.Document;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.ProjectType;
import com.projectforge.entity.Role;
import com.projectforge.entity.User;
import com.projectforge.exception.InvalidExportException;
import com.projectforge.exception.ResourceNotFoundException;
import com.projectforge.repository.DocumentRepository;
import com.projectforge.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExportServiceTest {

    private ExportService service;
    private InMemoryProjectRepository projectRepository;
    private InMemoryDocumentRepository documentRepository;
    private User owner;
    private User attacker;
    private Project project;
    private Document doc1;
    private Document doc2;

    @BeforeEach
    void setUp() {
        projectRepository = new InMemoryProjectRepository();
        documentRepository = new InMemoryDocumentRepository();

        ExportValidator validator = new ExportValidator();
        ExportManifestBuilder manifestBuilder = new ExportManifestBuilder();
        ZipPackageBuilder packageBuilder = new ZipPackageBuilder(manifestBuilder);

        service = new ExportService(projectRepository, documentRepository, validator, packageBuilder);

        owner = new User("Owner", "owner@example.com", "hash", Role.USER);
        owner.setId(1L);

        attacker = new User("Attacker", "attacker@example.com", "hash", Role.USER);
        attacker.setId(2L);

        project = new Project(
                owner,
                "CloudPulse Telemetry",
                "Telemetry pipeline",
                "Logs",
                "Engineers",
                ProjectType.API_BACKEND,
                ProjectDifficulty.ADVANCED,
                "Java",
                "None"
        );
        project.setId(10L);
        project.setStatus(ProjectStatus.GENERATED);
        project.setProjectSpecification("{\"schemaVersion\":\"1.0.0\",\"project\":{\"name\":\"CloudPulse Telemetry\"}}");
        projectRepository.save(project);

        doc1 = new Document(project, DocumentType.README, "README.md", "# CloudPulse Readme v3 Content", 3);
        doc1.setId(100L);
        documentRepository.save(doc1);

        doc2 = new Document(project, DocumentType.ARCHITECTURE, "ARCHITECTURE.md", "# Architecture Guide v1 Content", 1);
        doc2.setId(101L);
        documentRepository.save(doc2);
    }

    @Test
    @DisplayName("prepareExport returns correct metadata without modifying anything")
    void prepareExportSuccess() {
        ExportResponse response = service.prepareExport(project.getId(), owner.getEmail());

        assertThat(response.projectId()).isEqualTo(project.getId());
        assertThat(response.projectName()).isEqualTo(project.getName());
        assertThat(response.documentCount()).isEqualTo(2);
        assertThat(response.filename()).isEqualTo("ProjectForge_CloudPulse_Telemetry.zip");
        assertThat(response.exportedAt()).isNotNull();
    }

    @Test
    @DisplayName("generateExportPackage produces valid ZIP and verifies strict read-only immutability")
    void generateExportPackageSuccessAndImmutability() {
        String specBefore = project.getProjectSpecification();
        ProjectStatus statusBefore = project.getStatus();
        String doc1ContentBefore = doc1.getContent();
        int doc1VersionBefore = doc1.getVersion();

        ExportService.ExportPackage exportPackage = service.generateExportPackage(project.getId(), owner.getEmail());

        assertThat(exportPackage.filename()).isEqualTo("ProjectForge_CloudPulse_Telemetry.zip");
        assertThat(exportPackage.content()).isNotEmpty();

        // Verify zero database mutation (read-only)
        Project freshProject = projectRepository.findById(project.getId()).orElseThrow();
        assertThat(freshProject.getProjectSpecification()).isEqualTo(specBefore);
        assertThat(freshProject.getStatus()).isEqualTo(statusBefore);

        Document freshDoc1 = documentRepository.findById(doc1.getId()).orElseThrow();
        assertThat(freshDoc1.getContent()).isEqualTo(doc1ContentBefore);
        assertThat(freshDoc1.getVersion()).isEqualTo(doc1VersionBefore);
    }

    @Test
    @DisplayName("Anti-IDOR: Non-owner export attempt throws ResourceNotFoundException (404)")
    void nonOwnerExportThrows404() {
        assertThatThrownBy(() -> service.prepareExport(project.getId(), attacker.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> service.generateExportPackage(project.getId(), attacker.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Exporting project with no documents throws InvalidExportException")
    void emptyProjectThrows() {
        documentRepository.deleteAll();

        assertThatThrownBy(() -> service.prepareExport(project.getId(), owner.getEmail()))
                .isInstanceOf(InvalidExportException.class)
                .hasMessageContaining("no documents available for export");

        assertThatThrownBy(() -> service.generateExportPackage(project.getId(), owner.getEmail()))
                .isInstanceOf(InvalidExportException.class)
                .hasMessageContaining("no documents available for export");
    }

    // --- In-Memory Repositories for Unit Testing ---

    private static class InMemoryProjectRepository implements ProjectRepository {
        private final Map<Long, Project> projects = new HashMap<>();
        private final AtomicLong idGen = new AtomicLong(1);

        @Override public Optional<Project> findById(Long id) { return Optional.ofNullable(projects.get(id)); }
        @Override public <S extends Project> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen.getAndIncrement());
            projects.put(entity.getId(), entity);
            return entity;
        }
        @Override public boolean existsByIdAndUserId(Long id, Long userId) {
            Project p = projects.get(id);
            return p != null && p.getUser() != null && p.getUser().getId().equals(userId);
        }
        @Override public List<Project> findAllByUserIdOrderByUpdatedAtDesc(Long userId) {
            return projects.values().stream().filter(p -> p.getUser().getId().equals(userId)).toList();
        }
        @Override public Optional<Project> findByIdAndUserId(Long id, Long userId) {
            Project p = projects.get(id);
            return (p != null && p.getUser().getId().equals(userId)) ? Optional.of(p) : Optional.empty();
        }
        @Override public void deleteByIdAndUserId(Long id, Long userId) {
            projects.entrySet().removeIf(e -> e.getKey().equals(id) && e.getValue().getUser().getId().equals(userId));
        }
        @Override public long countByUserId(Long userId) {
            return projects.values().stream().filter(p -> p.getUser().getId().equals(userId)).count();
        }
        @Override public long countByUserIdAndStatus(Long userId, ProjectStatus status) {
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
        @Override public void flush() {}
        @Override public <S extends Project> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Project> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<Project> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch() { throw new UnsupportedOperationException(); }
        @Override public Project getOne(Long aLong) { throw new UnsupportedOperationException(); }
        @Override public Project getById(Long aLong) { throw new UnsupportedOperationException(); }
        @Override public Project getReferenceById(Long aLong) { return projects.get(aLong); }
        @Override public <S extends Project> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Project> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends Project> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public List<Project> findAll() { return new ArrayList<>(projects.values()); }
        @Override public List<Project> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public boolean existsById(Long aLong) { return projects.containsKey(aLong); }
        @Override public long count() { return projects.size(); }
        @Override public void deleteById(Long aLong) { projects.remove(aLong); }
        @Override public void delete(Project entity) { projects.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends Project> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { projects.clear(); }
        @Override public List<Project> findAll(Sort sort) { return findAll(); }
        @Override public Page<Project> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends Project> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Project> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends Project> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Project> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Project, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    }

    private static class InMemoryDocumentRepository implements DocumentRepository {
        private final Map<Long, Document> documents = new HashMap<>();
        private final AtomicLong idGen = new AtomicLong(1);

        @Override public List<Document> findAllByProjectIdOrderByFilenameAsc(Long projectId) {
            return documents.values().stream()
                    .filter(d -> d.getProject().getId().equals(projectId))
                    .sorted((a, b) -> a.getFilename().compareToIgnoreCase(b.getFilename()))
                    .toList();
        }
        @Override public Optional<Document> findByIdAndProjectUserId(Long documentId, Long userId) {
            Document d = documents.get(documentId);
            return (d != null && d.getProject().getUser().getId().equals(userId)) ? Optional.of(d) : Optional.empty();
        }
        @Override public Optional<Document> findByIdWithLock(Long id) { return Optional.ofNullable(documents.get(id)); }
        @Override public Optional<Document> findByProjectIdAndFilename(Long projectId, String filename) {
            return documents.values().stream()
                    .filter(d -> d.getProject().getId().equals(projectId) && d.getFilename().equalsIgnoreCase(filename))
                    .findFirst();
        }
        @Override public void deleteAllByProjectId(Long projectId) {
            documents.entrySet().removeIf(e -> e.getValue().getProject().getId().equals(projectId));
        }
        @Override public long countByProjectId(Long projectId) {
            return documents.values().stream().filter(d -> d.getProject().getId().equals(projectId)).count();
        }
        @Override public List<Object[]> countByProjectIds(List<Long> projectIds) {
            Map<Long, Long> counts = documents.values().stream()
                    .filter(d -> projectIds.contains(d.getProject().getId()))
                    .collect(java.util.stream.Collectors.groupingBy(d -> d.getProject().getId(), java.util.stream.Collectors.counting()));
            return counts.entrySet().stream()
                    .map(e -> new Object[]{ e.getKey(), e.getValue() })
                    .toList();
        }
        @Override public <S extends Document> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen.getAndIncrement());
            documents.put(entity.getId(), entity);
            return entity;
        }
        @Override public Optional<Document> findById(Long id) { return Optional.ofNullable(documents.get(id)); }
        @Override public void deleteAll() { documents.clear(); }
        @Override public void flush() {}
        @Override public <S extends Document> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Document> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<Document> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch() { throw new UnsupportedOperationException(); }
        @Override public Document getOne(Long aLong) { throw new UnsupportedOperationException(); }
        @Override public Document getById(Long aLong) { throw new UnsupportedOperationException(); }
        @Override public Document getReferenceById(Long aLong) { return documents.get(aLong); }
        @Override public <S extends Document> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public List<Document> findAll() { return new ArrayList<>(documents.values()); }
        @Override public List<Document> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public boolean existsById(Long aLong) { return documents.containsKey(aLong); }
        @Override public long count() { return documents.size(); }
        @Override public void deleteById(Long aLong) { documents.remove(aLong); }
        @Override public void delete(Document entity) { documents.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends Document> entities) { throw new UnsupportedOperationException(); }
        @Override public List<Document> findAll(Sort sort) { return findAll(); }
        @Override public Page<Document> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Document, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    }
}
