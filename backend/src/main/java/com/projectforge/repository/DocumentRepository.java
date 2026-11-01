package com.projectforge.repository;

import com.projectforge.entity.Document;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Document entity.
 * Provides project-scoped querying and anti-IDOR user authorization.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * Lists all documents belonging to a project, ordered by filename ascending.
     */
    List<Document> findAllByProjectIdOrderByFilenameAsc(Long projectId);

    /**
     * Finds a single document by ID, verifying that the parent project belongs to the given user.
     * Enforces anti-IDOR authorization.
     */
    @Query("SELECT d FROM Document d JOIN d.project p WHERE d.id = :documentId AND p.user.id = :userId")
    Optional<Document> findByIdAndProjectUserId(@Param("documentId") Long documentId, @Param("userId") Long userId);

    /**
     * Finds a document by ID acquiring a pessimistic write lock for version safety.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Document d WHERE d.id = :id")
    Optional<Document> findByIdWithLock(@Param("id") Long id);

    /**
     * Finds a document by project ID and filename.
     */
    Optional<Document> findByProjectIdAndFilename(Long projectId, String filename);

    /**
     * Deletes all documents for a project during re-generation.
     */
    @Modifying
    @Query("DELETE FROM Document d WHERE d.project.id = :projectId")
    void deleteAllByProjectId(@Param("projectId") Long projectId);

    /**
     * Counts documents for a project.
     */
    long countByProjectId(Long projectId);
}
