$port = 8089
$baseUrl = "http://localhost:$port"
$logFile = "scratch/backend_live_phase4.log"

Write-Host "=========================================================="
Write-Host "   ProjectForge AI - Phase 4 Live End-to-End Verification  "
Write-Host "=========================================================="

Write-Host "[1/9] Launching Spring Boot Backend on port $port..."

if (Test-Path $logFile) {
    Remove-Item $logFile -Force
}

$javaBin = if ($env:JAVA_HOME) { "$env:JAVA_HOME\bin\java.exe" } else { "java" }

$proc = Start-Process -FilePath $javaBin -ArgumentList @(
    "-jar", "backend/target/projectforge-backend-1.0.0-SNAPSHOT.jar",
    "--server.port=$port",
    "--app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "--app.jwt.expiration-ms=86400000",
    "--spring.datasource.url=jdbc:h2:mem:livephase4db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "--spring.datasource.driver-class-name=org.h2.Driver",
    "--spring.datasource.username=sa",
    "--spring.datasource.password=",
    "--spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "--spring.jpa.hibernate.ddl-auto=create-drop",
    "--logging.level.com.projectforge=INFO"
) -PassThru -NoNewWindow -RedirectStandardOutput $logFile -RedirectStandardError "scratch/backend_live_phase4_err.log"

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
        if (Test-Path "scratch/backend_live_phase4_err.log") { Get-Content "scratch/backend_live_phase4_err.log" -Tail 30 }
        exit 1
    }

    Write-Host "[2/9] Health check UP: $($health.service) (200 OK)"

    # Test Unauthenticated access to /api/projects/1/analyze
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects/1/analyze" -Method Post -ErrorAction Stop
        Write-Error "FAIL: Unauthenticated access to /analyze was allowed!"
        exit 1
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 401) {
            Write-Host "[3/9] Unauthenticated access to /api/projects/1/analyze correctly rejected with 401 Unauthorized."
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
    Write-Host "[4/9] Registered and logged in User Alpha (JWT generated)."

    # Register & Login User Beta
    $regBetaPayload = @{ name = "Beta User"; email = "beta@example.com"; password = "Password123!" } | ConvertTo-Json
    $regBeta = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBetaPayload -ContentType "application/json"

    $loginBetaPayload = @{ email = "beta@example.com"; password = "Password123!" } | ConvertTo-Json
    $loginBeta = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBetaPayload -ContentType "application/json"
    $tokenBeta = $loginBeta.token
    Write-Host "[4/9] Registered and logged in User Beta (JWT generated)."

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
    Write-Host "[5/9] Alpha created Project 1 (id: $proj1Id, name: '$($proj1.name)', status: '$($proj1.status)')."

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
    Write-Host "[5/9] Beta created Project 2 (id: $proj2Id, name: '$($proj2.name)', status: '$($proj2.status)')."

    # IDOR Tests: Alpha attempts to access Beta's Project 2 across Phase 4 endpoints
    $clarifyBody = @{
        answers = @(
            @{ questionId = "Q1"; answer = "Attacker answer attempting cross-tenant injection" }
        )
    } | ConvertTo-Json

    $phase4IdorEndpoints = @(
        @{ Method = "Post"; Uri = "$baseUrl/api/projects/$proj2Id/analyze"; Body = $null; Desc = "POST /analyze Project 2" },
        @{ Method = "Get"; Uri = "$baseUrl/api/projects/$proj2Id/requirements"; Body = $null; Desc = "GET /requirements Project 2" },
        @{ Method = "Post"; Uri = "$baseUrl/api/projects/$proj2Id/clarifications"; Body = $clarifyBody; Desc = "POST /clarifications Project 2" }
    )

    Write-Host "[6/9] Verifying Phase 4 Anti-IDOR Defense on foreign Project 2..."
    foreach ($test in $phase4IdorEndpoints) {
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

    # Step 7: Check validation before analysis has run and test AI failure rollback
    Write-Host "[7/9] Testing pre-analysis guards and AI service failure rollback on Project 1..."
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects/$proj1Id/requirements" -Method Get -Headers $headersAlpha -ErrorAction Stop
        Write-Error "FAIL: Expected 400 Bad Request when getting requirements for unanalyzed project!"
        exit 1
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -eq 400) {
            Write-Host "   Verified GET /requirements on unanalyzed project correctly returned 400 Bad Request."
        }
    }

    # Test POST /analyze without OPENAI_API_KEY -> Verify graceful 503 and status rollback
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects/$proj1Id/analyze" -Method Post -Headers $headersAlpha -ErrorAction Stop
        Write-Error "FAIL: Expected 503 Service Unavailable when OPENAI_API_KEY is not configured!"
        exit 1
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -eq 503) {
            Write-Host "   Verified POST /analyze without configured OPENAI_API_KEY returns 503 Service Unavailable (no fake AI)."
        } else {
            Write-Error "FAIL: Expected 503 Service Unavailable, got $code"
            exit 1
        }
    }

    # Verify status rollback to READY_FOR_ANALYSIS or DRAFT
    $proj1AfterFailure = Invoke-RestMethod -Uri "$baseUrl/api/projects/$proj1Id" -Method Get -Headers $headersAlpha
    if ($proj1AfterFailure.status -ne "READY_FOR_ANALYSIS" -and $proj1AfterFailure.status -ne "DRAFT") {
        Write-Error "FAIL: Status was not rolled back after analysis failure! Got: $($proj1AfterFailure.status)"
        exit 1
    }
    Write-Host "   Verified Project 1 status was safely rolled back to $($proj1AfterFailure.status) on AI failure."

    # Step 8: Update structured specification JSON on Project 1 & verify GET /requirements
    Write-Host "[8/9] Storing structured specification and testing GET /requirements..."
    $validSpecJson = @{
        specification = @{
            schemaVersion = "1.0.0"
            project = @{
                name = "Alpha Cloud Gateway"
                goal = "High-performance distributed telemetry and API gateway"
                problem = "Microservices require resilient traffic management"
                type = "API_BACKEND"
                difficulty = "ADVANCED"
            }
            users = @(
                @{
                    role = "DevOps Engineer"
                    description = "Configures rate limits and routing rules"
                    responsibilities = @("Manage route configs", "Monitor latency")
                    permissions = @("MANAGE_ROUTES", "VIEW_METRICS")
                }
            )
            roles = @("DevOps Engineer", "Security Admin")
            features = @(
                @{ id = "FEAT-1"; name = "Token Bucket Rate Limiting"; description = "Rate limits requests"; category = "CORE"; priority = "HIGH" },
                @{ id = "FEAT-2"; name = "Circuit Breaking"; description = "Protects upstream microservices"; category = "CORE"; priority = "HIGH" },
                @{ id = "FEAT-3"; name = "Distributed Tracing"; description = "Propagates trace headers"; category = "SUPPORTING"; priority = "MEDIUM" }
            )
            modules = @(
                @{ name = "TrafficRouter"; description = "Routes ingress traffic"; responsibilities = @("Dispatch requests"); dependencies = @() },
                @{ name = "RateLimiter"; description = "Calculates token buckets"; responsibilities = @("Enforce RPS limits"); dependencies = @("TrafficRouter") }
            )
            technology = @{
                frontend = @("React", "Vite")
                backend = @("Java 17", "Spring Boot 3")
                database = @("PostgreSQL", "Redis")
                ai = @()
                deployment = @("Docker", "Kubernetes")
                other = @("Prometheus")
            }
            architecture = @{
                style = "Modular Monolith"
                components = @("Traffic Router", "Rate Limiter", "Config Store")
                dataFlow = @("Ingress -> Token Bucket -> Proxy -> Backend")
            }
            database = @{
                entities = @(
                    @{ name = "routes"; fields = @("id BIGSERIAL", "path VARCHAR(200)", "upstream_url VARCHAR(500)"); primaryKey = "id"; foreignKeys = @(); relationships = @(); constraints = @() },
                    @{ name = "rate_limit_policies"; fields = @("id BIGSERIAL", "rps_limit INT", "burst_capacity INT"); primaryKey = "id"; foreignKeys = @(); relationships = @(); constraints = @() }
                )
                relationships = @("One route associates with one rate limit policy")
            }
            apis = @(
                @{ method = "POST"; path = "/api/gateway/routes"; purpose = "Define new gateway route"; authenticationRequired = $true; requestBody = "{ path, upstreamUrl }"; responseBody = "{ id, status }" },
                @{ method = "GET"; path = "/api/gateway/health"; purpose = "Gateway health check"; authenticationRequired = $false; requestBody = $null; responseBody = "{ status: 'UP' }" }
            )
            security = @{
                authentication = "JWT with HS-512 signing"
                authorization = "Role-Based Access Control"
                securityRequirements = @("Enforce HTTPS/TLS 1.3", "Rate limit at 50k RPS", "Zero-trust network segmentation")
            }
            testing = @{
                unitTesting = @("JUnit 5", "AssertJ", "Mockito")
                integrationTesting = @("Testcontainers", "MockMvc")
                securityTesting = @("OWASP ZAP scans")
            }
            deployment = @{
                environment = "Kubernetes on GCP GKE"
                requirements = @("PostgreSQL 16", "Redis 7 Cluster")
            }
            nonFunctionalRequirements = @("Sub-5ms p99 overhead", "99.999% uptime")
            constraints = @("Zero external telemetry leakage")
            assumptions = @("Redis cluster is deployed in same VPC")
            openQuestions = @(
                @{
                    id = "Q1"
                    question = "Should Redis cluster failover allow degraded pass-through without rate limiting?"
                    importance = "HIGH"
                    reason = "Decides between fail-open or fail-closed security posture during network partition"
                }
            )
            risks = @(
                @{
                    risk = "Redis saturation under traffic burst"
                    severity = "HIGH"
                    mitigation = "Local in-memory token cache fallback"
                }
            )
            qualityScore = 88
        }
    } | ConvertTo-Json -Depth 10

    $updatedSpec = Invoke-RestMethod -Uri "$baseUrl/api/projects/$proj1Id/specification" -Method Put -Headers $headersAlpha -Body $validSpecJson -ContentType "application/json"

    # Now call GET /api/projects/1/requirements
    $reqResp = Invoke-RestMethod -Uri "$baseUrl/api/projects/$proj1Id/requirements" -Method Get -Headers $headersAlpha
    if ($reqResp.projectId -ne $proj1Id -or $reqResp.qualityScore -lt 80 -or $reqResp.openQuestions.Count -ne 1) {
        Write-Error "FAIL: GET /requirements returned unexpected data!"
        exit 1
    }
    Write-Host "   GET /requirements retrieved valid specification (qualityScore: $($reqResp.qualityScore), openQuestions: $($reqResp.openQuestions.Count))."

    # Step 9: Submit Clarification validation
    Write-Host "[9/9] Testing Clarification submission and Project cleanup..."
    $invalidClarification = @{ answers = @() } | ConvertTo-Json
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects/$proj1Id/clarifications" -Method Post -Headers $headersAlpha -Body $invalidClarification -ContentType "application/json" -ErrorAction Stop
        Write-Error "FAIL: Expected 400 Bad Request on empty clarification answers!"
        exit 1
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 400) {
            Write-Host "   Empty clarification answers correctly rejected with 400 Bad Request."
        }
    }

    # Delete Project 1 and verify multi-tenant cleanliness
    $deleteResp = Invoke-WebRequest -Uri "$baseUrl/api/projects/$proj1Id" -Method Delete -Headers $headersAlpha -UseBasicParsing
    if ($deleteResp.StatusCode -ne 204) {
        Write-Error "FAIL: Expected 204 No Content for DELETE, got $($deleteResp.StatusCode)"
        exit 1
    }
    Write-Host "   Project 1 deleted cleanly (204 No Content)."

    # Verify Beta's Project 2 remains intact
    $betaProj2 = Invoke-RestMethod -Uri "$baseUrl/api/projects/$proj2Id" -Method Get -Headers $headersBeta
    if ($betaProj2.name -ne "Beta Mobile Wallet") {
        Write-Error "FAIL: Beta's project was corrupted!"
        exit 1
    }
    Write-Host "   Verified Beta's Project 2 remains intact."

    Write-Host "`n>>> ALL 9/9 LIVE PHASE 4 VERIFICATION CHECKS PASSED SUCCESSFULLY! <<<"

} finally {
    Write-Host "`nStopping backend process (PID $($proc.Id))..."
    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    Write-Host "Backend process stopped cleanly."
}
