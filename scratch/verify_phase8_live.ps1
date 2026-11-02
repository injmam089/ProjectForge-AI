$port = 8093
$baseUrl = "http://localhost:$port"
$logFile = "scratch/backend_live_phase8.log"
$errLogFile = "scratch/backend_live_phase8_err.log"
$zipOutputFile = "scratch/test_export_alpha.zip"
$zipAliasFile = "scratch/test_export_alias.zip"
$extractDir = "scratch/extracted_alpha"

Write-Host "=========================================================="
Write-Host "   ProjectForge AI - Phase 8 Live End-to-End Verification  "
Write-Host "=========================================================="

Write-Host "[1/14] Launching Spring Boot Backend on port $port..."

if (Test-Path $logFile) { Remove-Item $logFile -Force }
if (Test-Path $errLogFile) { Remove-Item $errLogFile -Force }
if (Test-Path $zipOutputFile) { Remove-Item $zipOutputFile -Force }
if (Test-Path $zipAliasFile) { Remove-Item $zipAliasFile -Force }
if (Test-Path $extractDir) { Remove-Item $extractDir -Recurse -Force }

$javaBin = if ($env:JAVA_HOME) { "$env:JAVA_HOME\bin\java.exe" } else { "java" }

$proc = Start-Process -FilePath $javaBin -ArgumentList @(
    "-jar", "backend/target/projectforge-backend-1.0.0-SNAPSHOT.jar",
    "--server.port=$port",
    "--app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "--app.jwt.expiration-ms=86400000",
    "--spring.datasource.url=jdbc:h2:mem:livephase8db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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

    Write-Host "[2/14] Health check UP: $($health.service) (200 OK)"

    # Test Unauthenticated access to Phase 8 export endpoints
    Write-Host "[3/14] Verifying 401 Unauthorized on unauthenticated Phase 8 export endpoints..."
    $endpointsToTest401 = @(
        @{ Method = "Post"; Uri = "$baseUrl/api/projects/1/export" },
        @{ Method = "Get";  Uri = "$baseUrl/api/projects/1/export/download" },
        @{ Method = "Get";  Uri = "$baseUrl/api/projects/1/export" }
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
    Write-Host "  -> All 3 unauthenticated Phase 8 endpoints correctly rejected with 401 Unauthorized."

    # Register & Login User Alpha and User Beta
    Write-Host "[4/14] Registering and authenticating User Alpha and User Beta..."
    $alphaReg = @{ email = "alpha@phase8.test"; password = "Password123!"; name = "User Alpha" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $alphaReg -ContentType "application/json" | Out-Null
    $alphaLogin = @{ email = "alpha@phase8.test"; password = "Password123!" } | ConvertTo-Json
    $alphaAuth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $alphaLogin -ContentType "application/json"
    $alphaToken = $alphaAuth.token
    $alphaHeaders = @{ Authorization = "Bearer $alphaToken" }

    $betaReg = @{ email = "beta@phase8.test"; password = "Password123!"; name = "User Beta" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $betaReg -ContentType "application/json" | Out-Null
    $betaLogin = @{ email = "beta@phase8.test"; password = "Password123!" } | ConvertTo-Json
    $betaAuth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $betaLogin -ContentType "application/json"
    $betaToken = $betaAuth.token
    $betaHeaders = @{ Authorization = "Bearer $betaToken" }

    Write-Host "  -> Users authenticated successfully."

    # Create Alpha's project in DRAFT status (0 documents)
    Write-Host "[5/14] Testing empty project rejection (0 documents)..."
    $projBody = @{
        name = "CloudPulse Telemetry"
        description = "Distributed observability and metrics collector"
        problemStatement = "High cardinality metrics overwhelm traditional time-series backends."
        targetUsers = "Site reliability engineers and platform teams"
        projectType = "API_BACKEND"
        difficulty = "ADVANCED"
        preferredTechStack = "Java 21, Spring Boot, PostgreSQL, Kafka"
        specialRequirements = "Sub-second ingest latency and strict tenant isolation"
    } | ConvertTo-Json

    $alphaProject = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Post -Body $projBody -ContentType "application/json" -Headers $alphaHeaders
    $alphaProjId = $alphaProject.id
    Write-Host "  -> Created project '$($alphaProject.name)' with ID: $alphaProjId (status: $($alphaProject.status))"

    # Attempt export on empty project -> should fail with 400 Bad Request
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects/$alphaProjId/export" -Method Post -Headers $alphaHeaders -ErrorAction Stop
        Write-Error "FAIL: Export preparation on project with 0 documents should have returned 400!"
        exit 1
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -ne 400) {
            Write-Error "Expected 400 Bad Request on empty project, got $code"
            exit 1
        }
        Write-Host "  -> POST /api/projects/$alphaProjId/export correctly rejected with 400 Bad Request (0 documents)."
    }

    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects/$alphaProjId/export/download" -Method Get -Headers $alphaHeaders -ErrorAction Stop
        Write-Error "FAIL: Export download on project with 0 documents should have returned 400!"
        exit 1
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -ne 400) {
            Write-Error "Expected 400 Bad Request on empty project download, got $code"
            exit 1
        }
        Write-Host "  -> GET /api/projects/$alphaProjId/export/download correctly rejected with 400 Bad Request (0 documents)."
    }

    # Prepare project for analysis, inject approved spec, and generate documents
    Write-Host "[6/14] Approving specification and generating Phase 6 documentation set..."
    Invoke-RestMethod -Uri "$baseUrl/api/projects/$alphaProjId/prepare-analysis" -Method Post -Headers $alphaHeaders | Out-Null

    $validSpec = @{
        specification = @{
            schemaVersion = "1.0.0"
            project = @{
                name = "CloudPulse Telemetry"
                goal = "Enterprise distributed system with real-time analytics and event streaming"
                problem = "DevOps engineers require unified observability and multi-tenant stream processing"
                type = "API_BACKEND"
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
            qualityScore = 95
        }
    } | ConvertTo-Json -Depth 10

    Invoke-RestMethod -Uri "$baseUrl/api/projects/$alphaProjId/review" -Method Put -Headers $alphaHeaders -Body $validSpec -ContentType "application/json" | Out-Null

    # User Alpha explicitly approves project
    $approveBody = @{ confirmation = $true } | ConvertTo-Json
    $approved = Invoke-RestMethod -Uri "$baseUrl/api/projects/$alphaProjId/approve" -Method Post -Headers $alphaHeaders -Body $approveBody -ContentType "application/json"
    if ($approved.status -ne "APPROVED") {
        Write-Error "Project status expected APPROVED, got $($approved.status)"
        exit 1
    }

    # Generate Phase 6 documents
    $genDocs = Invoke-RestMethod -Uri "$baseUrl/api/projects/$alphaProjId/generate" -Method Post -Headers $alphaHeaders
    Write-Host "  -> Phase 6 generation complete. Generated $($genDocs.documentsCount) documents with status: $($genDocs.status)"
    if ($genDocs.documentsCount -ne 14) {
        Write-Error "Expected 14 documents, got $($genDocs.documentsCount)"
        exit 1
    }

    # Snapshot specification for immutability check
    $originalSpec = Invoke-RestMethod -Uri "$baseUrl/api/projects/$alphaProjId/specification" -Method Get -Headers $alphaHeaders
    $originalSpecString = $originalSpec | ConvertTo-Json -Depth 10 -Compress

    # Retrieve all documents to locate README.md
    Write-Host "[7/14] Modifying README.md to v2 to verify active document content is exported..."
    $workspace = Invoke-RestMethod -Uri "$baseUrl/api/projects/$alphaProjId/workspace" -Method Get -Headers $alphaHeaders
    $readmeDoc = $workspace.documents | Where-Object { $_.filename -eq "README.md" }
    if (-not $readmeDoc) {
        Write-Error "README.md not found in workspace documents!"
        exit 1
    }
    $readmeId = $readmeDoc.id

    $v2Text = "# CloudPulse Telemetry - Active Edition v2`n`nVerified manual edits to test that export captures active content."
    $saveBody = @{ content = $v2Text } | ConvertTo-Json
    $saveRes = Invoke-RestMethod -Uri "$baseUrl/api/documents/$readmeId" -Method Put -Body $saveBody -ContentType "application/json" -Headers $alphaHeaders
    Write-Host "  -> Saved README.md as version $($saveRes.version). Character count: $($saveRes.characterCount)"

    # Test Phase 8 Export Preparation: POST /api/projects/{id}/export
    Write-Host "[8/14] Calling POST /api/projects/$alphaProjId/export to validate metadata..."
    $exportPrep = Invoke-RestMethod -Uri "$baseUrl/api/projects/$alphaProjId/export" -Method Post -Headers $alphaHeaders
    Write-Host "  -> Export Metadata:"
    Write-Host "     Project ID:     $($exportPrep.projectId)"
    Write-Host "     Project Name:   $($exportPrep.projectName)"
    Write-Host "     Document Count: $($exportPrep.documentCount)"
    Write-Host "     Filename:       $($exportPrep.filename)"
    Write-Host "     Exported At:    $($exportPrep.exportedAt)"

    if ($exportPrep.documentCount -ne 14) {
        Write-Error "Expected documentCount == 14 in export metadata, got $($exportPrep.documentCount)"
        exit 1
    }
    if ($exportPrep.filename -ne "ProjectForge_CloudPulse_Telemetry.zip") {
        Write-Error "Expected filename 'ProjectForge_CloudPulse_Telemetry.zip', got '$($exportPrep.filename)'"
        exit 1
    }

    # Test Phase 8 ZIP Download: GET /api/projects/{id}/export/download
    Write-Host "[9/14] Downloading binary ZIP package via GET /api/projects/$alphaProjId/export/download..."
    $downloadUrl = "$baseUrl/api/projects/$alphaProjId/export/download"
    
    # Use WebClient or Invoke-WebRequest with token header to capture raw binary bytes
    $webClient = New-Object System.Net.WebClient
    $webClient.Headers.Add("Authorization", "Bearer $alphaToken")
    $webClient.DownloadFile($downloadUrl, (Resolve-Path .).Path + "\$zipOutputFile")

    if (-not (Test-Path $zipOutputFile)) {
        Write-Error "Downloaded ZIP file does not exist at $zipOutputFile!"
        exit 1
    }

    $zipSize = (Get-Item $zipOutputFile).Length
    Write-Host "  -> ZIP archive downloaded successfully. Size: $zipSize bytes"
    if ($zipSize -lt 2000) {
        Write-Error "ZIP file size is suspiciously small: $zipSize bytes"
        exit 1
    }

    # Test Unzip and Validate Structure & Contents
    Write-Host "[10/14] Unzipping package and validating directory structure and manifest..."
    Expand-Archive -Path $zipOutputFile -DestinationPath $extractDir -Force

    $baseDir = "$extractDir\ProjectForge_CloudPulse_Telemetry"
    if (-not (Test-Path $baseDir)) {
        Write-Error "Root folder ProjectForge_CloudPulse_Telemetry not found in extracted archive!"
        exit 1
    }
    Write-Host "  -> Root folder found: ProjectForge_CloudPulse_Telemetry"

    # Count Markdown files
    $mdFiles = Get-ChildItem -Path $baseDir -Filter "*.md"
    Write-Host "  -> Found $($mdFiles.Count) Markdown documents in root folder:"
    foreach ($f in $mdFiles) {
        Write-Host "     - $($f.Name) ($($f.Length) bytes)"
    }
    if ($mdFiles.Count -ne 14) {
        Write-Error "Expected 14 markdown files, found $($mdFiles.Count)"
        exit 1
    }

    # Verify README.md contains v2 edited text (proving active content was packaged)
    $readmeContent = Get-Content "$baseDir\README.md" -Raw
    if (-not $readmeContent.Contains("Active Edition v2")) {
        Write-Error "README.md does not contain v2 active edited content!"
        exit 1
    }
    Write-Host "  -> VERIFIED: README.md contains v2 active edited content."

    # Verify projectforge/EXPORT_MANIFEST.json
    $manifestPath = "$baseDir\projectforge\EXPORT_MANIFEST.json"
    if (-not (Test-Path $manifestPath)) {
        Write-Error "EXPORT_MANIFEST.json not found at $manifestPath!"
        exit 1
    }
    $manifestJson = Get-Content $manifestPath -Raw | ConvertFrom-Json
    Write-Host "  -> Manifest Details:"
    Write-Host "     Product:        $($manifestJson.product)"
    Write-Host "     Export Version: $($manifestJson.exportVersion)"
    Write-Host "     Project Name:   $($manifestJson.projectName)"
    Write-Host "     Project ID:     $($manifestJson.projectId)"
    Write-Host "     Document Count: $($manifestJson.documentCount)"
    Write-Host "     Documents List: $($manifestJson.documents.Count) entries"

    if ($manifestJson.product -ne "ProjectForge AI") {
        Write-Error "Manifest product is not 'ProjectForge AI'"
        exit 1
    }
    if ($manifestJson.documentCount -ne 14) {
        Write-Error "Manifest documentCount is not 14"
        exit 1
    }
    if ($manifestJson.documents.Count -ne 14) {
        Write-Error "Manifest documents array length is not 14"
        exit 1
    }

    # Check README.md entry in manifest
    $readmeEntry = $manifestJson.documents | Where-Object { $_.filename -eq "README.md" }
    if (-not $readmeEntry) {
        Write-Error "README.md missing from manifest documents list!"
        exit 1
    }
    if ($readmeEntry.version -ne 2) {
        Write-Error "Expected README.md version 2 in manifest, got $($readmeEntry.version)"
        exit 1
    }
    Write-Host "  -> VERIFIED: EXPORT_MANIFEST.json lists README.md as version 2."

    # Verify no secrets or sensitive files exist anywhere in the archive
    $allFiles = Get-ChildItem -Path $extractDir -Recurse -File
    foreach ($file in $allFiles) {
        if ($file.Name -match "\.env|application\.properties|\.key|\.pem|credentials") {
            Write-Error "SECURITY LEAK: Found sensitive file $($file.FullName) in archive!"
            exit 1
        }
    }
    Write-Host "  -> VERIFIED: Zero sensitive files or credential leaks in export archive."

    # Test Direct Alias: GET /api/projects/{id}/export
    Write-Host "[11/14] Testing direct alias endpoint GET /api/projects/$alphaProjId/export..."
    $webClient.DownloadFile("$baseUrl/api/projects/$alphaProjId/export", (Resolve-Path .).Path + "\$zipAliasFile")
    $aliasSize = (Get-Item $zipAliasFile).Length
    if ($aliasSize -lt 5000 -or [Math]::Abs($aliasSize - $zipSize) -gt 100) {
        Write-Error "Alias ZIP size ($aliasSize) is invalid or deviates unexpectedly from original ZIP ($zipSize)"
        exit 1
    }
    Write-Host "  -> Direct alias GET /api/projects/$alphaProjId/export verified ($aliasSize bytes)."

    # Test Immutability Invariant
    Write-Host "[12/14] Verifying read-only immutability invariant on project and specification..."
    $projectAfterExport = Invoke-RestMethod -Uri "$baseUrl/api/projects/$alphaProjId" -Method Get -Headers $alphaHeaders
    if ($projectAfterExport.status -ne "GENERATED") {
        Write-Error "Export mutated project status! Expected GENERATED, got $($projectAfterExport.status)"
        exit 1
    }
    $currentSpec = Invoke-RestMethod -Uri "$baseUrl/api/projects/$alphaProjId/specification" -Method Get -Headers $alphaHeaders
    $currentSpecString = $currentSpec | ConvertTo-Json -Depth 10 -Compress
    if ($originalSpecString -ne $currentSpecString) {
        Write-Error "Export mutated project specification JSON!"
        exit 1
    }

    $readmeAfterExport = Invoke-RestMethod -Uri "$baseUrl/api/documents/$readmeId" -Method Get -Headers $alphaHeaders
    if ($readmeAfterExport.version -ne 2) {
        Write-Error "Export mutated document version! Expected 2, got $($readmeAfterExport.version)"
        exit 1
    }
    if ($readmeAfterExport.content -ne $v2Text) {
        Write-Error "Export mutated document content!"
        exit 1
    }
    Write-Host "  -> VERIFIED: Project status, specification JSON, and document contents are 100% untouched."

    # Test Anti-IDOR: User Beta cross-tenant access to Alpha's export endpoints
    Write-Host "[13/14] Testing anti-IDOR defense (User Beta attempting to export Alpha's project)..."
    
    # POST /api/projects/{alphaProjId}/export with Beta's token
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects/$alphaProjId/export" -Method Post -Headers $betaHeaders -ErrorAction Stop
        Write-Error "FAIL: User Beta was able to prepare export for User Alpha's project!"
        exit 1
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -ne 404) {
            Write-Error "Expected 404 Not Found on cross-tenant POST /export, got $code"
            exit 1
        }
        Write-Host "  -> Beta POST /api/projects/$alphaProjId/export correctly rejected with 404 Not Found."
    }

    # GET /api/projects/{alphaProjId}/export/download with Beta's token
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects/$alphaProjId/export/download" -Method Get -Headers $betaHeaders -ErrorAction Stop
        Write-Error "FAIL: User Beta was able to download export for User Alpha's project!"
        exit 1
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -ne 404) {
            Write-Error "Expected 404 Not Found on cross-tenant GET /export/download, got $code"
            exit 1
        }
        Write-Host "  -> Beta GET /api/projects/$alphaProjId/export/download correctly rejected with 404 Not Found."
    }

    # GET /api/projects/{alphaProjId}/export with Beta's token
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects/$alphaProjId/export" -Method Get -Headers $betaHeaders -ErrorAction Stop
        Write-Error "FAIL: User Beta was able to access alias export for User Alpha's project!"
        exit 1
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -ne 404) {
            Write-Error "Expected 404 Not Found on cross-tenant alias GET /export, got $code"
            exit 1
        }
        Write-Host "  -> Beta GET /api/projects/$alphaProjId/export correctly rejected with 404 Not Found."
    }

    # Non-existent project ID (99999)
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects/99999/export" -Method Post -Headers $alphaHeaders -ErrorAction Stop
        Write-Error "FAIL: Non-existent project ID should return 404!"
        exit 1
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -ne 404) {
            Write-Error "Expected 404 Not Found for non-existent project, got $code"
            exit 1
        }
        Write-Host "  -> Alpha POST /api/projects/99999/export correctly returned 404 Not Found."
    }

    Write-Host "[14/14] All Phase 8 verification assertions PASSED with flying colors!"
    Write-Host "=========================================================="
    Write-Host "   PHASE 8 END-TO-END LIVE VERIFICATION: SUCCESS          "
    Write-Host "=========================================================="

} finally {
    Write-Host "Cleaning up live backend process (PID: $($proc.Id))..."
    if ($proc -and -not $proc.HasExited) {
        Stop-Process -Id $proc.Id -Force
    }
}
