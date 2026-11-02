package com.projectforge.repository;

import com.projectforge.entity.ProjectHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for ProjectHistory entity.
 */
@Repository
public interface ProjectHistoryRepository extends JpaRepository<ProjectHistory, Long> {

    /**
     * Finds paginated history events for a project ordered newest first.
     */
    @Query("SELECT h FROM ProjectHistory h WHERE h.project.id = :projectId ORDER BY h.createdAt DESC, h.id DESC")
    Page<ProjectHistory> findAllByProjectIdOrderByCreatedAtDesc(@Param("projectId") Long projectId, Pageable pageable);

    /**
     * Finds all history events for a project ordered newest first.
     */
    @Query("SELECT h FROM ProjectHistory h WHERE h.project.id = :projectId ORDER BY h.createdAt DESC, h.id DESC")
    List<ProjectHistory> findAllByProjectIdOrderByCreatedAtDesc(@Param("projectId") Long projectId);

    /**
     * Counts history events for a project.
     */
    long countByProjectId(Long projectId);

    /**
     * Deletes all history records for a project during project deletion.
     */
    @Modifying
    @Query("DELETE FROM ProjectHistory h WHERE h.project.id = :projectId")
    void deleteAllByProjectId(@Param("projectId") Long projectId);
}
