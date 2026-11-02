package com.projectforge.repository;

import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.ProjectType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Finds paginated projects for a user matching optional status, type, difficulty, and search keyword.
     */
    @Query("SELECT p FROM Project p WHERE p.user.id = :userId " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:type IS NULL OR p.projectType = :type) " +
           "AND (:difficulty IS NULL OR p.difficulty = :difficulty) " +
           "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Project> findWithFilters(
            @Param("userId") Long userId,
            @Param("status") ProjectStatus status,
            @Param("type") ProjectType type,
            @Param("difficulty") ProjectDifficulty difficulty,
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Finds all projects for a user matching optional status, type, difficulty, and search keyword with sort.
     */
    @Query("SELECT p FROM Project p WHERE p.user.id = :userId " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:type IS NULL OR p.projectType = :type) " +
           "AND (:difficulty IS NULL OR p.difficulty = :difficulty) " +
           "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Project> findWithFilters(
            @Param("userId") Long userId,
            @Param("status") ProjectStatus status,
            @Param("type") ProjectType type,
            @Param("difficulty") ProjectDifficulty difficulty,
            @Param("search") String search,
            Sort sort
    );
}
