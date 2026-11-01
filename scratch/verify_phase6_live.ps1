$port = 8091
$baseUrl = "http://localhost:$port"
$logFile = "scratch/backend_live_phase6.log"
$errLogFile = "scratch/backend_live_phase6_err.log"

Write-Host "=========================================================="
Write-Host "   ProjectForge AI - Phase 6 Live End-to-End Verification  "
Write-Host "=========================================================="

Write-Host "[1/12] Launching Spring Boot Backend on port $port..."

if (Test-Path $logFile) { Remove-Item $logFile -Force }
if (Test-Path $errLogFile) { Remove-Item $errLogFile -Force }

$javaBin = if ($env:JAVA_HOME) { "$env:JAVA_HOME\bin\java.exe" } else { "java" }

$proc = Start-Process -FilePath $javaBin -ArgumentList @(
    "-jar", "backend/target/projectforge-backend-1.0.0-SNAPSHOT.jar",
    "--server.port=$port",
    "--app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "--app.jwt.expiration-ms=86400000",
    "--spring.datasource.url=jdbc:h2:mem:livephase6db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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

    Write-Host "[2/12] Health check UP: $($health.service) (200 OK)"

    # Test Unauthenticated access to Phase 6 endpoints
    Write-Host "[3/12] Verifying 401 Unauthorized on unauthenticated Phase 6 endpoints..."
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects/1/generate" -Method Post -ErrorAction Stop
        Write-Error "FAIL: Unauthenticated POST /api/projects/1/generate succeeded!"
        exit 1
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 401) {
            Write-Error "Expected 401, got $($_.Exception.Response.StatusCode.value__)"
            exit 1
        }
    }

    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects/1/documents" -Method Get -ErrorAction Stop
        Write-Error "FAIL: Unauthenticated GET /api/projects/1/documents succeeded!"
        exit 1
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 401) {
            Write-Error "Expected 401, got $($_.Exception.Response.StatusCode.value__)"
            exit 1
        }
    }

    try {
        Invoke-RestMethod -Uri "$baseUrl/api/documents/1" -Method Get -ErrorAction Stop
        Write-Error "FAIL: Unauthenticated GET /api/documents/1 succeeded!"
        exit 1
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 401) {
            Write-Error "Expected 401, got $($_.Exception.Response.StatusCode.value__)"
            exit 1
        }
    }
    Write-Host "  -> All 3 unauthenticated endpoints returned 401 Unauthorized."

    # Register & Login Alpha & Beta
    Write-Host "[4/12] Registering and logging in User Alpha and User Beta..."
    $alphaReg = @{ email = "alpha@phase6.test"; password = "Password123!"; name = "User Alpha" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $alphaReg -ContentType "application/json"
    $alphaLogin = @{ email = "alpha@phase6.test"; password = "Password123!" } | ConvertTo-Json
    $alphaAuth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $alphaLogin -ContentType "application/json"
    $alphaToken = $alphaAuth.token
    $alphaHeaders = @{ Authorization = "Bearer $alphaToken" }

    $betaReg = @{ email = "beta@phase6.test"; password = "Password123!"; name = "User Beta" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $betaReg -ContentType "application/json"
    $betaLogin = @{ email = "beta@phase6.test"; password = "Password123!" } | ConvertTo-Json
    $betaAuth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $betaLogin -ContentType "application/json"
    $betaToken = $betaAuth.token
    $betaHeaders = @{ Authorization = "Bearer $betaToken" }
    Write-Host "  -> Registered & logged in; received JWTs for Alpha and Beta."

    # Alpha creates project
    Write-Host "[5/12] Alpha creating project & setting approved specification..."
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
    $projectId = $project.id
    Write-Host "  -> Project created with ID: $projectId, Status: $($project.status)"

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

    $reviewRes = Invoke-RestMethod -Uri "$baseUrl/api/projects/$projectId/review" -Method Put -Headers $alphaHeaders -Body $validSpec -ContentType "application/json"
    Write-Host "  -> Review saved: Status $($reviewRes.status), Quality Score: $($reviewRes.qualityScore), Can Approve: $($reviewRes.canApprove)"

    # Verify document generation fails if NOT approved (status: REQUIREMENTS_READY)
    Write-Host "[6/12] Verifying generation rejection on unapproved project (Status: $($reviewRes.status))..."
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects/$projectId/generate" -Method Post -Headers $alphaHeaders -ErrorAction Stop
        Write-Error "FAIL: Document generation was permitted on non-APPROVED project!"
        exit 1
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 400) {
            Write-Error "Expected 400 Bad Request, got $($_.Exception.Response.StatusCode.value__)"
            exit 1
        }
        Write-Host "  -> Correctly rejected with 400 Bad Request (InvalidProjectStateException)."
    }

    # User Alpha explicitly approves project
    Write-Host "[7/12] Alpha explicitly approving project..."
    $approveBody = @{ confirmation = $true } | ConvertTo-Json
    $approvalRes = Invoke-RestMethod -Uri "$baseUrl/api/projects/$projectId/approve" -Method Post -Headers $alphaHeaders -Body $approveBody -ContentType "application/json"
    if ($approvalRes.status -ne "APPROVED" -or -not $approvalRes.approved) {
        Write-Error "FAIL: Project approval failed! Status: $($approvalRes.status)"
        exit 1
    }
    Write-Host "  -> Project successfully APPROVED and locked."

    # Verify Anti-IDOR: Beta cannot trigger generation on Alpha's project
    Write-Host "[8/12] Verifying Anti-IDOR: Beta generating Alpha's project..."
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects/$projectId/generate" -Method Post -Headers $betaHeaders -ErrorAction Stop
        Write-Error "FAIL: Beta was able to generate Alpha's documents!"
        exit 1
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 404) {
            Write-Error "Expected 404 Not Found, got $($_.Exception.Response.StatusCode.value__)"
            exit 1
        }
        Write-Host "  -> Correctly rejected with 404 Not Found (Zero metadata leak)."
    }

    # User Alpha generates documents
    Write-Host "[9/12] Alpha generating documentation package strictly from approved spec..."
    $genRes = Invoke-RestMethod -Uri "$baseUrl/api/projects/$projectId/generate" -Method Post -Headers $alphaHeaders
    Write-Host "  -> Generation result: Status $($genRes.status), Documents Count: $($genRes.documentsCount)"

    if ($genRes.status -ne "GENERATED") {
        Write-Error "FAIL: Expected status GENERATED, got $($genRes.status)"
        exit 1
    }
    if ($genRes.documentsCount -lt 10) {
        Write-Error "FAIL: Expected at least 10 documents, got $($genRes.documentsCount)"
        exit 1
    }

    # Verify Approved Spec Immutability: Post-approval changes rejected
    Write-Host "[10/12] Verifying approved specification immutability..."
    try {
        $editSpecBody = @{ specification = @{ projectName = "Hacked Name" } } | ConvertTo-Json
        Invoke-RestMethod -Uri "$baseUrl/api/projects/$projectId/review" -Method Put -Headers $alphaHeaders -Body $editSpecBody -ContentType "application/json" -ErrorAction Stop
        Write-Error "FAIL: PUT /review succeeded on approved project!"
        exit 1
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 400) {
            Write-Error "Expected 400 Bad Request, got $($_.Exception.Response.StatusCode.value__)"
            exit 1
        }
    }
    Write-Host "  -> Specification remains strictly immutable (PUT /review rejected with 400)."

    # List project documents and check single document retrieval
    Write-Host "[11/12] Verifying document manifest and single document retrieval..."
    $docsList = Invoke-RestMethod -Uri "$baseUrl/api/projects/$projectId/documents" -Method Get -Headers $alphaHeaders
    Write-Host "  -> Retrieved $($docsList.Count) documents in manifest."

    $filenames = $docsList | ForEach-Object { $_.filename }
    Write-Host "  -> Generated files: $($filenames -join ', ')"

    # Check that standard documents are present
    $requiredFiles = @("README.md", "PROJECT_REQUIREMENTS.md", "TECH_STACK.md", "ARCHITECTURE.md", "DATABASE.md", "API_SPECIFICATION.md", "SECURITY.md", "TESTING.md", "DEPLOYMENT.md")
    foreach ($rf in $requiredFiles) {
        if (-not ($filenames -contains $rf)) {
            Write-Error "FAIL: Expected file $rf missing from generated manifest!"
            exit 1
        }
    }

    # Fetch README.md
    $readmeMeta = $docsList | Where-Object { $_.filename -eq "README.md" }
    $readmeDoc = Invoke-RestMethod -Uri "$baseUrl/api/documents/$($readmeMeta.id)" -Method Get -Headers $alphaHeaders
    Write-Host "  -> Successfully fetched README.md ($($readmeDoc.characterCount) chars, version $($readmeDoc.version))"

    if (-not $readmeDoc.content.Contains("CloudMatrix Microservices Platform")) {
        Write-Error "FAIL: README.md does not contain approved project name!"
        exit 1
    }

    # Anti-IDOR check on documents for Beta
    Write-Host "  -> Verifying Beta cannot list Alpha's documents (404)..."
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects/$projectId/documents" -Method Get -Headers $betaHeaders -ErrorAction Stop
        Write-Error "FAIL: Beta could list Alpha's documents!"
        exit 1
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 404) {
            Write-Error "Expected 404, got $($_.Exception.Response.StatusCode.value__)"
            exit 1
        }
    }

    Write-Host "  -> Verifying Beta cannot get Alpha's single document (404)..."
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/documents/$($readmeMeta.id)" -Method Get -Headers $betaHeaders -ErrorAction Stop
        Write-Error "FAIL: Beta could access Alpha's document content!"
        exit 1
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 404) {
            Write-Error "Expected 404, got $($_.Exception.Response.StatusCode.value__)"
            exit 1
        }
    }

    # Re-generation check: Re-generating from GENERATED replaces cleanly without duplicates
    Write-Host "[12/12] Verifying document re-generation from GENERATED status..."
    $regenRes = Invoke-RestMethod -Uri "$baseUrl/api/projects/$projectId/generate" -Method Post -Headers $alphaHeaders
    Write-Host "  -> Re-generation succeeded: Status $($regenRes.status), Documents Count: $($regenRes.documentsCount)"

    $reloadedList = Invoke-RestMethod -Uri "$baseUrl/api/projects/$projectId/documents" -Method Get -Headers $alphaHeaders
    if ($reloadedList.Count -ne $docsList.Count) {
        Write-Error "FAIL: Re-generation created duplicate or mismatched documents! Expected $($docsList.Count), got $($reloadedList.Count)"
        exit 1
    }
    Write-Host "  -> Document count after re-generation is clean and exact ($($reloadedList.Count) documents, 0 duplicates)."

    Write-Host ""
    Write-Host "=========================================================="
    Write-Host "   PHASE 6 LIVE VERIFICATION COMPLETE: ALL CHECKS PASSED  "
    Write-Host "=========================================================="

} finally {
    Write-Host "Stopping background test server (PID: $($proc.Id))..."
    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
}
