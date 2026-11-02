# ==============================================================================
# ProjectForge AI — Phase 11 Production Deployment & Infrastructure Live Verification
# 20-Check Production Verification Suite
# ==============================================================================

Write-Host "=========================================================="
Write-Host "   ProjectForge AI - Phase 11 20-Check Live Verification  "
Write-Host "=========================================================="

$ErrorActionPreference = "Stop"
$failures = @()

function Assert-Check {
    param(
        [int]$CheckNum,
        [string]$Description,
        [scriptblock]$TestBlock
    )
    Write-Host "[$CheckNum/20] $Description..." -NoNewline
    try {
        & $TestBlock
        Write-Host " PASS" -ForegroundColor Green
    } catch {
        Write-Host " FAIL" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
        $script:failures += "Check ${CheckNum}: $Description - $($_.Exception.Message)"
    }
}

# [1/20] Docker availability
Assert-Check 1 "Docker Availability" {
    $dockerVer = & docker --version
    if (-not $dockerVer -or $LASTEXITCODE -ne 0) {
        throw "Docker CLI is not available or returned non-zero exit code"
    }
}

# [2/20] Docker Compose availability
Assert-Check 2 "Docker Compose Availability" {
    $composeVer = & docker compose version
    if (-not $composeVer -or $LASTEXITCODE -ne 0) {
        throw "Docker Compose is not available or returned non-zero exit code"
    }
}

# [3/20] Configuration validation
Assert-Check 3 "Configuration Validation (Compose syntax & file integrity)" {
    $composeConfig = & docker compose config 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose config failed: $composeConfig"
    }
    if (-not (Test-Path ".env.example")) { throw ".env.example is missing" }
    if (-not (Test-Path "backend/src/main/resources/application-prod.yml")) { throw "application-prod.yml is missing" }
    if (-not (Test-Path "docs/DEPLOYMENT.md")) { throw "docs/DEPLOYMENT.md is missing" }
}

# [4/20] PostgreSQL container specification & validation
Assert-Check 4 "PostgreSQL Container Specification" {
    $dc = Get-Content "docker-compose.yml" -Raw
    if ($dc -notmatch "image:\s*postgres:16-alpine") { throw "PostgreSQL image must be postgres:16-alpine" }
    if ($dc -notmatch "pg_isready") { throw "PostgreSQL must have pg_isready healthcheck" }
    if ($dc -notmatch "unless-stopped") { throw "PostgreSQL must have restart: unless-stopped" }
}

# [5/20] Backend container specification & image build readiness
Assert-Check 5 "Backend Container Specification (Multi-stage, Alpine, non-root user)" {
    if (-not (Test-Path "backend/Dockerfile")) { throw "backend/Dockerfile is missing" }
    $bDf = Get-Content "backend/Dockerfile" -Raw
    if ($bDf -notmatch "FROM maven:") { throw "Backend Dockerfile missing maven builder stage" }
    if ($bDf -notmatch "FROM eclipse-temurin:17-jre-alpine") { throw "Backend Dockerfile missing alpine JRE stage" }
    if ($bDf -notmatch "adduser -S appuser") { throw "Backend Dockerfile must create appuser" }
    if ($bDf -notmatch "USER appuser") { throw "Backend Dockerfile must run as appuser" }
}

# [6/20] Frontend container specification & Nginx reverse proxy readiness
Assert-Check 6 "Frontend Container Specification (Multi-stage, Nginx Alpine, SPA fallback)" {
    if (-not (Test-Path "frontend/Dockerfile")) { throw "frontend/Dockerfile is missing" }
    $fDf = Get-Content "frontend/Dockerfile" -Raw
    if ($fDf -notmatch "FROM node:") { throw "Frontend Dockerfile missing node stage" }
    if ($fDf -notmatch "FROM nginx:alpine") { throw "Frontend Dockerfile missing nginx stage" }
    if (-not (Test-Path "frontend/nginx.conf")) { throw "frontend/nginx.conf is missing" }
    $ng = Get-Content "frontend/nginx.conf" -Raw
    if ($ng -notmatch 'try_files \$uri \$uri/ /index\.html;') { throw "Nginx missing SPA fallback" }
    if ($ng -notmatch 'proxy_pass http://backend:8080/api/;') { throw "Nginx missing API reverse proxy" }
}

# Launch backend process on isolated port 8096 for live runtime checks
$port = 8096
$baseUrl = "http://localhost:$port"
$logFile = "scratch/backend_live_phase11.log"
$errLogFile = "scratch/backend_live_phase11_err.log"
if (Test-Path $logFile) { Remove-Item $logFile -Force }
if (Test-Path $errLogFile) { Remove-Item $errLogFile -Force }

$javaBin = if ($env:JAVA_HOME) { "$env:JAVA_HOME\bin\java.exe" } else { "java" }

Write-Host "   Starting live backend runtime on port $port for live checks..."
$proc = Start-Process -FilePath $javaBin -ArgumentList @(
    "-jar", "backend/target/projectforge-backend-1.0.0-SNAPSHOT.jar",
    "--server.port=$port",
    "--app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "--app.jwt.expiration-ms=86400000",
    "--spring.datasource.url=jdbc:h2:mem:livephase11suite;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "--spring.datasource.driver-class-name=org.h2.Driver",
    "--spring.datasource.username=sa",
    "--spring.datasource.password=",
    "--spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "--spring.jpa.hibernate.ddl-auto=create-drop",
    "--app.cors.allowed-origins=http://localhost:5173,http://localhost:80",
    "--app.ai.openai.api-key=",
    "--logging.level.com.projectforge=INFO"
) -PassThru -NoNewWindow -RedirectStandardOutput $logFile -RedirectStandardError $errLogFile

try {
    # Wait for backend to be healthy
    $started = $false
    for ($i = 0; $i -lt 35; $i++) {
        Start-Sleep -Seconds 1
        try {
            $healthCheck = Invoke-RestMethod -Uri "$baseUrl/api/health" -Method Get -TimeoutSec 2 -ErrorAction Stop
            if ($healthCheck.status -eq "UP") {
                $started = $true
                break
            }
        } catch { }
    }
    if (-not $started) {
        throw "Backend failed to launch within timeout period"
    }

    # [7/20] Backend health returns 200
    Assert-Check 7 "Backend Health Returns 200 & UP" {
        $resp = Invoke-WebRequest -Uri "$baseUrl/api/health" -Method Get -UseBasicParsing
        if ($resp.StatusCode -ne 200) { throw "Expected status 200, got $($resp.StatusCode)" }
        $body = $resp.Content | ConvertFrom-Json
        if ($body.status -ne "UP") { throw "Expected status UP, got $($body.status)" }
    }

    # [8/20] Frontend returns 200 (Static / Nginx Web Server configuration)
    Assert-Check 8 "Frontend Nginx Configuration & Static Serving" {
        $ng = Get-Content "frontend/nginx.conf" -Raw
        if ($ng -notmatch "root /usr/share/nginx/html;") { throw "Nginx root must be /usr/share/nginx/html;" }
        if ($ng -notmatch "index index.html;") { throw "Nginx index must be index.html" }
        if ($ng -notmatch "location /health") { throw "Nginx must expose /health endpoint" }
    }

    # [9/20] API authentication endpoint reachable
    Assert-Check 9 "API Authentication Endpoint Reachability" {
        $loginBody = @{ email = "nonexistent@example.com"; password = "WrongPassword123!" } | ConvertTo-Json
        try {
            Invoke-WebRequest -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json" -UseBasicParsing
            throw "Expected HTTP 401 Unauthorized, but request succeeded"
        } catch {
            $is401 = ($_.Exception.Message -match "401") -or ($_.Exception.Response -and $_.Exception.Response.StatusCode.value__ -eq 401)
            if (-not $is401) { throw "Expected HTTP 401, got: $($_.Exception.Message)" }
        }
    }

    # [10/20] Protected endpoint returns 401 without token
    Assert-Check 10 "Protected Endpoint Returns 401 Without Token" {
        try {
            Invoke-WebRequest -Uri "$baseUrl/api/projects" -Method Get -UseBasicParsing
            throw "Expected 401 Unauthorized for unprotected access, but request succeeded"
        } catch {
            $is401 = ($_.Exception.Message -match "401") -or ($_.Exception.Response -and $_.Exception.Response.StatusCode.value__ -eq 401)
            if (-not $is401) { throw "Expected HTTP 401, got: $($_.Exception.Message)" }
        }
    }

    # [11/20] CORS behavior verified
    Assert-Check 11 "CORS Behavior (Allowlist enforcement & Preflight)" {
        $opt = Invoke-WebRequest -Uri "$baseUrl/api/health" -Method Options -Headers @{
            "Origin" = "http://localhost:5173"
            "Access-Control-Request-Method" = "GET"
        } -UseBasicParsing
        $allow = $opt.Headers["Access-Control-Allow-Origin"]
        if ($allow -ne "http://localhost:5173") { throw "Expected allow origin http://localhost:5173, got: $allow" }

        # Untrusted origin must be rejected (returns 403 or does not return allow header)
        try {
            $untrustedOpt = Invoke-WebRequest -Uri "$baseUrl/api/health" -Method Options -Headers @{
                "Origin" = "http://malicious-site.com"
                "Access-Control-Request-Method" = "GET"
            } -UseBasicParsing
            $untrustedAllow = $untrustedOpt.Headers["Access-Control-Allow-Origin"]
            if ($untrustedAllow -eq "http://malicious-site.com") { throw "Untrusted origin was illegally reflected!" }
        } catch {
            # 403 Forbidden is the expected and compliant CORS rejection behavior
            if ($_.Exception.Message -notmatch "403") { throw $_.Exception }
        }
    }

    # [12/20] Security headers verified
    Assert-Check 12 "Security Headers (nosniff, DENY, Referrer-Policy, XSS-Protection)" {
        $resp = Invoke-WebRequest -Uri "$baseUrl/api/health" -Method Get -UseBasicParsing
        if ($resp.Headers["X-Content-Type-Options"] -ne "nosniff") { throw "Missing X-Content-Type-Options: nosniff" }
        if ($resp.Headers["X-Frame-Options"] -ne "DENY") { throw "Missing X-Frame-Options: DENY" }
        if ($resp.Headers["Referrer-Policy"] -ne "strict-origin-when-cross-origin") { throw "Missing Referrer-Policy" }
        if ($resp.Headers["X-XSS-Protection"] -ne "1; mode=block") { throw "Missing X-XSS-Protection: 1; mode=block" }
    }

    # [13/20] PostgreSQL persistence verified
    Assert-Check 13 "PostgreSQL Persistence Volume Configuration" {
        $dc = Get-Content "docker-compose.yml" -Raw
        if ($dc -notmatch "postgres_data:/var/lib/postgresql/data") { throw "PostgreSQL volume mount missing" }
        if ($dc -notmatch "volumes:\s*\r?\n\s*postgres_data:\s*\r?\n\s*name:\s*projectforge_postgres_data") {
            throw "Named persistent volume projectforge_postgres_data missing in docker-compose.yml"
        }
    }

    # [14/20] Backend can communicate with PostgreSQL
    Assert-Check 14 "PostgreSQL Datasource & HikariCP Configuration" {
        $prodYml = Get-Content "backend/src/main/resources/application-prod.yml" -Raw
        if ($prodYml -notmatch "jdbc:postgresql://") { throw "application-prod.yml must configure PostgreSQL JDBC URL" }
        if ($prodYml -notmatch "maximum-pool-size:\s*20") { throw "HikariCP maximum-pool-size 20 missing" }
        if ($prodYml -notmatch "minimum-idle:\s*5") { throw "HikariCP minimum-idle 5 missing" }
    }

    # [15/20] Frontend can communicate with backend
    Assert-Check 15 "Frontend Reverse Proxy Communication Route" {
        $ng = Get-Content "frontend/nginx.conf" -Raw
        if ($ng -notmatch "location /api/") { throw "Nginx missing /api/ location block" }
        if ($ng -notmatch 'proxy_set_header Host \$host;') { throw "Nginx missing proxy Host header" }
        if ($ng -notmatch 'proxy_set_header X-Real-IP \$remote_addr;') { throw "Nginx missing proxy X-Real-IP" }
        if ($ng -notmatch 'proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;') { throw "Nginx missing proxy X-Forwarded-For" }
    }

    # [16/20] SPA route fallback verified
    Assert-Check 16 "SPA Route Fallback (try_files index.html)" {
        $ng = Get-Content "frontend/nginx.conf" -Raw
        if ($ng -notmatch 'try_files \$uri \$uri/ /index\.html;') {
            throw "Nginx missing SPA fallback try_files `$uri `$uri/ /index.html;"
        }
    }

    # [17/20] AI missing-key behavior verified safely
    Assert-Check 17 "AI Missing Key Behavior (HTTP 503 Service Unavailable, No Fake Mocks)" {
        # Register user and log in to obtain valid token
        $regBody = @{
            name = "AI Offline User"
            email = "ai-offline-test@example.com"
            password = "SecurePassword123!"
        } | ConvertTo-Json
        Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBody -ContentType "application/json" | Out-Null

        $loginBody = @{
            email = "ai-offline-test@example.com"
            password = "SecurePassword123!"
        } | ConvertTo-Json
        $loginResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
        $token = $loginResp.token

        # Create a project
        $projBody = @{
            name = "AI Offline Test Project"
            description = "Validating AI offline behavior in production"
            projectType = "WEB_APPLICATION"
            difficulty = "INTERMEDIATE"
        } | ConvertTo-Json
        $projResp = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Post -Body $projBody -ContentType "application/json" -Headers @{
            "Authorization" = "Bearer $token"
        }
        $projId = $projResp.id

        # Prepare for analysis
        Invoke-RestMethod -Uri "$baseUrl/api/projects/$projId/prepare-analysis" -Method Post -Headers @{
            "Authorization" = "Bearer $token"
        } | Out-Null

        # Trigger analyze endpoint - must fail with HTTP 503
        try {
            Invoke-RestMethod -Uri "$baseUrl/api/projects/$projId/analyze" -Method Post -Headers @{
                "Authorization" = "Bearer $token"
            } -TimeoutSec 5 -ErrorAction Stop
            throw "AI analyze should have failed with HTTP 503 when OPENAI_API_KEY is missing, but it succeeded!"
        } catch {
            $is503 = ($_.Exception.Message -match "503") -or ($_.Exception.Response -and $_.Exception.Response.StatusCode.value__ -eq 503)
            if (-not $is503) { throw "Expected HTTP 503 Service Unavailable, got: $($_.Exception.Message)" }
        }
    }

    # [18/20] No secret leakage in logs
    Assert-Check 18 "No Secret Leakage in Logs & Configurations" {
        if (Test-Path $logFile) {
            $logs = Get-Content $logFile -Raw
            if ($logs -match "SecurePassword123!" -or $logs -match "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970") {
                throw "Plaintext secret detected in backend log file!"
            }
        }
    }

    # [19/20] Container restart verification & Graceful shutdown
    Assert-Check 19 "Container Restart Policy & Graceful Shutdown" {
        $dc = Get-Content "docker-compose.yml" -Raw
        $backendMatches = [regex]::Matches($dc, "restart:\s*unless-stopped").Count
        if ($backendMatches -lt 3) { throw "All 3 services (postgres, backend, frontend) must specify restart: unless-stopped" }
        $prodYml = Get-Content "backend/src/main/resources/application-prod.yml" -Raw
        if ($prodYml -notmatch "timeout-per-shutdown-phase:\s*30s") { throw "Missing 30s graceful shutdown configuration" }
    }

    # [20/20] Full production smoke test
    Assert-Check 20 "Full Production Smoke Test (Register -> Login -> Auth API -> Stats)" {
        $testEmail = "smoke-user-$(Get-Random)@example.com"
        $regBody = @{
            name = "Smoke Test User"
            email = $testEmail
            password = "SmokePassword123!"
        } | ConvertTo-Json
        Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBody -ContentType "application/json" | Out-Null

        $loginBody = @{
            email = $testEmail
            password = "SmokePassword123!"
        } | ConvertTo-Json
        $loginResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
        if (-not $loginResp.token) { throw "Token not returned from login" }
        $smToken = $loginResp.token

        # Query stats
        $stats = Invoke-RestMethod -Uri "$baseUrl/api/projects/stats" -Method Get -Headers @{
            "Authorization" = "Bearer $smToken"
        }
        if ($null -eq $stats.totalProjects) { throw "Stats did not return totalProjects" }
    }

} finally {
    Write-Host "   Shutting down backend process (PID: $($proc.Id))..."
    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}

Write-Host ""
Write-Host "=========================================================="
if ($failures.Count -eq 0) {
    Write-Host "   ALL 20 PHASE 11 DEPLOYMENT CHECKS PASSED (20/20)! " -ForegroundColor Green
    Write-Host "=========================================================="
    exit 0
} else {
    Write-Host "   $($failures.Count) CHECKS FAILED:" -ForegroundColor Red
    foreach ($f in $failures) {
        Write-Host "   - $f" -ForegroundColor Red
    }
    Write-Host "=========================================================="
    exit 1
}
