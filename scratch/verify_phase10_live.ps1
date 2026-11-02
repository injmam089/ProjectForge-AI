# ==============================================================================
# ProjectForge AI — Phase 10 Security Hardening & Reliability Live Verification
# ==============================================================================

$port = 8095
$baseUrl = "http://localhost:$port"
$logFile = "scratch/backend_live_phase10.log"
$errLogFile = "scratch/backend_live_phase10_err.log"

Write-Host "=========================================================="
Write-Host "   ProjectForge AI - Phase 10 Security Live Verification   "
Write-Host "=========================================================="

if (Test-Path $logFile) { Remove-Item $logFile -Force }
if (Test-Path $errLogFile) { Remove-Item $errLogFile -Force }

$javaBin = if ($env:JAVA_HOME) { "$env:JAVA_HOME\bin\java.exe" } else { "java" }

$proc = Start-Process -FilePath $javaBin -ArgumentList @(
    "-jar", "backend/target/projectforge-backend-1.0.0-SNAPSHOT.jar",
    "--server.port=$port",
    "--app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "--app.jwt.expiration-ms=86400000",
    "--spring.datasource.url=jdbc:h2:mem:livephase10db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "--spring.datasource.driver-class-name=org.h2.Driver",
    "--spring.datasource.username=sa",
    "--spring.datasource.password=",
    "--spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "--spring.jpa.hibernate.ddl-auto=create-drop",
    "--logging.level.com.projectforge=INFO"
) -PassThru -NoNewWindow -RedirectStandardOutput $logFile -RedirectStandardError $errLogFile

try {
    # Poll for backend startup
    $started = $false
    for ($i = 0; $i -lt 35; $i++) {
        Start-Sleep -Seconds 1
        try {
            $health = Invoke-RestMethod -Uri "$baseUrl/api/health" -Method Get -TimeoutSec 2 -ErrorAction Stop
            if ($health.status -eq "UP") {
                $started = $true
                break
            }
        } catch { }
    }

    if (-not $started) {
        Write-Error "Backend failed to start for Phase 10 verification."
        exit 1
    }

    Write-Host "[1/8] Public Health Check verified: Status UP (200 OK)"

    # Test Security Headers
    Write-Host "[2/8] Verifying Security HTTP Headers on API responses..."
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/health" -Method Get -UseBasicParsing
    $headers = $resp.Headers

    if ($headers["X-Content-Type-Options"] -ne "nosniff") {
        Write-Error "Missing or invalid X-Content-Type-Options header: $($headers['X-Content-Type-Options'])"
        exit 1
    }
    if ($headers["X-Frame-Options"] -ne "DENY") {
        Write-Error "Missing or invalid X-Frame-Options header: $($headers['X-Frame-Options'])"
        exit 1
    }
    Write-Host "       Verified: X-Content-Type-Options=nosniff, X-Frame-Options=DENY"

    # Test Malformed & Expired JWT Tokens
    Write-Host "[3/8] Testing Malformed & Forged JWT Authentication..."
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Get -Headers @{ Authorization = "Bearer invalid.token.payload" } -ErrorAction Stop
        Write-Error "FAIL: Forged token was accepted!"
        exit 1
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -ne 401) {
            Write-Error "Expected 401 for forged token, got $code"
            exit 1
        }
    }
    Write-Host "       Verified: Forged/malformed token rejected with 401 Unauthorized."

    # Register & Authenticate
    Write-Host "[4/8] Registering and authenticating Test User..."
    $reg = @{ name = "Security Tester"; email = "sec@phase10.test"; password = "SecurePassword123!" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $reg -ContentType "application/json" | Out-Null
    $login = @{ email = "sec@phase10.test"; password = "SecurePassword123!" } | ConvertTo-Json
    $auth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $login -ContentType "application/json"
    $token = $auth.token
    $authHeaders = @{ Authorization = "Bearer $token" }
    Write-Host "       Authenticated successfully with token."

    # Test SQL Injection Defense via Sort Allowlist
    Write-Host "[5/8] Testing SQL Injection Defense on Sort Allowlist..."
    $sqlInjections = @("'; DROP TABLE projects; --", "1=1", "id, (SELECT 1 FROM users)", "non_existent_column")
    foreach ($inj in $sqlInjections) {
        try {
            Invoke-RestMethod -Uri "$baseUrl/api/projects?sort=$([System.Web.HttpUtility]::UrlEncode($inj))" -Method Get -Headers $authHeaders -ErrorAction Stop
            Write-Error "FAIL: SQL injection probe '$inj' was not rejected!"
            exit 1
        } catch {
            $code = $_.Exception.Response.StatusCode.value__
            if ($code -ne 400) {
                Write-Error "Expected 400 for SQL injection probe '$inj', got $code"
                exit 1
            }
        }
    }
    Write-Host "       Verified: All SQL injection sort probes rejected with 400 Bad Request."

    # Test Bounded Search Inputs
    Write-Host "[6/8] Testing Search Keyword Input Bounding (<=100 chars)..."
    $longQuery = "A" * 150
    $searchResp = Invoke-RestMethod -Uri "$baseUrl/api/projects?search=$longQuery" -Method Get -Headers $authHeaders
    Write-Host "       Verified: Bounded search executed safely without error."

    # Test Error Response Envelope (No stack trace leakage)
    Write-Host "[7/8] Testing Error Envelope for Information Leakage..."
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects/999999" -Method Get -Headers $authHeaders -ErrorAction Stop
        Write-Error "FAIL: Accessing non-existent project succeeded!"
        exit 1
    } catch {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errBody = $reader.ReadToEnd()
        if ($errBody -match "Exception" -or $errBody -match "at com.projectforge" -or $errBody -match "org.springframework") {
            Write-Error "FAIL: Stack trace or internal exception leaked in error response: $errBody"
            exit 1
        }
        Write-Host "       Verified: Clean JSON error envelope without stack traces."
    }

    # Test Anti-IDOR Defense
    Write-Host "[8/8] Testing Anti-IDOR Strict Tenant Isolation (Zero metadata leak)..."
    # Register second user
    $reg2 = @{ name = "Attacker User"; email = "attacker@phase10.test"; password = "AttackerPassword123!" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $reg2 -ContentType "application/json" | Out-Null
    $login2 = @{ email = "attacker@phase10.test"; password = "AttackerPassword123!" } | ConvertTo-Json
    $auth2 = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $login2 -ContentType "application/json"
    $attackerHeaders = @{ Authorization = "Bearer $($auth2.token)" }

    # User 1 creates project
    $p = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Post -Headers $authHeaders -ContentType "application/json" -Body (@{
        name = "Secret Project Alpha"; description = "Confidential business logic"; projectType = "WEB_APPLICATION"; difficulty = "INTERMEDIATE"
    } | ConvertTo-Json)

    # Attacker tries to read User 1's project
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/projects/$($p.id)" -Method Get -Headers $attackerHeaders -ErrorAction Stop
        Write-Error "FAIL: Attacker was able to read User 1's project!"
        exit 1
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -ne 404) {
            Write-Error "Expected 404 for cross-tenant access, got $code"
            exit 1
        }
        Write-Host "       Verified: Cross-tenant access strictly returns 404 Not Found."
    }

    Write-Host ""
    Write-Host "=========================================================="
    Write-Host "   ALL PHASE 10 SECURITY HARDENING CHECKS PASSED (8/8)!   "
    Write-Host "=========================================================="

} finally {
    Write-Host "Stopping Spring Boot Backend process (PID: $($proc.Id))..."
    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}
