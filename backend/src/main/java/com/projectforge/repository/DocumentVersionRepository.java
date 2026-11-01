package com.projectforge.repository;

import com.projectforge.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for DocumentVersion entity.
 */
@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    /**
     * Finds all historical versions for a document ordered newest first.
     */
    @Query("SELECT v FROM DocumentVersion v WHERE v.document.id = :documentId ORDER BY v.version DESC")
    List<DocumentVersion> findAllByDocumentIdOrderByVersionDesc(@Param("documentId") Long documentId);

    /**
     * Finds a specific version snapshot of a document.
     */
    @Query("SELECT v FROM DocumentVersion v WHERE v.document.id = :documentId AND v.version = :version")
    Optional<DocumentVersion> findByDocumentIdAndVersion(@Param("documentId") Long documentId, @Param("version") int version);

    /**
     * Finds highest version recorded for a document.
     */
    @Query("SELECT COALESCE(MAX(v.version), 0) FROM DocumentVersion v WHERE v.document.id = :documentId")
    int findMaxVersionByDocumentId(@Param("documentId") Long documentId);

    /**
     * Counts version records for a document.
     */
    long countByDocumentId(Long documentId);

    /**
     * Deletes all versions for a document.
     */
    @Modifying
    @Query("DELETE FROM DocumentVersion v WHERE v.document.id = :documentId")
    void deleteAllByDocumentId(@Param("documentId") Long documentId);

    /**
     * Deletes all versions for all documents belonging to a project.
     */
    @Modifying
    @Query("DELETE FROM DocumentVersion v WHERE v.document.project.id = :projectId")
    void deleteAllByProjectId(@Param("projectId") Long projectId);
}
