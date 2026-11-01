package com.projectforge.repository;

import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Project entities.
 * Strictly enforces user ownership across all query methods.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Finds all projects owned by the specified user, ordered by most recently updated.
     */
    List<Project> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    /**
     * Finds a specific project by ID and owner user ID (strict ownership check).
     */
    Optional<Project> findByIdAndUserId(Long id, Long userId);

    /**
     * Checks if a project exists for the given ID and owner user ID.
     */
    boolean existsByIdAndUserId(Long id, Long userId);

    /**
     * Deletes a project by ID and owner user ID.
     */
    void deleteByIdAndUserId(Long id, Long userId);

    /**
     * Counts the total projects owned by the specified user.
     */
    long countByUserId(Long userId);

    /**
     * Counts the projects owned by the specified user with a given status.
     */
    long countByUserIdAndStatus(Long userId, ProjectStatus status);
}
