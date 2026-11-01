$port = 8089
$baseUrl = "http://localhost:$port"
$logFile = "scratch/backend_live.log"

Write-Host "=========================================================="
Write-Host "   ProjectForge AI - Phase 3 Live End-to-End Verification  "
Write-Host "=========================================================="

Write-Host "[1/8] Launching Spring Boot Backend on port $port..."

if (Test-Path $logFile) {
    Remove-Item $logFile -Force
}

$javaBin = if ($env:JAVA_HOME) { "$env:JAVA_HOME\bin\java.exe" } else { "java" }

$proc = Start-Process -FilePath $javaBin -ArgumentList @(
    "-jar", "backend/target/projectforge-backend-1.0.0-SNAPSHOT.jar",
    "--server.port=$port",
    "--app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "--app.jwt.expiration-ms=86400000",
    "--spring.datasource.url=jdbc:h2:mem:livedb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "--spring.datasource.driver-class-name=org.h2.Driver",
    "--spring.datasource.username=sa",
    "--spring.datasource.password=",
    "--spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "--spring.jpa.hibernate.ddl-auto=create-drop",
    "--logging.level.com.projectforge=INFO"
) -PassThru -NoNewWindow -RedirectStandardOutput $logFile -RedirectStandardError "scratch/backend_live_err.log"

try {
    # Poll for health check
    $started = $false
    for ($i = 0; $i -lt 30; $i++) {
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
        if (Test-Path "scratch/backend_live_err.log") { Get-Content "scratch/backend_live_err.log" -Tail 30 }
        exit 1
    }

    Write-Host "[2/8] Health check UP: $($health.service) (200 OK)"

    # Test Unauthenticated access to /api/projects
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Get -ErrorAction Stop
        Write-Error "FAIL: Unauthenticated access was allowed!"
        exit 1
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 401) {
            Write-Host "[3/8] Unauthenticated access to /api/projects correctly rejected with 401 Unauthorized."
        } else {
            Write-Error "FAIL: Expected 401 Unauthorized, got $statusCode"
            exit 1
        }
    }

    # Register & Login User Alpha
    $regAlphaPayload = @{ name = "Alpha User"; email = "alpha@example.com"; password = "Password123!" } | ConvertTo-Json
    $regAlpha = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regAlphaPayload -ContentType "application/json"
    
    $loginAlphaPayload = @{ email = "alpha@example.com"; password = "Password123!" } | ConvertTo-Json
    $loginAlpha = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginAlphaPayload -ContentType "application/json"
    $tokenAlpha = $loginAlpha.token
    Write-Host "[4/8] Registered and logged in User Alpha (JWT generated)."

    # Register & Login User Beta
    $regBetaPayload = @{ name = "Beta User"; email = "beta@example.com"; password = "Password123!" } | ConvertTo-Json
    $regBeta = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBetaPayload -ContentType "application/json"

    $loginBetaPayload = @{ email = "beta@example.com"; password = "Password123!" } | ConvertTo-Json
    $loginBeta = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBetaPayload -ContentType "application/json"
    $tokenBeta = $loginBeta.token
    Write-Host "[4/8] Registered and logged in User Beta (JWT generated)."

    # Headers
    $headersAlpha = @{ Authorization = "Bearer $tokenAlpha" }
    $headersBeta = @{ Authorization = "Bearer $tokenBeta" }

    # Alpha creates Project 1 with all 8 intake fields
    $createProj1Payload = @{
        name = "Alpha Cloud Gateway"
        description = "A high-performance cloud API gateway with circuit breakers and token buckets"
        problemStatement = "Microservices require resilient egress and ingress management under high load"
        targetUsers = "DevOps Engineers and Cloud Architects"
        projectType = "API_BACKEND"
        difficulty = "ADVANCED"
        preferredTechStack = "Java 17, Spring Boot, Redis, PostgreSQL"
        constraints = "Must sustain 50k RPS with sub-10ms latency"
    } | ConvertTo-Json

    $proj1 = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Post -Headers $headersAlpha -Body $createProj1Payload -ContentType "application/json"
    $proj1Id = $proj1.id
    if ($proj1.status -ne "DRAFT") {
        Write-Error "FAIL: Initial status expected DRAFT, got $($proj1.status)"
        exit 1
    }
    Write-Host "[5/8] Alpha created Project 1 (id: $proj1Id, name: '$($proj1.name)', status: '$($proj1.status)')."

    # Beta creates Project 2
    $createProj2Payload = @{
        name = "Beta Mobile Wallet"
        description = "Secure mobile wallet for offline contactless payments and receipts"
        problemStatement = "High latency mobile payments fail in low-connectivity areas"
        targetUsers = "Mobile Consumers"
        projectType = "MOBILE_APPLICATION"
        difficulty = "INTERMEDIATE"
        preferredTechStack = "Kotlin, Android Jetpack, SQLite"
        constraints = "Zero internet connectivity for point-of-sale exchange"
    } | ConvertTo-Json

    $proj2 = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Post -Headers $headersBeta -Body $createProj2Payload -ContentType "application/json"
    $proj2Id = $proj2.id
    Write-Host "[5/8] Beta created Project 2 (id: $proj2Id, name: '$($proj2.name)', status: '$($proj2.status)')."

    # Verify List isolation
    $alphaList = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Get -Headers $headersAlpha
    $betaList = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Get -Headers $headersBeta

    if ($alphaList.Count -ne 1 -or $alphaList[0].id -ne $proj1Id) {
        Write-Error "FAIL: Alpha's project list is corrupted or leaked."
        exit 1
    }
    if ($betaList.Count -ne 1 -or $betaList[0].id -ne $proj2Id) {
        Write-Error "FAIL: Beta's project list is corrupted or leaked."
        exit 1
    }
    Write-Host "[6/8] Strict List Isolation verified: Alpha sees only Project 1, Beta sees only Project 2."

    # IDOR Tests: Alpha attempts to access Beta's Project 2 across all operations
    $validUpdatePayload = @{
        name = "Hacked Project"
        description = "Valid updated project idea text"
        projectType = "WEB_APPLICATION"
        difficulty = "INTERMEDIATE"
    } | ConvertTo-Json

    $idorEndpoints = @(
        @{ Method = "Get"; Uri = "$baseUrl/api/projects/$proj2Id"; Desc = "GET Project 2" },
        @{ Method = "Put"; Uri = "$baseUrl/api/projects/$proj2Id"; Body = $validUpdatePayload; Desc = "PUT Project 2" },
        @{ Method = "Delete"; Uri = "$baseUrl/api/projects/$proj2Id"; Desc = "DELETE Project 2" },
        @{ Method = "Post"; Uri = "$baseUrl/api/projects/$proj2Id/prepare-analysis"; Desc = "POST /prepare-analysis Project 2" },
        @{ Method = "Get"; Uri = "$baseUrl/api/projects/$proj2Id/specification"; Desc = "GET Project 2 Specification" },
        @{ Method = "Put"; Uri = "$baseUrl/api/projects/$proj2Id/specification"; Body = (@{ specification = @{ key = "val" } } | ConvertTo-Json); Desc = "PUT Project 2 Specification" }
    )

    foreach ($test in $idorEndpoints) {
        try {
            if ($test.Body) {
                Invoke-RestMethod -Uri $test.Uri -Method $test.Method -Headers $headersAlpha -Body $test.Body -ContentType "application/json" -ErrorAction Stop
            } else {
                Invoke-RestMethod -Uri $test.Uri -Method $test.Method -Headers $headersAlpha -ErrorAction Stop
            }
            Write-Error "FAIL: IDOR security breach on $($test.Desc)! Expected 404 Not Found."
            exit 1
        } catch {
            $code = $_.Exception.Response.StatusCode.value__
            if ($code -eq 404) {
                Write-Host "   IDOR Defense Passed: Alpha -> $($test.Desc) resulted in HTTP 404 Not Found (Zero Leakage)."
            } else {
                Write-Error "FAIL: Expected 404 Not Found for $($test.Desc), got $code"
                exit 1
            }
        }
    }

    # Step 7: Test POST /api/projects/{id}/prepare-analysis on Project 1
    Write-Host "[7/8] Testing POST /api/projects/$proj1Id/prepare-analysis..."
    $prepResp = Invoke-RestMethod -Uri "$baseUrl/api/projects/$proj1Id/prepare-analysis" -Method Post -Headers $headersAlpha
    if ($prepResp.status -ne "READY_FOR_ANALYSIS") {
        Write-Error "FAIL: Expected status READY_FOR_ANALYSIS, got $($prepResp.status)"
        exit 1
    }
    Write-Host "   Successfully transitioned Project 1 to READY_FOR_ANALYSIS (200 OK)."

    # Specification Update & Retrieval for Alpha's Project 1
    $specPayload = @{
        specification = @{
            schemaVersion = "1.0.0"
            overview = @{ title = "Alpha Gateway Spec"; status = "READY_FOR_ANALYSIS" }
            modules = @("Routing", "RateLimiter", "Telemetry")
        }
    } | ConvertTo-Json -Depth 5

    $updatedSpec = Invoke-RestMethod -Uri "$baseUrl/api/projects/$proj1Id/specification" -Method Put -Headers $headersAlpha -Body $specPayload -ContentType "application/json"
    $retrievedSpec = Invoke-RestMethod -Uri "$baseUrl/api/projects/$proj1Id/specification" -Method Get -Headers $headersAlpha

    if ($retrievedSpec.specification.schemaVersion -ne "1.0.0") {
        Write-Error "FAIL: Specification update/retrieval verification failed."
        exit 1
    }
    Write-Host "   Specification storage & retrieval verified for Project 1."

    # Update Project 1 while in READY_FOR_ANALYSIS (editable)
    $updateProj1Payload = @{
        name = "Alpha Cloud Gateway Pro"
        description = "Enhanced high-performance cloud API gateway with distributed tracing"
        problemStatement = "Microservices require resilient egress and ingress management under high load"
        targetUsers = "DevOps Engineers and Cloud Architects"
        projectType = "FULL_STACK_APPLICATION"
        difficulty = "EXPERT"
        preferredTechStack = "Java 21, Spring Boot 3, Redis Cluster, PostgreSQL 16"
        constraints = "Must sustain 100k RPS with sub-5ms latency"
    } | ConvertTo-Json

    $updatedProj1 = Invoke-RestMethod -Uri "$baseUrl/api/projects/$proj1Id" -Method Put -Headers $headersAlpha -Body $updateProj1Payload -ContentType "application/json"
    if ($updatedProj1.name -ne "Alpha Cloud Gateway Pro" -or $updatedProj1.status -ne "READY_FOR_ANALYSIS") {
        Write-Error "FAIL: Project update failed or status corrupted."
        exit 1
    }
    Write-Host "   Project update during READY_FOR_ANALYSIS succeeded: $($updatedProj1.name) (difficulty: $($updatedProj1.difficulty))."

    # Step 8: Delete Project 1 and verify isolation & cleanliness
    Write-Host "[8/8] Testing DELETE and final multi-tenant isolation..."
    $deleteResp = Invoke-WebRequest -Uri "$baseUrl/api/projects/$proj1Id" -Method Delete -Headers $headersAlpha -UseBasicParsing
    if ($deleteResp.StatusCode -ne 204) {
        Write-Error "FAIL: Expected 204 No Content for DELETE, got $($deleteResp.StatusCode)"
        exit 1
    }
    Write-Host "   Project 1 deleted (204 No Content)."

    # Verify Project 1 is gone
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects/$proj1Id" -Method Get -Headers $headersAlpha -ErrorAction Stop
        Write-Error "FAIL: Project 1 still exists after deletion!"
        exit 1
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 404) {
            Write-Host "   Verified Project 1 is 404 Not Found after deletion."
        }
    }

    # Verify Beta's Project 2 is intact
    $betaProj2 = Invoke-RestMethod -Uri "$baseUrl/api/projects/$proj2Id" -Method Get -Headers $headersBeta
    if ($betaProj2.name -ne "Beta Mobile Wallet") {
        Write-Error "FAIL: Beta's project was corrupted!"
        exit 1
    }
    Write-Host "   Verified Beta's Project 2 remains intact."

    Write-Host "`n>>> ALL 8/8 LIVE VERIFICATION CHECKS PASSED SUCCESSFULLY! <<<"

} finally {
    Write-Host "`nStopping backend process (PID $($proc.Id))..."
    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    Write-Host "Backend process stopped cleanly."
}
