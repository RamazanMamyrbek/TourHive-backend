# EP-0010 Moderation, Reports, Audit & Security Hardening E2E Verification Script
$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080"
$mailpitApi = "http://localhost:8025/api/v1"
$rnd = Get-Random -Minimum 100000 -Maximum 999999

Write-Host "=== EP-0010: MODERATION, REPORTS, AUDIT & HARDENING E2E TEST SUITE ===" -ForegroundColor Cyan

function Assert-Condition($condition, $message) {
    if (-not $condition) {
        Write-Host "[-] Assertion FAILED: $message" -ForegroundColor Red
        exit 1
    } else {
        Write-Host "[+] Assertion PASSED: $message" -ForegroundColor Green
    }
}

function Get-ErrorJson($ex) {
    if ($ex.ErrorDetails -and $ex.ErrorDetails.Message) {
        try { return ($ex.ErrorDetails.Message | ConvertFrom-Json) } catch {}
    }
    if ($ex.Exception.Response) {
        try {
            $stream = $ex.Exception.Response.GetResponseStream()
            if ($stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $text = $reader.ReadToEnd()
                return ($text | ConvertFrom-Json)
            }
        } catch {}
    }
    return $null
}

function Get-VerificationCode($email) {
    Start-Sleep -Milliseconds 800
    $msgs = Invoke-RestMethod -Uri "$mailpitApi/messages" -Method Get
    foreach ($m in $msgs.messages) {
        if ($m.To[0].Address -eq $email) {
            $msgDetail = Invoke-RestMethod -Uri "$mailpitApi/message/$($m.ID)" -Method Get
            if ($msgDetail.Text -match "(\d{6})") {
                return $matches[1]
            }
        }
    }
    throw "Verification code not found in Mailpit for $email"
}

function Register-And-Verify($email, $pwd, $nickname, $first, $last) {
    $body = @{
        email = $email
        password = $pwd
        role = "USER"
        firstName = $first
        lastName = $last
        nickname = $nickname
    } | ConvertTo-Json

    $reg = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/register" -Method Post -Body $body -ContentType "application/json"
    $code = Get-VerificationCode $email
    
    $verifyBody = @{
        email = $email
        code = $code
    } | ConvertTo-Json

    $res = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/verify-email" -Method Post -Body $verifyBody -ContentType "application/json"
    
    $prof = Invoke-RestMethod -Uri "$baseUrl/api/v1/users/me" -Method Get -Headers @{ Authorization = "Bearer $($res.data.accessToken)" }

    return @{
        email = $email
        userId = $prof.data.userId
        accessToken = $res.data.accessToken
        refreshToken = $res.data.refreshToken
    }
}

# 1. Health check
$health = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get
Assert-Condition ($health.status -eq "UP") "Backend is UP"

# 2. Register Users
$pwd = "StrongPass123!"
$userA = Register-And-Verify "ep10_reporter_$rnd@tourhive.com" $pwd "rep_$rnd" "Alice" "Reporter"
$userB = Register-And-Verify "ep10_violator_$rnd@tourhive.com" $pwd "vio_$rnd" "Bob" "Spammer"
$modUser = Register-And-Verify "ep10_mod_$rnd@tourhive.com" $pwd "mod_$rnd" "Mark" "Moderator"
$adminUser = Register-And-Verify "ep10_admin_$rnd@tourhive.com" $pwd "adm_$rnd" "Arthur" "Admin"

# Elevate mod to MODERATOR and admin to ADMIN in Postgres
& docker exec tourhive-postgres psql -U tourhive_user -d tourhive_db -c "UPDATE users SET role = 'MODERATOR' WHERE email = '$($modUser.email)';" | Out-Null
& docker exec tourhive-postgres psql -U tourhive_user -d tourhive_db -c "UPDATE users SET role = 'ADMIN' WHERE email = '$($adminUser.email)';" | Out-Null

# Login to get fresh tokens with updated roles
$modLogin = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method Post -Body (@{ email = "ep10_mod_$rnd@tourhive.com"; password = $pwd } | ConvertTo-Json) -ContentType "application/json"
$adminLogin = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method Post -Body (@{ email = "ep10_admin_$rnd@tourhive.com"; password = $pwd } | ConvertTo-Json) -ContentType "application/json"

$headersA = @{ Authorization = "Bearer $($userA.accessToken)" }
$headersB = @{ Authorization = "Bearer $($userB.accessToken)" }
$headersMod = @{ Authorization = "Bearer $($modLogin.data.accessToken)" }
$headersAdmin = @{ Authorization = "Bearer $($adminLogin.data.accessToken)" }

Write-Host "--- TEST GROUP 1: REPORT SUBMISSION & VALIDATION ---" -ForegroundColor Yellow

# 3. User A reports User B
$reportReq = @{
    targetType = "USER"
    targetId = $userB.userId
    reason = "SPAM"
    description = "User is spamming multiple direct messages advertising unauthorized external services."
} | ConvertTo-Json

$reportResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/reports" -Method Post -Body $reportReq -Headers $headersA -ContentType "application/json"
Assert-Condition ($reportResp.data.id -ne $null) "Report submitted successfully with ID: $($reportResp.data.id)"
Assert-Condition ($reportResp.data.status -eq "PENDING") "Initial report status is PENDING"
$reportId = $reportResp.data.id

# 4. User A attempts to report themselves (must fail with 400 CANNOT_REPORT_OWN_CONTENT)
$selfReportReq = @{
    targetType = "USER"
    targetId = $userA.userId
    reason = "OTHER"
    description = "Self reporting"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "$baseUrl/api/v1/reports" -Method Post -Body $selfReportReq -Headers $headersA -ContentType "application/json"
    Assert-Condition $false "Self report should have failed"
} catch {
    $err = Get-ErrorJson $_
    Assert-Condition ($err.code -eq "CANNOT_REPORT_OWN_CONTENT") "Self report correctly rejected with CANNOT_REPORT_OWN_CONTENT"
}

# 5. User A submits duplicate report on same target (must fail with 409 DUPLICATE_REPORT)
try {
    Invoke-RestMethod -Uri "$baseUrl/api/v1/reports" -Method Post -Body $reportReq -Headers $headersA -ContentType "application/json"
    Assert-Condition $false "Duplicate report should have failed"
} catch {
    $err = Get-ErrorJson $_
    Assert-Condition ($err.code -eq "DUPLICATE_REPORT") "Duplicate report correctly rejected with DUPLICATE_REPORT"
}

# 6. User A gets my submitted reports
$myReports = Invoke-RestMethod -Uri "$baseUrl/api/v1/reports/my" -Method Get -Headers $headersA
Assert-Condition ($myReports.data.content.Count -ge 1) "Reporter sees submitted report in /reports/my"

Write-Host "--- TEST GROUP 2: MODERATION & RESOLUTION ---" -ForegroundColor Yellow

# 7. Non-admin accessing moderator queue (must fail with 403)
try {
    Invoke-RestMethod -Uri "$baseUrl/api/v1/admin/reports" -Method Get -Headers $headersA
    Assert-Condition $false "Regular user accessing admin reports should fail"
} catch {
    Assert-Condition ($_.Exception.Response.StatusCode.value__ -eq 403) "Regular user blocked from admin reports with 403"
}

# 8. Moderator lists reports
$adminReports = Invoke-RestMethod -Uri "$baseUrl/api/v1/admin/reports?status=PENDING" -Method Get -Headers $headersMod
Assert-Condition ($adminReports.data.content.Count -ge 1) "Moderator sees pending reports in queue"

# 9. Moderator views report detail
$reportDetail = Invoke-RestMethod -Uri "$baseUrl/api/v1/admin/reports/$reportId" -Method Get -Headers $headersMod
Assert-Condition ($reportDetail.data.targetSummary -like "*ep10_violator_$rnd*") "Report detail contains resolved target summary"

# 10. Moderator resolves report with USER_BLOCKED
$resolveReq = @{
    status = "RESOLVED"
    actionTaken = "USER_BLOCKED"
    resolutionNotes = "Confirmed spamming behavior. Account suspended."
} | ConvertTo-Json

$resolvedReport = Invoke-RestMethod -Uri "$baseUrl/api/v1/admin/reports/$reportId/resolve" -Method Post -Body $resolveReq -Headers $headersMod -ContentType "application/json"
Assert-Condition ($resolvedReport.data.status -eq "RESOLVED") "Report resolved with status RESOLVED"
Assert-Condition ($resolvedReport.data.actionTaken -eq "USER_BLOCKED") "Action taken is USER_BLOCKED"

# 11. Verify User B is blocked
try {
    Invoke-RestMethod -Uri "$baseUrl/api/v1/users/me" -Method Get -Headers $headersB
    Assert-Condition $false "Blocked user token should be rejected"
} catch {
    $err = Get-ErrorJson $_
    Assert-Condition ($err.code -eq "ACCOUNT_BLOCKED") "Blocked user token rejected with 403 ACCOUNT_BLOCKED"
}

# Verify login for blocked user is rejected
try {
    Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method Post -Body (@{ email = "ep10_violator_$rnd@tourhive.com"; password = $pwd } | ConvertTo-Json) -ContentType "application/json"
    Assert-Condition $false "Blocked user login should be rejected"
} catch {
    $err = Get-ErrorJson $_
    Assert-Condition ($err.code -eq "ACCOUNT_BLOCKED") "Blocked user login rejected with 403 ACCOUNT_BLOCKED"
}

Write-Host "--- TEST GROUP 3: ADMIN USER MANAGEMENT ---" -ForegroundColor Yellow

# 12. Moderator views user detail
$userBDetail = Invoke-RestMethod -Uri "$baseUrl/api/v1/admin/users/$($userB.userId)" -Method Get -Headers $headersMod
Assert-Condition ($userBDetail.data.status -eq "BLOCKED") "Admin user detail reflects status BLOCKED"
Assert-Condition ($userBDetail.data.blockReason -like "*Confirmed spamming*") "Admin user detail includes blockReason"

# 13. Moderator unblocks User B
$unblocked = Invoke-RestMethod -Uri "$baseUrl/api/v1/admin/users/$($userB.userId)/unblock" -Method Post -Headers $headersMod
Assert-Condition ($unblocked.data.status -eq "ACTIVE") "User unblocked status is ACTIVE"

# 14. User B can login and access me again
$reLoginB = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method Post -Body (@{ email = "ep10_violator_$rnd@tourhive.com"; password = $pwd } | ConvertTo-Json) -ContentType "application/json"
$newHeadersB = @{ Authorization = "Bearer $($reLoginB.data.accessToken)" }
$profileB = Invoke-RestMethod -Uri "$baseUrl/api/v1/users/me" -Method Get -Headers $newHeadersB
Assert-Condition ($profileB.data.email -eq "ep10_violator_$rnd@tourhive.com") "Unblocked user successfully accesses profile"

# 15. Moderator tries to change role (must fail with 403, ADMIN only)
try {
    $roleReq = @{ role = "MODERATOR" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/v1/admin/users/$($userB.userId)/role" -Method Patch -Body $roleReq -Headers $headersMod -ContentType "application/json"
    Assert-Condition $false "Moderator should not be able to change user role"
} catch {
    Assert-Condition ($_.Exception.Response.StatusCode.value__ -eq 403) "Role change blocked for MODERATOR with 403"
}

# 16. Admin changes User B role to MODERATOR
$roleReq = @{ role = "MODERATOR" } | ConvertTo-Json
$roleUpdated = Invoke-RestMethod -Uri "$baseUrl/api/v1/admin/users/$($userB.userId)/role" -Method Patch -Body $roleReq -Headers $headersAdmin -ContentType "application/json"
Assert-Condition ($roleUpdated.data.role -eq "MODERATOR") "Admin successfully updated user role to MODERATOR"

Write-Host "--- TEST GROUP 4: AUDIT LOGS ---" -ForegroundColor Yellow

# 17. Admin retrieves audit logs
$auditLogs = Invoke-RestMethod -Uri "$baseUrl/api/v1/admin/audit-logs?page=0&size=20" -Method Get -Headers $headersAdmin
Assert-Condition ($auditLogs.data.content.Count -ge 4) "Audit logs contain administrative actions"

$actionsLogged = $auditLogs.data.content | ForEach-Object { $_.action }
Assert-Condition ($actionsLogged -contains "REPORT_RESOLVED") "Audit contains REPORT_RESOLVED"
Assert-Condition ($actionsLogged -contains "USER_UNBLOCKED") "Audit contains USER_UNBLOCKED"
Assert-Condition ($actionsLogged -contains "USER_ROLE_CHANGED") "Audit contains USER_ROLE_CHANGED"

Write-Host "--- TEST GROUP 5: ACCOUNT DELETION & INTEGRITY ---" -ForegroundColor Yellow

# 18. User self-deletion
$deleteResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/users/me" -Method Delete -Headers $headersA
Assert-Condition ($deleteResp.success -eq $true) "Account deletion returned success"

# 19. Subsequent request with deleted user token is rejected with 403 ACCOUNT_DELETED
try {
    Invoke-RestMethod -Uri "$baseUrl/api/v1/users/me" -Method Get -Headers $headersA
    Assert-Condition $false "Deleted user token should be rejected"
} catch {
    $err = Get-ErrorJson $_
    Assert-Condition ($err.code -eq "ACCOUNT_DELETED") "Deleted user token rejected with 403 ACCOUNT_DELETED"
}

# 20. Login attempt with deleted user account is rejected
try {
    Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method Post -Body (@{ email = "ep10_reporter_$rnd@tourhive.com"; password = $pwd } | ConvertTo-Json) -ContentType "application/json"
    Assert-Condition $false "Login for deleted account should fail"
} catch {
    $err = Get-ErrorJson $_
    Assert-Condition ($err.code -eq "INVALID_CREDENTIALS" -or $err.code -eq "ACCOUNT_DELETED") "Login with deleted account correctly rejected ($($err.code))"
}

Write-Host "`n>>> ALL EP-0010 E2E TESTS PASSED SUCCESSFULLY! <<<" -ForegroundColor Green
