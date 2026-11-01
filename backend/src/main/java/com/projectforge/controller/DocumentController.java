package com.projectforge.controller;

import com.projectforge.dto.DocumentAiEditRequest;
import com.projectforge.dto.DocumentAiEditResponse;
import com.projectforge.dto.DocumentEditRequest;
import com.projectforge.dto.DocumentResponse;
import com.projectforge.dto.DocumentSaveResponse;
import com.projectforge.dto.DocumentVersionResponse;
import com.projectforge.dto.DocumentVersionSummaryResponse;
import com.projectforge.service.DocumentAiEditService;
import com.projectforge.service.DocumentGenerationService;
import com.projectforge.service.DocumentWorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller exposing Markdown document workspace operations:
 * viewing, editing, versioning, restore, canonical regeneration, and AI-assisted editing.
 * Requires authentication and strictly enforces owning user authorization (anti-IDOR).
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentGenerationService documentGenerationService;
    private final DocumentWorkspaceService documentWorkspaceService;
    private final DocumentAiEditService documentAiEditService;

    public DocumentController(
            DocumentGenerationService documentGenerationService,
            DocumentWorkspaceService documentWorkspaceService,
            DocumentAiEditService documentAiEditService
    ) {
        this.documentGenerationService = documentGenerationService;
        this.documentWorkspaceService = documentWorkspaceService;
        this.documentAiEditService = documentAiEditService;
    }

    /**
     * Retrieves a single generated document by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        DocumentResponse response = documentGenerationService.getDocument(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Manually updates and saves a Markdown document.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DocumentSaveResponse> saveDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentEditRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        DocumentSaveResponse response = documentWorkspaceService.saveDocument(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Lists version history metadata for a document.
     */
    @GetMapping("/{id}/versions")
    public ResponseEntity<List<DocumentVersionSummaryResponse>> getVersions(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<DocumentVersionSummaryResponse> response = documentWorkspaceService.getVersions(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a specific historical version of a document including full content.
     */
    @GetMapping("/{id}/versions/{version}")
    public ResponseEntity<DocumentVersionResponse> getVersion(
            @PathVariable Long id,
            @PathVariable int version,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        DocumentVersionResponse response = documentWorkspaceService.getVersion(id, version, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Restores a document to a previous historical version.
     */
    @PostMapping("/{id}/restore/{version}")
    public ResponseEntity<DocumentResponse> restoreVersion(
            @PathVariable Long id,
            @PathVariable int version,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        DocumentResponse response = documentWorkspaceService.restoreVersion(id, version, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Regenerates an individual Markdown document from the approved specification.
     */
    @PostMapping("/{id}/regenerate")
    public ResponseEntity<DocumentResponse> regenerateDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        DocumentResponse response = documentWorkspaceService.regenerateDocument(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Executes AI-assisted editing on an individual Markdown document.
     */
    @PostMapping("/{id}/ai-edit")
    public ResponseEntity<DocumentAiEditResponse> aiEditDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentAiEditRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        DocumentAiEditResponse response = documentAiEditService.aiEditDocument(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}
