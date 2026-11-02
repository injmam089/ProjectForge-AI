$port = 8094
$baseUrl = "http://localhost:$port"
$logFile = "scratch/backend_live_phase9.log"
$errLogFile = "scratch/backend_live_phase9_err.log"

Write-Host "=========================================================="
Write-Host "   ProjectForge AI - Phase 9 Live End-to-End Verification  "
Write-Host "=========================================================="

Write-Host "[1/16] Launching Spring Boot Backend on port $port..."

if (Test-Path $logFile) { Remove-Item $logFile -Force }
if (Test-Path $errLogFile) { Remove-Item $errLogFile -Force }

$javaBin = if ($env:JAVA_HOME) { "$env:JAVA_HOME\bin\java.exe" } else { "java" }

$proc = Start-Process -FilePath $javaBin -ArgumentList @(
    "-jar", "backend/target/projectforge-backend-1.0.0-SNAPSHOT.jar",
    "--server.port=$port",
    "--app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "--app.jwt.expiration-ms=86400000",
    "--spring.datasource.url=jdbc:h2:mem:livephase9db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "--spring.datasource.driver-class-name=org.h2.Driver",
    "--spring.datasource.username=sa",
    "--spring.datasource.password=",
    "--spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "--spring.jpa.hibernate.ddl-auto=create-drop",
    "--logging.level.com.projectforge=INFO"
) -PassThru -NoNewWindow -RedirectStandardOutput $logFile -RedirectStandardError $errLogFile

try {
    # Poll for health check
    $started = $false
    for ($i = 0; $i -lt 35; $i++) {
        Start-Sleep -Seconds 1
        try {
            $health = Invoke-RestMethod -Uri "$baseUrl/api/health" -Method Get -TimeoutSec 2 -ErrorAction Stop
            if ($health.status -eq "UP") {
                $started = $true
                break
            }
        } catch {
            # Still booting
        }
    }

    if (-not $started) {
        Write-Host "Backend failed to start. Logs:"
        if (Test-Path $logFile) { Get-Content $logFile -Tail 30 }
        if (Test-Path $errLogFile) { Get-Content $errLogFile -Tail 30 }
        exit 1
    }

    Write-Host "[2/16] Health check UP: $($health.service) (200 OK)"

    # Test Unauthenticated access to Phase 9 endpoints
    Write-Host "[3/16] Verifying 401 Unauthorized on unauthenticated Phase 9 endpoints..."
    $endpointsToTest401 = @(
        @{ Method = "Get";    Uri = "$baseUrl/api/projects" },
        @{ Method = "Get";    Uri = "$baseUrl/api/projects/stats" },
        @{ Method = "Get";    Uri = "$baseUrl/api/projects/1/history" },
        @{ Method = "Delete"; Uri = "$baseUrl/api/projects/1" }
    )

    foreach ($ep in $endpointsToTest401) {
        try {
            Invoke-RestMethod -Uri $ep.Uri -Method $ep.Method -ErrorAction Stop
            Write-Error "FAIL: Unauthenticated $($ep.Method) $($ep.Uri) succeeded!"
            exit 1
        } catch {
            $code = $_.Exception.Response.StatusCode.value__
            if ($code -ne 401) {
                Write-Error "Expected 401 for $($ep.Uri), got $code"
                exit 1
            }
        }
    }
    Write-Host "       401 Unauthorized verified for all unauthenticated dashboard endpoints."

    # Register & Login User Alpha and User Beta
    Write-Host "[4/16] Registering and authenticating User Alpha and User Beta..."
    $alphaReg = @{ name = "User Alpha"; email = "alpha@phase9.test"; password = "Password123!" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $alphaReg -ContentType "application/json" | Out-Null
    $alphaLogin = @{ email = "alpha@phase9.test"; password = "Password123!" } | ConvertTo-Json
    $alphaAuth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $alphaLogin -ContentType "application/json"
    $alphaToken = $alphaAuth.token
    $alphaHeaders = @{ Authorization = "Bearer $alphaToken" }

    $betaReg = @{ name = "User Beta"; email = "beta@phase9.test"; password = "Password123!" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $betaReg -ContentType "application/json" | Out-Null
    $betaLogin = @{ email = "beta@phase9.test"; password = "Password123!" } | ConvertTo-Json
    $betaAuth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $betaLogin -ContentType "application/json"
    $betaToken = $betaAuth.token
    $betaHeaders = @{ Authorization = "Bearer $betaToken" }
    Write-Host "       Tokens acquired for Alpha and Beta."

    # Create Projects for Alpha and Beta
    Write-Host "[5/16] Creating projects for Alpha (3 projects) and Beta (1 project)..."
    $alphaP1 = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Post -Headers $alphaHeaders -ContentType "application/json" -Body (@{
        name = "Alpha CloudPulse Monitoring"
        description = "Distributed telemetry and observability platform"
        problemStatement = "Microservices lack unified metrics"
        targetUsers = "DevOps Engineers"
        projectType = "WEB_APPLICATION"
        difficulty = "ADVANCED"
        preferredTechStack = "Java, React, PostgreSQL"
        specialRequirements = "Sub-10ms query latency"
    } | ConvertTo-Json)

    $alphaP2 = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Post -Headers $alphaHeaders -ContentType "application/json" -Body (@{
        name = "Alpha Mobile Wallet"
        description = "Decentralized mobile payment application"
        problemStatement = "Cross-border fees are excessive"
        targetUsers = "Mobile Consumers"
        projectType = "MOBILE_APPLICATION"
        difficulty = "INTERMEDIATE"
        preferredTechStack = "Flutter, Go, PostgreSQL"
        specialRequirements = "Biometric authentication"
    } | ConvertTo-Json)

    $alphaP3 = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Post -Headers $alphaHeaders -ContentType "application/json" -Body (@{
        name = "Alpha CLI Automation Tool"
        description = "Developer command-line scaffolding tool"
        problemStatement = "Manual setup takes too long"
        targetUsers = "Developers"
        projectType = "DESKTOP_APPLICATION"
        difficulty = "BEGINNER"
        preferredTechStack = "Rust, Cliv2"
        specialRequirements = "Single binary release"
    } | ConvertTo-Json)

    $betaP1 = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Post -Headers $betaHeaders -ContentType "application/json" -Body (@{
        name = "Beta Enterprise Cloud Service"
        description = "High throughput financial transactions service"
        problemStatement = "Legacy systems can't scale"
        targetUsers = "Financial Institutions"
        projectType = "API_BACKEND"
        difficulty = "EXPERT"
        preferredTechStack = "Java, Kafka, Cassandra"
        specialRequirements = "PCI-DSS Level 1"
    } | ConvertTo-Json)

    Write-Host "       Alpha projects created: IDs $($alphaP1.id), $($alphaP2.id), $($alphaP3.id)"
    Write-Host "       Beta project created: ID $($betaP1.id)"

    # Verify Multi-Tenant Isolation
    Write-Host "[6/16] Verifying multi-tenant isolation on dashboard listing..."
    $alphaListing = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Get -Headers $alphaHeaders
    if ($alphaListing.Count -ne 3) {
        Write-Error "Alpha should have 3 projects, but got $($alphaListing.Count)"
        exit 1
    }
    foreach ($p in $alphaListing) {
        if ($p.id -eq $betaP1.id) {
            Write-Error "LEAK: Beta's project $($betaP1.id) appeared in Alpha's projects listing!"
            exit 1
        }
    }

    $betaListing = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Get -Headers $betaHeaders
    if ($betaListing.Count -ne 1) {
        Write-Error "Beta should have 1 project, but got $($betaListing.Count)"
        exit 1
    }
    if ($betaListing[0].id -ne $betaP1.id) {
        Write-Error "Beta's listing should contain project $($betaP1.id)"
        exit 1
    }
    Write-Host "       Multi-tenant isolation verified on GET /api/projects."

    # Verify User-Scoped Statistics Endpoint
    Write-Host "[7/16] Testing user-scoped stats endpoint (GET /api/projects/stats)..."
    $alphaStats = Invoke-RestMethod -Uri "$baseUrl/api/projects/stats" -Method Get -Headers $alphaHeaders
    if ($alphaStats.totalProjects -ne 3 -or $alphaStats.draft -ne 3) {
        Write-Error "Alpha stats mismatch: expected total=3, draft=3; got total=$($alphaStats.totalProjects), draft=$($alphaStats.draft)"
        exit 1
    }

    $betaStats = Invoke-RestMethod -Uri "$baseUrl/api/projects/stats" -Method Get -Headers $betaHeaders
    if ($betaStats.totalProjects -ne 1 -or $betaStats.draft -ne 1) {
        Write-Error "Beta stats mismatch: expected total=1, draft=1; got total=$($betaStats.totalProjects), draft=$($betaStats.draft)"
        exit 1
    }
    Write-Host "       Stats verified: Alpha total=$($alphaStats.totalProjects), Beta total=$($betaStats.totalProjects)."

    # Verify Keyword Search
    Write-Host "[8/16] Testing bounded keyword search (GET /api/projects?search=...)..."
    $searchWallet = Invoke-RestMethod -Uri "$baseUrl/api/projects?page=0&size=10&search=Wallet" -Method Get -Headers $alphaHeaders
    if ($searchWallet.totalElements -ne 1 -or $searchWallet.content[0].name -ne "Alpha Mobile Wallet") {
        Write-Error "Search 'Wallet' should return 1 project ('Alpha Mobile Wallet'), got $($searchWallet.totalElements)"
        exit 1
    }

    $searchEmpty = Invoke-RestMethod -Uri "$baseUrl/api/projects?page=0&size=10&search=NonExistent" -Method Get -Headers $alphaHeaders
    if ($searchEmpty.totalElements -ne 0) {
        Write-Error "Search 'NonExistent' should return 0 projects, got $($searchEmpty.totalElements)"
        exit 1
    }
    Write-Host "       Keyword search verified: match found and empty results handled correctly."

    # Verify Multi-Attribute Filtering (status, type, difficulty)
    Write-Host "[9/16] Testing multi-attribute filtering (status, type, difficulty)..."
    $filterType = Invoke-RestMethod -Uri "$baseUrl/api/projects?page=0&size=10&type=MOBILE_APPLICATION" -Method Get -Headers $alphaHeaders
    if ($filterType.totalElements -ne 1 -or $filterType.content[0].projectType -ne "MOBILE_APPLICATION") {
        Write-Error "Type filter MOBILE_APPLICATION should return 1 project, got $($filterType.totalElements)"
        exit 1
    }

    $filterDiff = Invoke-RestMethod -Uri "$baseUrl/api/projects?page=0&size=10&difficulty=ADVANCED" -Method Get -Headers $alphaHeaders
    if ($filterDiff.totalElements -ne 1 -or $filterDiff.content[0].difficulty -ne "ADVANCED") {
        Write-Error "Difficulty filter ADVANCED should return 1 project, got $($filterDiff.totalElements)"
        exit 1
    }

    $filterStatus = Invoke-RestMethod -Uri "$baseUrl/api/projects?page=0&size=10&status=DRAFT" -Method Get -Headers $alphaHeaders
    if ($filterStatus.totalElements -ne 3) {
        Write-Error "Status filter DRAFT should return 3 projects, got $($filterStatus.totalElements)"
        exit 1
    }
    Write-Host "       Filtering by status, type, and difficulty verified."

    # Verify Server-Side Pagination
    Write-Host "[10/16] Testing server-side pagination (page=0, size=2)..."
    $page0 = Invoke-RestMethod -Uri "$baseUrl/api/projects?page=0&size=2" -Method Get -Headers $alphaHeaders
    if ($page0.content.Count -ne 2 -or $page0.totalElements -ne 3 -or $page0.totalPages -ne 2) {
        Write-Error "Page 0 size 2 failed: content count=$($page0.content.Count), total=$($page0.totalElements), pages=$($page0.totalPages)"
        exit 1
    }

    $page1 = Invoke-RestMethod -Uri "$baseUrl/api/projects?page=1&size=2" -Method Get -Headers $alphaHeaders
    if ($page1.content.Count -ne 1) {
        Write-Error "Page 1 size 2 failed: content count=$($page1.content.Count)"
        exit 1
    }
    Write-Host "       Server-side pagination verified (Page 0 count=2, Page 1 count=1, totalPages=2)."

    # Verify Sorting & Sort Allowlist Security
    Write-Host "[11/16] Testing sorting and sort allowlist validation..."
    $sortedByName = Invoke-RestMethod -Uri "$baseUrl/api/projects?page=0&size=10&sort=name&direction=ASC" -Method Get -Headers $alphaHeaders
    $names = $sortedByName.content | ForEach-Object { $_.name }
    Write-Host "       Ascending names: $($names -join ' -> ')"
    if ($names[0] -ne "Alpha CLI Automation Tool") {
        Write-Error "Expected 'Alpha CLI Automation Tool' first in ascending order, got '$($names[0])'"
        exit 1
    }

    # Test Disallowed Sort Parameter
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects?page=0&size=10&sort=disallowed_column" -Method Get -Headers $alphaHeaders -ErrorAction Stop
        Write-Error "FAIL: Disallowed sort column did not fail!"
        exit 1
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -ne 400) {
            Write-Error "Expected 400 for disallowed sort column, got $code"
            exit 1
        }
        Write-Host "       Disallowed sort column rejected with 400 Bad Request (SQL injection defense verified)."
    }

    # Verify Project History & Audit Subsystem
    Write-Host "[12/16] Testing Project History audit trail (GET /api/projects/{id}/history)..."
    # Project 1 was created, let's prepare it for analysis to record an ANALYSIS_STARTED event
    $prep = Invoke-RestMethod -Uri "$baseUrl/api/projects/$($alphaP1.id)/prepare-analysis" -Method Post -Headers $alphaHeaders
    if ($prep.status -ne "READY_FOR_ANALYSIS") {
        Write-Error "Expected READY_FOR_ANALYSIS status, got $($prep.status)"
        exit 1
    }

    $historyP1 = Invoke-RestMethod -Uri "$baseUrl/api/projects/$($alphaP1.id)/history" -Method Get -Headers $alphaHeaders
    if ($historyP1.totalElements -lt 2) {
        Write-Error "Expected at least 2 history records for Project 1, got $($historyP1.totalElements)"
        exit 1
    }

    $eventTypes = $historyP1.content | ForEach-Object { $_.eventType }
    Write-Host "       Project 1 History Events (newest first): $($eventTypes -join ' -> ')"
    if ($eventTypes[0] -ne "ANALYSIS_STARTED") {
        Write-Error "Newest event should be ANALYSIS_STARTED, got $($eventTypes[0])"
        exit 1
    }
    if ($historyP1.content[0].createdBy -ne "alpha@phase9.test") {
        Write-Error "History createdBy should be 'alpha@phase9.test', got $($historyP1.content[0].createdBy)"
        exit 1
    }
    Write-Host "       Project history audit logging and reverse-chronological ordering verified."

    # Verify History for Project with 0 previous events auto-seeds baseline creation event
    Write-Host "[13/16] Testing baseline history auto-seeding for clean projects..."
    $historyP2 = Invoke-RestMethod -Uri "$baseUrl/api/projects/$($alphaP2.id)/history" -Method Get -Headers $alphaHeaders
    if ($historyP2.totalElements -lt 1) {
        Write-Error "Baseline creation event should have been auto-seeded for Project 2"
        exit 1
    }
    if ($historyP2.content[0].eventType -ne "PROJECT_CREATED") {
        Write-Error "Baseline event should be PROJECT_CREATED, got $($historyP2.content[0].eventType)"
        exit 1
    }
    Write-Host "       Baseline history seeding verified."

    # Anti-IDOR Authorization Check on History and Delete
    Write-Host "[14/16] Testing Anti-IDOR protection on history and delete endpoints..."
    try {
        # Beta tries to read Alpha's history
        Invoke-RestMethod -Uri "$baseUrl/api/projects/$($alphaP1.id)/history" -Method Get -Headers $betaHeaders -ErrorAction Stop
        Write-Error "FAIL: Beta was able to read Alpha's project history!"
        exit 1
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -ne 404) {
            Write-Error "Expected 404 for cross-tenant history access, got $code"
            exit 1
        }
        Write-Host "       Anti-IDOR verified: Cross-tenant history access returns 404 Not Found."
    }

    try {
        # Beta tries to delete Alpha's project
        Invoke-RestMethod -Uri "$baseUrl/api/projects/$($alphaP1.id)" -Method Delete -Headers $betaHeaders -ErrorAction Stop
        Write-Error "FAIL: Beta was able to delete Alpha's project!"
        exit 1
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -ne 404) {
            Write-Error "Expected 404 for cross-tenant project deletion, got $code"
            exit 1
        }
        Write-Host "       Anti-IDOR verified: Cross-tenant project deletion returns 404 Not Found."
    }

    # Safe Cascade Deletion
    Write-Host "[15/16] Testing safe cascade deletion (DELETE /api/projects/{id})..."
    $delResponse = Invoke-WebRequest -Uri "$baseUrl/api/projects/$($alphaP3.id)" -Method Delete -Headers $alphaHeaders
    if ($delResponse.StatusCode -ne 204) {
        Write-Error "Expected 204 No Content for deletion, got $($delResponse.StatusCode)"
        exit 1
    }

    # Verify project is gone
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects/$($alphaP3.id)" -Method Get -Headers $alphaHeaders -ErrorAction Stop
        Write-Error "FAIL: Deleted project $($alphaP3.id) still accessible!"
        exit 1
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -ne 404) {
            Write-Error "Expected 404 for deleted project, got $code"
            exit 1
        }
    }

    # Verify Alpha's stats updated
    $alphaStatsAfter = Invoke-RestMethod -Uri "$baseUrl/api/projects/stats" -Method Get -Headers $alphaHeaders
    if ($alphaStatsAfter.totalProjects -ne 2) {
        Write-Error "Expected Alpha totalProjects to be 2 after deletion, got $($alphaStatsAfter.totalProjects)"
        exit 1
    }
    Write-Host "       Cascade deletion verified: Project removed, subsequent access returns 404, stats updated."

    # Regression: Backward Compatibility of GET /api/projects without params
    Write-Host "[16/16] Testing backward compatibility of unpaginated GET /api/projects..."
    $legacyProjects = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Get -Headers $alphaHeaders
    if ($legacyProjects -isnot [System.Array] -or $legacyProjects.Count -ne 2) {
        Write-Error "Legacy GET /api/projects should return array of 2 projects, got type: $($legacyProjects.GetType().FullName)"
        exit 1
    }
    if ($null -eq $legacyProjects[0].documentCount) {
        Write-Error "Legacy project summary should include documentCount property"
        exit 1
    }
    Write-Host "       Backward compatibility verified: Returns array without pagination wrapper."

    Write-Host ""
    Write-Host "=========================================================="
    Write-Host "   ALL PHASE 9 LIVE VERIFICATION TESTS PASSED (16/16)!    "
    Write-Host "=========================================================="
} finally {
    Write-Host "Stopping Spring Boot Backend process (PID: $($proc.Id))..."
    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}
