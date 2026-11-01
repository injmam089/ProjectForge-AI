$port = 8092
$baseUrl = "http://localhost:$port"
$logFile = "scratch/backend_live_phase7.log"
$errLogFile = "scratch/backend_live_phase7_err.log"

Write-Host "=========================================================="
Write-Host "   ProjectForge AI - Phase 7 Live End-to-End Verification  "
Write-Host "=========================================================="

Write-Host "[1/13] Launching Spring Boot Backend on port $port..."

if (Test-Path $logFile) { Remove-Item $logFile -Force }
if (Test-Path $errLogFile) { Remove-Item $errLogFile -Force }

$javaBin = if ($env:JAVA_HOME) { "$env:JAVA_HOME\bin\java.exe" } else { "java" }

$proc = Start-Process -FilePath $javaBin -ArgumentList @(
    "-jar", "backend/target/projectforge-backend-1.0.0-SNAPSHOT.jar",
    "--server.port=$port",
    "--app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "--app.jwt.expiration-ms=86400000",
    "--spring.datasource.url=jdbc:h2:mem:livephase7db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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

    Write-Host "[2/13] Health check UP: $($health.service) (200 OK)"

    # Test Unauthenticated access to Phase 7 endpoints
    Write-Host "[3/13] Verifying 401 Unauthorized on unauthenticated Phase 7 endpoints..."
    $endpointsToTest401 = @(
        @{ Method = "Get"; Uri = "$baseUrl/api/projects/1/workspace" },
        @{ Method = "Put"; Uri = "$baseUrl/api/documents/1" },
        @{ Method = "Get"; Uri = "$baseUrl/api/documents/1/versions" },
        @{ Method = "Get"; Uri = "$baseUrl/api/documents/1/versions/1" },
        @{ Method = "Post"; Uri = "$baseUrl/api/documents/1/restore/1" },
        @{ Method = "Post"; Uri = "$baseUrl/api/documents/1/regenerate" },
        @{ Method = "Post"; Uri = "$baseUrl/api/documents/1/ai-edit" }
    )

    foreach ($ep in $endpointsToTest401) {
        try {
            Invoke-RestMethod -Uri $ep.Uri -Method $ep.Method -ErrorAction Stop
            Write-Error "FAIL: Unauthenticated $($ep.Method) $($ep.Uri) succeeded!"
            exit 1
        } catch {
            if ($_.Exception.Response.StatusCode.value__ -ne 401) {
                Write-Error "Expected 401 for $($ep.Uri), got $($_.Exception.Response.StatusCode.value__)"
                exit 1
            }
        }
    }
    Write-Host "  -> All 7 unauthenticated Phase 7 endpoints correctly rejected with 401 Unauthorized."

    # Register & Login Alpha & Beta
    Write-Host "[4/13] Registering and logging in User Alpha and User Beta..."
    $alphaReg = @{ email = "alpha@phase7.test"; password = "Password123!"; name = "User Alpha" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $alphaReg -ContentType "application/json" | Out-Null
    $alphaLogin = @{ email = "alpha@phase7.test"; password = "Password123!" } | ConvertTo-Json
    $alphaAuth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $alphaLogin -ContentType "application/json"
    $alphaToken = $alphaAuth.token
    $alphaHeaders = @{ Authorization = "Bearer $alphaToken" }

    $betaReg = @{ email = "beta@phase7.test"; password = "Password123!"; name = "User Beta" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $betaReg -ContentType "application/json" | Out-Null
    $betaLogin = @{ email = "beta@phase7.test"; password = "Password123!" } | ConvertTo-Json
    $betaAuth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $betaLogin -ContentType "application/json"
    $betaToken = $betaAuth.token
    $betaHeaders = @{ Authorization = "Bearer $betaToken" }

    Write-Host "  -> Users authenticated. Alpha token length: $($alphaToken.Length), Beta token length: $($betaToken.Length)"

    # Create and prepare Alpha's project
    Write-Host "[5/13] Creating, reviewing, approving project and generating Phase 6 documents..."
    $createBody = @{
        name = "CloudMatrix Microservices Platform"
        description = "Enterprise distributed system with real-time analytics, event streaming, and multi-tenant security architecture."
        problemStatement = "DevOps engineers require unified observability and multi-tenant stream processing under heavy load."
        targetUsers = "DevOps Engineers and Cloud Architects"
        projectType = "WEB_APPLICATION"
        difficulty = "ADVANCED"
        preferredTechStack = "Java 21, Spring Boot 3, PostgreSQL, Apache Kafka, Docker, React"
        specialRequirements = "Sub-100ms API response latency, GDPR compliance, high resilience"
    } | ConvertTo-Json
    $project = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Post -Headers $alphaHeaders -Body $createBody -ContentType "application/json"
    $projId = $project.id

    # Set structured specification JSON via PUT /api/projects/{id}/review
    $validSpec = @{
        specification = @{
            schemaVersion = "1.0.0"
            project = @{
                name = "CloudMatrix Microservices Platform"
                goal = "Enterprise distributed system with real-time analytics and event streaming"
                problem = "DevOps engineers require unified observability and multi-tenant stream processing"
                type = "WEB_APPLICATION"
                difficulty = "ADVANCED"
            }
            users = @(
                @{
                    role = "Cloud Architect"
                    description = "Configures microservice pipelines and streaming topology"
                    responsibilities = @("Deploy clusters", "Monitor telemetry")
                    permissions = @("MANAGE_PIPELINES", "VIEW_METRICS")
                }
            )
            roles = @("Cloud Architect", "Security Operator")
            features = @(
                @{ id = "FEAT-1"; name = "OAuth2 Gateway"; description = "Centralized authentication gateway"; category = "SECURITY"; priority = "HIGH" },
                @{ id = "FEAT-2"; name = "Event Ingestion"; description = "Kafka event stream consumer"; category = "CORE"; priority = "HIGH" },
                @{ id = "FEAT-3"; name = "Metrics Dashboard"; description = "Real-time React dashboard with Recharts"; category = "UI"; priority = "MEDIUM" }
            )
            modules = @(
                @{ name = "GatewayModule"; description = "Performs token validation and rate limiting"; responsibilities = @("Filter requests"); dependencies = @() },
                @{ name = "StreamModule"; description = "Consumes Kafka topics"; responsibilities = @("Process event batches"); dependencies = @("GatewayModule") }
            )
            technology = @{
                frontend = @("React", "Vite", "TailwindCSS")
                backend = @("Java 21", "Spring Boot 3")
                database = @("PostgreSQL", "Redis")
                ai = @()
                deployment = @("Docker", "Kubernetes")
                other = @("Apache Kafka", "OpenTelemetry")
            }
            architecture = @{
                style = "Microservices Architecture"
                components = @("API Gateway", "Event Ingestion Service", "Query Service", "Web Client")
                dataFlow = @("Client -> Gateway -> Event Consumer -> PostgreSQL / Kafka")
            }
            database = @{
                entities = @(
                    @{ name = "events"; fields = @("id BIGSERIAL", "topic VARCHAR(100)", "payload JSONB", "created_at TIMESTAMPTZ"); primaryKey = "id"; foreignKeys = @(); relationships = @(); constraints = @() },
                    @{ name = "tenants"; fields = @("id BIGSERIAL", "name VARCHAR(100)", "active BOOLEAN"); primaryKey = "id"; foreignKeys = @(); relationships = @(); constraints = @() }
                )
                relationships = @("One tenant produces multiple stream events")
            }
            apis = @(
                @{ method = "POST"; path = "/api/events"; purpose = "Submit raw event message"; authenticationRequired = $true; requestBody = "{ topic, payload }"; responseBody = "{ id, status }" },
                @{ method = "GET"; path = "/api/health"; purpose = "Platform health probe"; authenticationRequired = $false; requestBody = $null; responseBody = "{ status: 'UP' }" }
            )
            security = @{
                authentication = "JWT with HS-512 signing"
                authorization = "Role-Based Access Control (RBAC)"
                securityRequirements = @("TLS 1.3 enforced across all ingress", "Strict schema validation on event payloads")
            }
            testing = @{
                unitTesting = @("JUnit 5", "AssertJ", "Mockito")
                integrationTesting = @("Testcontainers PostgreSQL", "MockMvc")
                securityTesting = @("OWASP ZAP scans", "SonarQube SAST")
            }
            deployment = @{
                environment = "Kubernetes on AWS EKS"
                requirements = @("PostgreSQL 16", "Apache Kafka 3.6", "4GB RAM per service pod")
            }
            nonFunctionalRequirements = @("Sub-50ms p95 latency", "99.95% uptime SLA")
            constraints = @("Compliance with GDPR data storage policies")
            assumptions = @("Events formatted as JSON or CloudEvents v1.0")
            openQuestions = @()
            risks = @(
                @{ risk = "Kafka consumer lag during peak burst traffic"; severity = "HIGH"; mitigation = "Autoscale worker pods based on consumer group lag metrics" }
            )
            qualityScore = 92
        }
    } | ConvertTo-Json -Depth 10

    Invoke-RestMethod -Uri "$baseUrl/api/projects/$projId/review" -Method Put -Headers $alphaHeaders -Body $validSpec -ContentType "application/json" | Out-Null

    # User Alpha explicitly approves project
    $approveBody = @{ confirmation = $true } | ConvertTo-Json
    $approved = Invoke-RestMethod -Uri "$baseUrl/api/projects/$projId/approve" -Method Post -Headers $alphaHeaders -Body $approveBody -ContentType "application/json"
    if ($approved.status -ne "APPROVED") {
        Write-Error "Project status expected APPROVED, got $($approved.status)"
        exit 1
    }

    # Generate documents (Phase 6)
    $genDocs = Invoke-RestMethod -Uri "$baseUrl/api/projects/$projId/generate" -Method Post -Headers $alphaHeaders
    Write-Host "  -> Phase 6 generation complete. Generated $($genDocs.totalDocuments) documents with status: $($genDocs.status)"

    # Verify initial workspace
    Write-Host "[6/13] Testing GET /api/projects/$projId/workspace..."
    $workspace = Invoke-RestMethod -Uri "$baseUrl/api/projects/$projId/workspace" -Method Get -Headers $alphaHeaders
    if ($workspace.projectId -ne $projId -or $workspace.documents.Count -lt 14) {
        Write-Error "Workspace verification failed. Got $($workspace.documents.Count) documents."
        exit 1
    }
    Write-Host "  -> Workspace verified: Project '$($workspace.projectName)' with $($workspace.documents.Count) documents."

    # Pick README document for workspace testing
    $readmeDoc = $workspace.documents | Where-Object { $_.filename -eq "README.md" }
    if (-not $readmeDoc) {
        $readmeDoc = $workspace.documents[0]
    }
    $targetDocId = $readmeDoc.id
    $initialDoc = Invoke-RestMethod -Uri "$baseUrl/api/documents/$targetDocId" -Method Get -Headers $alphaHeaders
    $initialContent = $initialDoc.content
    Write-Host "  -> Target document: $($readmeDoc.filename) (ID: $targetDocId, version: $($readmeDoc.version), initial length: $($initialContent.Length) chars)"

    # Store exact specification JSON to test immutability later
    $originalSpec = Invoke-RestMethod -Uri "$baseUrl/api/projects/$projId/specification" -Method Get -Headers $alphaHeaders
    $originalSpecString = $originalSpec | ConvertTo-Json -Depth 10 -Compress

    # Test Validation on PUT /api/documents/{id}
    Write-Host "[7/13] Testing validation & security defenses on PUT /api/documents/$targetDocId..."

    # Test empty content
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/documents/$targetDocId" -Method Put -Headers $alphaHeaders -ContentType "application/json" -Body (@{
            content = "   "
        } | ConvertTo-Json) -ErrorAction Stop
        Write-Error "FAIL: Empty content edit was accepted!"
        exit 1
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 400) {
            Write-Error "Expected 400 for empty content, got $($_.Exception.Response.StatusCode.value__)"
            exit 1
        }
    }

    # Test script injection
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/documents/$targetDocId" -Method Put -Headers $alphaHeaders -ContentType "application/json" -Body (@{
            content = "# CloudMatrix Title`n<script>alert('xss')</script>`nValid length content that contains XSS attack payload."
        } | ConvertTo-Json) -ErrorAction Stop
        Write-Error "FAIL: Dangerous XSS script injection was accepted!"
        exit 1
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 400) {
            Write-Error "Expected 400 for XSS payload, got $($_.Exception.Response.StatusCode.value__)"
            exit 1
        }
    }
    Write-Host "  -> Content safety validation verified (rejected empty content & dangerous HTML tags)."

    # Test Valid Manual Edit
    Write-Host "[8/13] Executing valid manual edit (PUT /api/documents/$targetDocId)..."
    $editedContent = "# CloudMatrix Microservices Platform (v2 Manual)`n`nThis is an updated manual edit of the project overview with detailed architecture notes and runtime configuration parameters."
    $saveResponse = Invoke-RestMethod -Uri "$baseUrl/api/documents/$targetDocId" -Method Put -Headers $alphaHeaders -ContentType "application/json" -Body (@{
        content = $editedContent
    } | ConvertTo-Json)

    if ($saveResponse.version -ne 2) {
        Write-Error "Expected version 2 after manual edit, got $($saveResponse.version)"
        exit 1
    }
    Write-Host "  -> Document successfully saved as version $($saveResponse.version) ($($saveResponse.characterCount) chars)."

    # Verify GET /api/documents/{id} reflects updated content and version
    $fetchedDoc = Invoke-RestMethod -Uri "$baseUrl/api/documents/$targetDocId" -Method Get -Headers $alphaHeaders
    if ($fetchedDoc.version -ne 2 -or $fetchedDoc.content -ne $editedContent) {
        Write-Error "GET /api/documents/$targetDocId did not return updated content/version."
        exit 1
    }
    Write-Host "  -> Verified active document reflects version 2 and updated content."

    # Test Version History Listing (GET /api/documents/{id}/versions)
    Write-Host "[9/13] Testing version history listing (GET /api/documents/$targetDocId/versions)..."
    $versions = Invoke-RestMethod -Uri "$baseUrl/api/documents/$targetDocId/versions" -Method Get -Headers $alphaHeaders
    if ($versions.Count -ne 2) {
        Write-Error "Expected 2 versions in history, got $($versions.Count)"
        exit 1
    }
    if ($versions[0].version -ne 2 -or $versions[0].changeType -ne "MANUAL_EDIT") {
        Write-Error "Latest version in history should be v2 MANUAL_EDIT, got v$($versions[0].version) $($versions[0].changeType)"
        exit 1
    }
    if ($versions[1].version -ne 1 -or $versions[1].changeType -ne "GENERATE") {
        Write-Error "Original version in history should be v1 GENERATE, got v$($versions[1].version) $($versions[1].changeType)"
        exit 1
    }
    Write-Host "  -> Version history verified: [v2 MANUAL_EDIT, v1 GENERATE]."

    # Test Historical Version Snapshot Retrieval (GET /api/documents/{id}/versions/1)
    Write-Host "[10/13] Testing historical snapshot retrieval (GET /api/documents/$targetDocId/versions/1)..."
    $v1Snapshot = Invoke-RestMethod -Uri "$baseUrl/api/documents/$targetDocId/versions/1" -Method Get -Headers $alphaHeaders
    if ($v1Snapshot.version -ne 1 -or $v1Snapshot.content -ne $initialContent) {
        Write-Error "Historical version 1 content mismatch!"
        exit 1
    }

    # Test 404 on non-existent version
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/documents/$targetDocId/versions/999" -Method Get -Headers $alphaHeaders -ErrorAction Stop
        Write-Error "FAIL: GET non-existent version 999 succeeded!"
        exit 1
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 404) {
            Write-Error "Expected 404 for non-existent version, got $($_.Exception.Response.StatusCode.value__)"
            exit 1
        }
    }
    Write-Host "  -> Historical snapshot retrieval verified (v1 content preserved perfectly, 404 on missing version)."

    # Test Restore Version (POST /api/documents/{id}/restore/1)
    Write-Host "[11/13] Testing restore operation (POST /api/documents/$targetDocId/restore/1)..."
    $restoredDoc = Invoke-RestMethod -Uri "$baseUrl/api/documents/$targetDocId/restore/1" -Method Post -Headers $alphaHeaders
    if ($restoredDoc.version -ne 3) {
        Write-Error "Expected version 3 after restore, got $($restoredDoc.version)"
        exit 1
    }
    if ($restoredDoc.content -ne $initialContent) {
        Write-Error "Restored content does not match version 1 content!"
        exit 1
    }

    $versionsAfterRestore = Invoke-RestMethod -Uri "$baseUrl/api/documents/$targetDocId/versions" -Method Get -Headers $alphaHeaders
    if ($versionsAfterRestore.Count -ne 3 -or $versionsAfterRestore[0].changeType -ne "RESTORE") {
        Write-Error "Expected 3 versions with latest changeType RESTORE, got $($versionsAfterRestore[0].changeType)"
        exit 1
    }
    Write-Host "  -> Restore verified: Created v3 RESTORE snapshot without destroying history (v3 -> v2 -> v1)."

    # Test Individual Document Regeneration (POST /api/documents/{id}/regenerate)
    Write-Host "[12/13] Testing individual document regeneration (POST /api/documents/$targetDocId/regenerate)..."
    $regenDoc = Invoke-RestMethod -Uri "$baseUrl/api/documents/$targetDocId/regenerate" -Method Post -Headers $alphaHeaders
    if ($regenDoc.version -ne 4) {
        Write-Error "Expected version 4 after regeneration (strictly monotonic), got $($regenDoc.version)"
        exit 1
    }
    Write-Host "  -> Regeneration verified: Canonical generator executed, produced strictly monotonic version 4."

    # Test Specification Immutability
    Write-Host "[13/13] Verifying approved specification immutability & Anti-IDOR protections..."
    $currentSpec = Invoke-RestMethod -Uri "$baseUrl/api/projects/$projId/specification" -Method Get -Headers $alphaHeaders
    $currentSpecString = $currentSpec | ConvertTo-Json -Depth 10 -Compress

    if ($originalSpecString -ne $currentSpecString) {
        Write-Error "CRITICAL INVARIANT VIOLATION: Project Specification JSON was modified by document editing!"
        exit 1
    }
    Write-Host "  -> IMMUTABILITY INVARIANT PASSED: Specification JSON is 100% bit-for-bit identical to pre-edit state."

    # Anti-IDOR: User Beta tries all Phase 7 endpoints on Alpha's project/document
    Write-Host "  -> Testing Anti-IDOR across all Phase 7 endpoints with User Beta..."
    $idorEndpoints = @(
        @{ Method = "Get"; Uri = "$baseUrl/api/projects/$projId/workspace" },
        @{ Method = "Put"; Uri = "$baseUrl/api/documents/$targetDocId"; Body = @{ content = "# Hacked`nMalicious content from Beta." } | ConvertTo-Json },
        @{ Method = "Get"; Uri = "$baseUrl/api/documents/$targetDocId/versions" },
        @{ Method = "Get"; Uri = "$baseUrl/api/documents/$targetDocId/versions/1" },
        @{ Method = "Post"; Uri = "$baseUrl/api/documents/$targetDocId/restore/1" },
        @{ Method = "Post"; Uri = "$baseUrl/api/documents/$targetDocId/regenerate" },
        @{ Method = "Post"; Uri = "$baseUrl/api/documents/$targetDocId/ai-edit"; Body = @{ action = "IMPROVE" } | ConvertTo-Json }
    )

    foreach ($ep in $idorEndpoints) {
        try {
            $params = @{
                Uri = $ep.Uri
                Method = $ep.Method
                Headers = $betaHeaders
                ErrorAction = "Stop"
            }
            if ($ep.Body) {
                $params.ContentType = "application/json"
                $params.Body = $ep.Body
            }
            Invoke-RestMethod @params
            Write-Error "FAIL: IDOR vulnerability! Beta was able to access $($ep.Method) $($ep.Uri)"
            exit 1
        } catch {
            if ($_.Exception.Response.StatusCode.value__ -ne 404) {
                Write-Error "Expected 404 for IDOR check on $($ep.Uri), got $($_.Exception.Response.StatusCode.value__)"
                exit 1
            }
        }
    }
    Write-Host "  -> ANTI-IDOR VERIFIED: All 7 endpoints returned 404 Not Found (zero cross-tenant data leakage)."

    Write-Host ""
    Write-Host "=========================================================="
    Write-Host "   ALL PHASE 7 LIVE VERIFICATION CHECKS PASSED (13/13)!   "
    Write-Host "=========================================================="

} finally {
    if ($proc -and -not $proc.HasExited) {
        Write-Host "Stopping backend process (PID: $($proc.Id))..."
        Stop-Process -Id $proc.Id -Force
    }
}
