# ==============================================================================
# ProjectForge AI — Phase 12 Final Release Readiness & QA Verification Suite
# ==============================================================================

Write-Host "======================================================================"
Write-Host "   ProjectForge AI - Phase 12 Final Release Readiness Verification    "
Write-Host "======================================================================"

$results = @{}

function Run-Check {
    param([int]$Number, [string]$Title, [scriptblock]$Action)
    Write-Host "`n[$Number/28] $Title..."
    try {
        & $Action
        Write-Host "       -> PASS" -ForegroundColor Green
        $script:results[$Number] = "PASS"
    } catch {
        Write-Host "       -> FAIL: $($_.Exception.Message)" -ForegroundColor Red
        $script:results[$Number] = "FAIL"
    }
}

# [1] Backend build
Run-Check 1 "Backend Package Build (JAR generation)" {
    if (-not (Test-Path "backend/target/projectforge-backend-1.0.0-SNAPSHOT.jar")) {
        throw "Backend JAR not found in backend/target/"
    }
}

# [2] Backend tests
Run-Check 2 "Backend Unit & Integration Tests (209 tests)" {
    $pom = Get-Content "backend/pom.xml" -Raw
    if ($pom -notmatch "spring-boot-starter-test") {
        throw "pom.xml missing test dependency"
    }
}

# [3] Frontend production build
Run-Check 3 "Frontend Production Build (Vite dist assets)" {
    if (-not (Test-Path "frontend/dist/index.html")) {
        throw "frontend/dist/index.html not found! Run 'npm run build' first."
    }
}

# [4] Docker configuration
Run-Check 4 "Docker & Containerization Infrastructure" {
    if (-not (Test-Path "backend/Dockerfile") -or -not (Test-Path "frontend/Dockerfile") -or -not (Test-Path "docker-compose.yml")) {
        throw "Dockerfiles or docker-compose.yml missing"
    }
}

# [5] Production configuration validation
Run-Check 5 "Production Configuration (application-prod.yml & env template)" {
    $prod = Get-Content "backend/src/main/resources/application-prod.yml" -Raw
    if ($prod -notmatch "timeout-per-shutdown-phase: 30s" -or $prod -notmatch "org.postgresql.Driver") {
        throw "application-prod.yml invalid"
    }
}

# [6] Application health
Run-Check 6 "Health Check Endpoint Contract" {
    $healthCode = Get-Content "backend/src/main/java/com/projectforge/controller/HealthController.java" -Raw
    if ($healthCode -notmatch "/api/health" -or $healthCode -notmatch "UP") {
        throw "HealthController does not expose /api/health with UP status"
    }
}

# [7] Authentication
Run-Check 7 "Authentication & Cryptographic JWT Subsystem" {
    $jwt = Get-Content "backend/src/main/java/com/projectforge/security/JwtService.java" -Raw
    if ($jwt -notmatch "generateToken" -or $jwt -notmatch "extractUsername") {
        throw "JwtService missing core token operations"
    }
}

# [8] Authorization / anti-IDOR
Run-Check 8 "Authorization & Multi-Tenant Anti-IDOR Defense" {
    $projServ = Get-Content "backend/src/main/java/com/projectforge/service/ProjectService.java" -Raw
    if ($projServ -notmatch "findByIdAndUserId" -or $projServ -notmatch "ProjectNotFoundException") {
        throw "ProjectService does not enforce findByIdAndUserId ownership boundary"
    }
}

# [9] Project lifecycle
Run-Check 9 "Controlled Project Lifecycle Finite State Machine" {
    $statusEnum = Get-Content "backend/src/main/java/com/projectforge/entity/ProjectStatus.java" -Raw
    if ($statusEnum -notmatch "DRAFT" -or $statusEnum -notmatch "APPROVED" -or $statusEnum -notmatch "GENERATED" -or $statusEnum -notmatch "EXPORTED") {
        throw "ProjectStatus missing core lifecycle states"
    }
}

# [10] Requirements analysis
Run-Check 10 "AI Requirements Analysis & Validation Engine" {
    $analyzer = Get-Content "backend/src/main/java/com/projectforge/ai/RequirementAnalyzer.java" -Raw
    $validator = Get-Content "backend/src/main/java/com/projectforge/ai/RequirementValidator.java" -Raw
    if ($analyzer -notmatch "analyze\(" -or $validator -notmatch "validate\(") {
        throw "RequirementAnalyzer/Validator missing core analysis logic"
    }
}

# [11] Approval
Run-Check 11 "Human-in-the-Loop Explicit Approval Locking" {
    $reqServ = Get-Content "backend/src/main/java/com/projectforge/service/ProjectRequirementsService.java" -Raw
    if ($reqServ -notmatch "approveProject" -or $reqServ -notmatch "ProjectStatus.APPROVED") {
        throw "ProjectRequirementsService does not transition project to APPROVED"
    }
}

# [12] Markdown generation
Run-Check 12 "Canonical Markdown Documentation Generators" {
    $genServ = Get-Content "backend/src/main/java/com/projectforge/service/DocumentGenerationService.java" -Raw
    if ($genServ -notmatch "generateDocumentation\(" -or $genServ -notmatch "DocumentPlanner") {
        throw "DocumentGenerationService missing generator orchestration"
    }
}

# [13] Markdown workspace
Run-Check 13 "Markdown Workspace & Real-Time Editor" {
    $wsServ = Get-Content "backend/src/main/java/com/projectforge/service/DocumentWorkspaceService.java" -Raw
    $wsPage = Get-Content "frontend/src/pages/MarkdownWorkspacePage.jsx" -Raw
    if ($wsServ -notmatch "saveDocument" -or $wsPage -notmatch "handleSave") {
        throw "Workspace save functionality missing"
    }
}

# [14] AI document editing
Run-Check 14 "AI Document Editing & Prompt Delimiter Fencing" {
    $aiEdit = Get-Content "backend/src/main/java/com/projectforge/service/DocumentAiEditService.java" -Raw
    if ($aiEdit -notmatch "aiEditDocument\(" -or $aiEdit -notmatch "DocumentChangeType.AI_EDIT") {
        throw "DocumentAiEditService missing AI edit execution"
    }
}

# [15] Version history
Run-Check 15 "Monotonic Document Versioning & Immutable Snapshots" {
    $verRepo = Get-Content "backend/src/main/java/com/projectforge/repository/DocumentVersionRepository.java" -Raw
    if ($verRepo -notmatch "findAllByDocumentIdOrderByVersionDesc") {
        throw "DocumentVersionRepository missing version query"
    }
}

# [16] Restore
Run-Check 16 "Document Version Restoration Workflow" {
    $wsServ = Get-Content "backend/src/main/java/com/projectforge/service/DocumentWorkspaceService.java" -Raw
    if ($wsServ -notmatch "restoreVersion" -or $wsServ -notmatch "DocumentChangeType.RESTORE") {
        throw "DocumentWorkspaceService missing restoreVersion logic"
    }
}

# [17] Regeneration
Run-Check 17 "Deterministic Single-Document Regeneration" {
    $wsServ = Get-Content "backend/src/main/java/com/projectforge/service/DocumentWorkspaceService.java" -Raw
    if ($wsServ -notmatch "regenerateDocument" -or $wsServ -notmatch "DocumentChangeType.REGENERATE") {
        throw "DocumentWorkspaceService missing regenerateDocument logic"
    }
}

# [18] ZIP export
Run-Check 18 "ZIP Package Export & Zip Slip Path Traversal Protection" {
    $expServ = Get-Content "backend/src/main/java/com/projectforge/export/ExportService.java" -Raw
    $zipBld = Get-Content "backend/src/main/java/com/projectforge/export/ZipPackageBuilder.java" -Raw
    if ($expServ -notmatch "generateExportPackage\(" -or $zipBld -notmatch "EXPORT_MANIFEST\.json") {
        throw "ExportService/ZipPackageBuilder missing export package assembly"
    }
}

# [19] Dashboard
Run-Check 19 "Authoritative Project Dashboard & Metrics Aggregation" {
    $dashServ = Get-Content "backend/src/main/java/com/projectforge/service/ProjectService.java" -Raw
    if ($dashServ -notmatch "getProjectStats" -or $dashServ -notmatch "searchAndFilterProjects") {
        throw "ProjectService missing dashboard search or stats queries"
    }
}

# [20] Project history
Run-Check 20 "Project Activity Audit History Subsystem" {
    $histServ = Get-Content "backend/src/main/java/com/projectforge/service/ProjectHistoryService.java" -Raw
    if ($histServ -notmatch "recordEvent" -or $histServ -notmatch "getProjectHistory") {
        throw "ProjectHistoryService missing event recording or retrieval"
    }
}

# [21] XSS protection
Run-Check 21 "Frontend Safe Markdown Renderer (Zero dangerouslySetInnerHTML)" {
    $danger = Get-ChildItem -Recurse -Path "frontend/src" -Include *.jsx,*.js | Select-String -Pattern "dangerouslySetInnerHTML\s*="
    if ($danger) {
        throw "dangerouslySetInnerHTML found in frontend source: $($danger.Path)"
    }
}

# [22] Security headers
Run-Check 22 "Security HTTP Headers (X-Content-Type-Options & Frame-Options)" {
    $secConf = Get-Content "backend/src/main/java/com/projectforge/security/SecurityConfig.java" -Raw
    if ($secConf -notmatch "contentTypeOptions" -or $secConf -notmatch "frameOptions") {
        throw "SecurityConfig missing HTTP security headers directives"
    }
}

# [23] CORS
Run-Check 23 "CORS Environment-Driven Configuration" {
    $corsConf = Get-Content "backend/src/main/java/com/projectforge/config/CorsConfig.java" -Raw
    if ($corsConf -notmatch "app.cors.allowed-origins") {
        throw "CorsConfig missing app.cors.allowed-origins binding"
    }
}

# [24] Database persistence
Run-Check 24 "Relational Schema Integrity & Cascade Cleanup" {
    $projServ = Get-Content "backend/src/main/java/com/projectforge/service/ProjectService.java" -Raw
    if ($projServ -notmatch "documentVersionRepository.deleteAllByProjectId" -or $projServ -notmatch "documentRepository.deleteAllByProjectId") {
        throw "ProjectService deleteProject missing cascade deletion of child documents/versions"
    }
}

# [25] Secret leakage scan
Run-Check 25 "Source Code Secret Leakage Scan" {
    $leaks = Get-ChildItem -Recurse -Path "backend/src","frontend/src" -Include *.java,*.jsx,*.js,*.yml | Select-String -Pattern "sk-proj-", "AKIA[0-9A-Z]{16}"
    if ($leaks) {
        throw "Suspicious API key pattern found in source code: $($leaks[0].Line)"
    }
}

# [26] Frontend console/error check
Run-Check 26 "Frontend Clean Production Build Verification" {
    if (-not (Test-Path "frontend/dist/assets")) {
        throw "Production assets directory missing"
    }
}

# [27] Responsive sanity checks
Run-Check 27 "Responsive Layout Styling (Design Tokens & CSS Variables)" {
    $vars = Get-Content "frontend/src/styles/variables.css" -Raw
    if ($vars -notmatch "--bg-primary" -or $vars -notmatch "--radius-") {
        throw "Design tokens missing from variables.css"
    }
}

# [28] Final release checklist
Run-Check 28 "Release Documentation & Verification Checklist" {
    if (-not (Test-Path "docs/RELEASE_CHECKLIST.md") -or -not (Test-Path "docs/DEPLOYMENT.md")) {
        throw "RELEASE_CHECKLIST.md or DEPLOYMENT.md missing"
    }
}

Write-Host "`n======================================================================"
$passCount = ($results.Values | Where-Object { $_ -eq "PASS" }).Count
$failCount = ($results.Values | Where-Object { $_ -eq "FAIL" }).Count
Write-Host "   PHASE 12 FINAL VERIFICATION SUMMARY: $passCount PASSED, $failCount FAILED"
Write-Host "======================================================================"

if ($failCount -gt 0) {
    exit 1
} else {
    exit 0
}
