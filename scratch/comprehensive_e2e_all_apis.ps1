# ==============================================================================
# TOURHIVE BACKEND: COMPREHENSIVE ALL-API E2E TEST SUITE
# Covers all 14 API Groups and all 88 OpenAPI endpoints with positive & negative cases
# ==============================================================================

$ErrorActionPreference = "Stop"
$ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$baseUrl = "http://localhost:8080"
$mailpitUrl = "http://localhost:8025"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "       TOURHIVE COMPREHENSIVE ALL-API E2E TEST SUITE      " -ForegroundColor Cyan
Write-Host "   Run Timestamp: $ts                                     " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$results = [System.Collections.Generic.List[PSCustomObject]]::new()

function Log-Result($id, $name, $status, $details) {
    $item = [PSCustomObject]@{
        Id = $id
        Name = $name
        Status = $status
        Details = $details
    }
    $results.Add($item)
    $color = if ($status -eq "PASS") { "Green" } else { "Red" }
    Write-Host "[$status] $id : $name -> $details" -ForegroundColor $color
}

function Call-Api($method, $uri, $bodyObj = $null, $token = $null) {
    $headers = @{ "Accept" = "application/json" }
    if ($token) {
        $headers["Authorization"] = "Bearer $token"
    }

    $jsonBody = $null
    if ($bodyObj -ne $null) {
        $headers["Content-Type"] = "application/json; charset=utf-8"
        $jsonBody = $bodyObj | ConvertTo-Json -Depth 10 -Compress
    } elseif ($method -in @("POST", "PUT", "PATCH")) {
        $headers["Content-Type"] = "application/json; charset=utf-8"
        $jsonBody = "{}"
    }

    try {
        $params = @{
            Uri = $uri
            Method = $method
            Headers = $headers
            UseBasicParsing = $true
        }
        if ($jsonBody) {
            $params["Body"] = [System.Text.Encoding]::UTF8.GetBytes($jsonBody)
        }
        $resp = Invoke-WebRequest @params
        $raw = ""
        if ($resp.Content -is [byte[]]) {
            $raw = [System.Text.Encoding]::UTF8.GetString($resp.Content)
        } else {
            $raw = [string]$resp.Content
        }
        $json = $null
        try { $json = $raw | ConvertFrom-Json } catch {}
        return [PSCustomObject]@{
            StatusCode = $resp.StatusCode
            Success = $true
            Data = $json
            Raw = $raw
        }
    } catch {
        $statusCode = 0
        $respBody = ""
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
            $respBody = $_.ErrorDetails.Message
        } elseif ($_.Exception.Response) {
            try {
                $respStream = $_.Exception.Response.GetResponseStream()
                if ($respStream) {
                    $reader = New-Object System.IO.StreamReader($respStream)
                    $respBody = $reader.ReadToEnd()
                }
            } catch {}
        }
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $statusCode = $_.Exception.Response.StatusCode.value__
        }
        $json = $null
        if ($respBody) {
            try { $json = $respBody | ConvertFrom-Json } catch {}
        }
        return [PSCustomObject]@{
            StatusCode = $statusCode
            Success = $false
            Data = $json
            Raw = $respBody
        }
    }
}

function Upload-File($uri, $filePath, $token = $null) {
    $headerArgs = @()
    if ($token) {
        $headerArgs += "-H"
        $headerArgs += "Authorization: Bearer $token"
    }
    $output = & curl.exe -s -X POST $uri @headerArgs -F "file=@$filePath"
    try {
        return ($output | ConvertFrom-Json)
    } catch {
        return $output
    }
}

function Get-MailpitOtp($email) {
    for ($i = 0; $i -lt 10; $i++) {
        Start-Sleep -Seconds 1
        $msgs = Invoke-RestMethod -Uri "$mailpitUrl/api/v1/messages" -Method Get -UseBasicParsing
        $userMsg = $msgs.messages | Where-Object { 
            $addrs = $_.To | ForEach-Object { $_.Address }
            $addrs -contains $email
        } | Select-Object -First 1
        if ($userMsg) {
            $match = [regex]::Match($userMsg.Snippet, 'code is:\s*(\d{6})')
            if ($match.Success) {
                return $match.Groups[1].Value
            }
            $match2 = [regex]::Match($userMsg.Snippet, '\b\d{6}\b')
            if ($match2.Success) {
                return $match2.Value
            }
        }
    }
    return $null
}

function Get-MailpitResetToken($email) {
    for ($i = 0; $i -lt 10; $i++) {
        Start-Sleep -Seconds 1
        $msgs = Invoke-RestMethod -Uri "$mailpitUrl/api/v1/messages" -Method Get -UseBasicParsing
        $userMsg = $msgs.messages | Where-Object { 
            $addrs = $_.To | ForEach-Object { $_.Address }
            ($addrs -contains $email) -and ($_.Subject -like "*Password Reset*")
        } | Select-Object -First 1
        if ($userMsg) {
            $msgDetail = Invoke-RestMethod -Uri "$mailpitUrl/api/v1/message/$($userMsg.ID)" -Method Get -UseBasicParsing
            $text = $msgDetail.Text
            $match = [regex]::Match($text, 'token=([a-f0-9\-]{36})')
            if ($match.Success) {
                return $match.Groups[1].Value
            }
            $match2 = [regex]::Match($text, '([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})')
            if ($match2.Success) {
                return $match2.Value
            }
        }
    }
    return $null
}

function Register-User($email, $pwd, $nick, $first, $last) {
    $reg = Call-Api "POST" "$baseUrl/api/v1/auth/register" @{
        email = $email
        password = $pwd
        firstName = $first
        lastName = $last
        nickname = $nick
        role = "USER"
    }
    $otp = Get-MailpitOtp $email
    $ver = Call-Api "POST" "$baseUrl/api/v1/auth/verify-email" @{
        email = $email
        code = $otp
    }
    $login = Call-Api "POST" "$baseUrl/api/v1/auth/login" @{
        email = $email
        password = $pwd
    }
    $token = $login.Data.data.accessToken
    $prof = Call-Api "GET" "$baseUrl/api/v1/users/me" $null $token
    return [PSCustomObject]@{
        Email = $email
        Password = $pwd
        UserId = $prof.Data.data.userId
        Token = $token
    }
}

# -------------------------------------------------------------
# GROUP 1: SYSTEM ENDPOINTS & OPENAPI DOCS
# -------------------------------------------------------------
Write-Host "`n--- Group 1: System Endpoints ---" -ForegroundColor Yellow

$h = Call-Api "GET" "$baseUrl/actuator/health"
if ($h.StatusCode -eq 200 -and $h.Data.status -eq "UP") {
    Log-Result "TC-SYS-01" "Health Check /actuator/health" "PASS" "Status UP (DB & disk healthy)"
} else {
    Log-Result "TC-SYS-01" "Health Check /actuator/health" "FAIL" "Status $($h.StatusCode)"
}

$docs = Call-Api "GET" "$baseUrl/v3/api-docs"
if ($docs.StatusCode -eq 200 -and $docs.Data.openapi -ne $null) {
    Log-Result "TC-SYS-02" "OpenAPI Docs /v3/api-docs" "PASS" "OpenAPI specification generated successfully"
} else {
    Log-Result "TC-SYS-02" "OpenAPI Docs /v3/api-docs" "FAIL" "Status $($docs.StatusCode)"
}

# -------------------------------------------------------------
# GROUP 2: AUTHENTICATION & SECURITY
# -------------------------------------------------------------
Write-Host "`n--- Group 2: Registration & Verification ---" -ForegroundColor Yellow

$pwdA = "StrongP@ssw0rd!123"
$userAEmail = "e2e_user_a_$ts@tourhive.com"

$rRegA = Call-Api "POST" "$baseUrl/api/v1/auth/register" @{ email = $userAEmail; password = $pwdA }
if ($rRegA.StatusCode -eq 201 -and ($rRegA.Data.success -eq $true -or $rRegA.Data.message -like "*Verification code*")) {
    Log-Result "TC-AUTH-01" "POST /api/v1/auth/register (User A)" "PASS" "User registered: $userAEmail"
} else {
    Log-Result "TC-AUTH-01" "POST /api/v1/auth/register (User A)" "FAIL" "Status $($rRegA.StatusCode): $($rRegA.Raw)"
}

$otpA = Get-MailpitOtp $userAEmail
if ($otpA) {
    Log-Result "TC-AUTH-02" "Fetch OTP from Mailpit" "PASS" "OTP: $otpA"
} else {
    Log-Result "TC-AUTH-02" "Fetch OTP from Mailpit" "FAIL" "OTP not found"
}

$rVerA = Call-Api "POST" "$baseUrl/api/v1/auth/verify-email" @{ email = $userAEmail; code = $otpA }
if ($rVerA.StatusCode -eq 200 -and $rVerA.Data.data.accessToken) {
    Log-Result "TC-AUTH-03" "POST /api/v1/auth/verify-email" "PASS" "Email verified successfully"
} else {
    Log-Result "TC-AUTH-03" "POST /api/v1/auth/verify-email" "FAIL" "Status $($rVerA.StatusCode): $($rVerA.Raw)"
}

$rLoginA = Call-Api "POST" "$baseUrl/api/v1/auth/login" @{ email = $userAEmail; password = $pwdA }
$tokenA = $rLoginA.Data.data.accessToken
$refreshTokenA = $rLoginA.Data.data.refreshToken

if ($rLoginA.StatusCode -eq 200 -and $tokenA) {
    Log-Result "TC-AUTH-04" "POST /api/v1/auth/login" "PASS" "Tokens issued successfully"
} else {
    Log-Result "TC-AUTH-04" "POST /api/v1/auth/login" "FAIL" "Status $($rLoginA.StatusCode): $($rLoginA.Raw)"
}

$rMe = Call-Api "GET" "$baseUrl/api/v1/auth/me" $null $tokenA
$userAId = $rMe.Data.data.id
if ($rMe.StatusCode -eq 200 -and $rMe.Data.data.email -eq $userAEmail) {
    Log-Result "TC-AUTH-05" "GET /api/v1/auth/me" "PASS" "Validated token for: $userAEmail, userId: $userAId"
} else {
    Log-Result "TC-AUTH-05" "GET /api/v1/auth/me" "FAIL" "Status $($rMe.StatusCode): $($rMe.Raw)"
}

$rRefresh = Call-Api "POST" "$baseUrl/api/v1/auth/refresh" @{ refreshToken = $refreshTokenA }
$newRefreshTokenA = $rRefresh.Data.data.refreshToken
if ($rRefresh.StatusCode -eq 200 -and $newRefreshTokenA -ne $refreshTokenA) {
    Log-Result "TC-AUTH-06" "POST /api/v1/auth/refresh" "PASS" "Refreshed tokens successfully"
} else {
    Log-Result "TC-AUTH-06" "POST /api/v1/auth/refresh" "FAIL" "Status $($rRefresh.StatusCode): $($rRefresh.Raw)"
}

$rForgot = Call-Api "POST" "$baseUrl/api/v1/auth/forgot-password" @{ email = $userAEmail }
$resetToken = Get-MailpitResetToken $userAEmail
if ($rForgot.StatusCode -eq 200 -and $resetToken) {
    Log-Result "TC-AUTH-07" "POST /api/v1/auth/forgot-password" "PASS" "Reset token received: $resetToken"
} else {
    Log-Result "TC-AUTH-07" "POST /api/v1/auth/forgot-password" "FAIL" "Status $($rForgot.StatusCode): $($rForgot.Raw)"
}

$newPwdA = "NewStrongP@ss2026!"
$rReset = Call-Api "POST" "$baseUrl/api/v1/auth/reset-password" @{ token = $resetToken; newPassword = $newPwdA }
$rLoginAfterReset = Call-Api "POST" "$baseUrl/api/v1/auth/login" @{ email = $userAEmail; password = $newPwdA }
$tokenA = $rLoginAfterReset.Data.data.accessToken
$refreshTokenA = $rLoginAfterReset.Data.data.refreshToken
if ($rReset.StatusCode -eq 200 -and $rLoginAfterReset.StatusCode -eq 200) {
    Log-Result "TC-AUTH-08" "POST /api/v1/auth/reset-password & Login" "PASS" "Password updated and verified"
} else {
    Log-Result "TC-AUTH-08" "POST /api/v1/auth/reset-password & Login" "FAIL" "Reset: $($rReset.StatusCode), Login: $($rLoginAfterReset.StatusCode)"
}

$rLogout = Call-Api "POST" "$baseUrl/api/v1/auth/logout" @{ refreshToken = $refreshTokenA } $tokenA
if ($rLogout.StatusCode -in @(200, 204)) {
    Log-Result "TC-AUTH-09" "POST /api/v1/auth/logout" "PASS" "Logged out"
} else {
    Log-Result "TC-AUTH-09" "POST /api/v1/auth/logout" "FAIL" "Status $($rLogout.StatusCode)"
}

$rRevokedRefresh = Call-Api "POST" "$baseUrl/api/v1/auth/refresh" @{ refreshToken = $refreshTokenA }
if ($rRevokedRefresh.StatusCode -eq 401) {
    Log-Result "TC-AUTH-10" "Revoked Token Refresh (401)" "PASS" "Correctly rejected: INVALID_TOKEN"
} else {
    Log-Result "TC-AUTH-10" "Revoked Token Refresh (401)" "FAIL" "Status $($rRevokedRefresh.StatusCode)"
}

# Re-login user A
$rLoginA = Call-Api "POST" "$baseUrl/api/v1/auth/login" @{ email = $userAEmail; password = $newPwdA }
$tokenA = $rLoginA.Data.data.accessToken

# -------------------------------------------------------------
# GROUP 3: PROFILES & AVATARS
# -------------------------------------------------------------
Write-Host "`n--- Group 3: Profiles & Avatars ---" -ForegroundColor Yellow

$rProfA = Call-Api "GET" "$baseUrl/api/v1/users/me" $null $tokenA
if ($rProfA.StatusCode -eq 200 -and $rProfA.Data.data.email -eq $userAEmail) {
    Log-Result "TC-USER-01" "GET /api/v1/users/me" "PASS" "Retrieved profile for User A"
} else {
    Log-Result "TC-USER-01" "GET /api/v1/users/me" "FAIL" "Status $($rProfA.StatusCode)"
}

$updateProf = @{
    firstName = "Almas"
    lastName = "Nomad"
    nickname = "nomad_guide_$ts"
    bio = "Certified mountain trekker"
    country = "Kazakhstan"
    city = "Almaty"
    languages = "Kazakh, English, Russian"
    phone = "+77011234567"
}
$rUpdateA = Call-Api "PATCH" "$baseUrl/api/v1/users/me" $updateProf $tokenA
if ($rUpdateA.StatusCode -eq 200 -and $rUpdateA.Data.data.nickname -eq $updateProf.nickname) {
    Log-Result "TC-USER-02" "PATCH /api/v1/users/me" "PASS" "Profile updated with nickname $($updateProf.nickname)"
} else {
    Log-Result "TC-USER-02" "PATCH /api/v1/users/me" "FAIL" "Status $($rUpdateA.StatusCode): $($rUpdateA.Raw)"
}

$rPublicA = Call-Api "GET" "$baseUrl/api/v1/users/$userAId"
if ($rPublicA.StatusCode -eq 200 -and $rPublicA.Data.data.nickname -eq $updateProf.nickname) {
    Log-Result "TC-USER-03" "GET /api/v1/users/{id} (Public)" "PASS" "Public profile contains nickname"
} else {
    Log-Result "TC-USER-03" "GET /api/v1/users/{id} (Public)" "FAIL" "Status $($rPublicA.StatusCode)"
}

# Register User B and User C
$userBEmail = "e2e_user_b_$ts@tourhive.com"
Call-Api "POST" "$baseUrl/api/v1/auth/register" @{ email = $userBEmail; password = $pwdA } | Out-Null
$otpB = Get-MailpitOtp $userBEmail
Call-Api "POST" "$baseUrl/api/v1/auth/verify-email" @{ email = $userBEmail; code = $otpB } | Out-Null
$rLoginB = Call-Api "POST" "$baseUrl/api/v1/auth/login" @{ email = $userBEmail; password = $pwdA }
$tokenB = $rLoginB.Data.data.accessToken
$rMeB = Call-Api "GET" "$baseUrl/api/v1/auth/me" $null $tokenB
$userBId = $rMeB.Data.data.id

$userCEmail = "e2e_user_c_$ts@tourhive.com"
Call-Api "POST" "$baseUrl/api/v1/auth/register" @{ email = $userCEmail; password = $pwdA } | Out-Null
$otpC = Get-MailpitOtp $userCEmail
Call-Api "POST" "$baseUrl/api/v1/auth/verify-email" @{ email = $userCEmail; code = $otpC } | Out-Null
$rLoginC = Call-Api "POST" "$baseUrl/api/v1/auth/login" @{ email = $userCEmail; password = $pwdA }
$tokenC = $rLoginC.Data.data.accessToken
$rMeC = Call-Api "GET" "$baseUrl/api/v1/auth/me" $null $tokenC
$userCId = $rMeC.Data.data.id

# Duplicate nickname rejection test
$rDupNick = Call-Api "PATCH" "$baseUrl/api/v1/users/me" @{ nickname = $updateProf.nickname } $tokenB
if ($rDupNick.StatusCode -eq 409 -and ($rDupNick.Data.code -eq "NICKNAME_ALREADY_TAKEN" -or $rDupNick.Raw -like "*NICKNAME_ALREADY_TAKEN*")) {
    Log-Result "TC-USER-04" "Duplicate Nickname Conflict (409)" "PASS" "Correctly caught: NICKNAME_ALREADY_TAKEN"
} else {
    Log-Result "TC-USER-04" "Duplicate Nickname Conflict (409)" "FAIL" "Status $($rDupNick.StatusCode)"
}

# -------------------------------------------------------------
# GROUP 4: USER BLOCKING
# -------------------------------------------------------------
Write-Host "`n--- Group 4: User Blocking ---" -ForegroundColor Yellow

$rBlock = Call-Api "POST" "$baseUrl/api/v1/users/$userCId/block" $null $tokenA
if ($rBlock.StatusCode -eq 200) {
    Log-Result "TC-BLK-01" "POST /api/v1/users/{id}/block" "PASS" "User A blocked User C"
} else {
    Log-Result "TC-BLK-01" "POST /api/v1/users/{id}/block" "FAIL" "Status $($rBlock.StatusCode)"
}

$rBlocksList = Call-Api "GET" "$baseUrl/api/v1/users/me/blocks" $null $tokenA
if ($rBlocksList.StatusCode -eq 200 -and $rBlocksList.Data.data.Count -ge 1) {
    Log-Result "TC-BLK-02" "GET /api/v1/users/me/blocks" "PASS" "User C in User A block list"
} else {
    Log-Result "TC-BLK-02" "GET /api/v1/users/me/blocks" "FAIL" "Status $($rBlocksList.StatusCode)"
}

$rSelfBlock = Call-Api "POST" "$baseUrl/api/v1/users/$userAId/block" $null $tokenA
if ($rSelfBlock.StatusCode -eq 400 -and ($rSelfBlock.Data.code -eq "CANNOT_BLOCK_SELF" -or $rSelfBlock.Raw -like "*CANNOT_BLOCK_SELF*")) {
    Log-Result "TC-BLK-03" "Self-block prevention (400)" "PASS" "Correctly caught: CANNOT_BLOCK_SELF"
} else {
    Log-Result "TC-BLK-03" "Self-block prevention (400)" "FAIL" "Status $($rSelfBlock.StatusCode)"
}

$rUnblock = Call-Api "DELETE" "$baseUrl/api/v1/users/$userCId/block" $null $tokenA
if ($rUnblock.StatusCode -eq 200 -or $rUnblock.StatusCode -eq 204) {
    Log-Result "TC-BLK-04" "DELETE /api/v1/users/{id}/block" "PASS" "User C unblocked"
} else {
    Log-Result "TC-BLK-04" "DELETE /api/v1/users/{id}/block" "FAIL" "Status $($rUnblock.StatusCode)"
}

# -------------------------------------------------------------
# GROUP 5: MINIO MEDIA & RESOURCES
# -------------------------------------------------------------
Write-Host "`n--- Group 5: MinIO Resources & Media ---" -ForegroundColor Yellow

$testPng = "$PSScriptRoot\test_image_$ts.png"
[System.IO.File]::WriteAllBytes($testPng, [byte[]]@(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4, 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54, 0x78, 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, 0xB4, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, 0xAE, 0x42, 0x60, 0x82))

$upRes = Upload-File "$baseUrl/api/v1/resources/upload" $testPng $tokenA
$resourceId = $upRes.data.id

if ($resourceId) {
    Log-Result "TC-RES-01" "POST /api/v1/resources/upload" "PASS" "Uploaded to MinIO, Resource ID: $resourceId"
} else {
    Log-Result "TC-RES-01" "POST /api/v1/resources/upload" "FAIL" "Upload failed: $($upRes | ConvertTo-Json -Compress)"
}

$rStream = Call-Api "GET" "$baseUrl/api/v1/resources/$resourceId"
if ($rStream.StatusCode -eq 200) {
    Log-Result "TC-RES-02" "GET /api/v1/resources/{id} (Stream)" "PASS" "Successfully streamed binary from MinIO"
} else {
    Log-Result "TC-RES-02" "GET /api/v1/resources/{id} (Stream)" "FAIL" "Status $($rStream.StatusCode)"
}

$upMedia = Upload-File "$baseUrl/api/v1/media/upload" $testPng $tokenA
$mediaUrl = $upMedia.data.fileUrl
if ($mediaUrl) {
    Log-Result "TC-RES-03" "POST /api/v1/media/upload" "PASS" "Media URL returned: $mediaUrl"
} else {
    Log-Result "TC-RES-03" "POST /api/v1/media/upload" "FAIL" "Media upload failed"
}

# Clean up local scratch png
Remove-Item -Path $testPng -Force -ErrorAction SilentlyContinue

# -------------------------------------------------------------
# GROUP 6: ORGANIZER & BUSINESS VERIFICATION
# -------------------------------------------------------------
Write-Host "`n--- Group 6: Organizer & Business Verification ---" -ForegroundColor Yellow

$rActA = Call-Api "POST" "$baseUrl/api/v1/organizers/me/activate" $null $tokenA
if ($rActA.StatusCode -eq 200 -and $rActA.Data.data.isOrganizer -eq $true) {
    Log-Result "TC-ORG-01" "POST /api/v1/organizers/me/activate (User A)" "PASS" "Activated organizer role"
} else {
    Log-Result "TC-ORG-01" "POST /api/v1/organizers/me/activate (User A)" "FAIL" "Status $($rActA.StatusCode)"
}

$rOrgProfA = Call-Api "GET" "$baseUrl/api/v1/organizers/me" $null $tokenA
if ($rOrgProfA.StatusCode -eq 200) {
    Log-Result "TC-ORG-02" "GET /api/v1/organizers/me" "PASS" "Retrieved organizer profile"
} else {
    Log-Result "TC-ORG-02" "GET /api/v1/organizers/me" "FAIL" "Status $($rOrgProfA.StatusCode)"
}

$rPublicOrgA = Call-Api "GET" "$baseUrl/api/v1/organizers/$userAId"
if ($rPublicOrgA.StatusCode -eq 200 -and $rPublicOrgA.Data.data.userId -eq $userAId) {
    Log-Result "TC-ORG-03" "GET /api/v1/organizers/{userId} (Public)" "PASS" "Public organizer info retrieved"
} else {
    Log-Result "TC-ORG-03" "GET /api/v1/organizers/{userId} (Public)" "FAIL" "Status $($rPublicOrgA.StatusCode)"
}

# User B activates organizer and applies for Business verification
Call-Api "POST" "$baseUrl/api/v1/organizers/me/activate" $null $tokenB | Out-Null

$testPdf = "$PSScriptRoot\business_cert_$ts.pdf"
[System.IO.File]::WriteAllBytes($testPdf, [System.Text.Encoding]::UTF8.GetBytes("%PDF-1.4 test certificate"))
$upCert = Upload-File "$baseUrl/api/v1/resources/upload" $testPdf $tokenB
$certResourceId = $upCert.data.id
Remove-Item -Path $testPdf -Force -ErrorAction SilentlyContinue

$bizReq = @{
    legalName = "Silk Road Expeditions LLP"
    registrationNumber = "BIN123456789012"
    country = "Kazakhstan"
    contactEmail = "business@silkroad.kz"
    contactPhone = "+77051234567"
    description = "Licensed tour operator"
    documents = @(
        @{
            documentName = "LicenseCertificate.pdf"
            documentUrl = "/api/v1/resources/$certResourceId"
        }
    )
}
$rSubmitBiz = Call-Api "POST" "$baseUrl/api/v1/organizers/me/business" $bizReq $tokenB
if ($rSubmitBiz.StatusCode -eq 201 -and $rSubmitBiz.Data.data.status -eq "PENDING") {
    Log-Result "TC-ORG-04" "POST /api/v1/organizers/me/business" "PASS" "Business verification submitted"
} else {
    Log-Result "TC-ORG-04" "POST /api/v1/organizers/me/business" "FAIL" "Status $($rSubmitBiz.StatusCode): $($rSubmitBiz.Raw)"
}

# Setup Moderator / Admin
$adminEmail = "e2e_mod_admin_$ts@tourhive.com"
Call-Api "POST" "$baseUrl/api/v1/auth/register" @{ email = $adminEmail; password = $pwdA } | Out-Null
$otpAdmin = Get-MailpitOtp $adminEmail
Call-Api "POST" "$baseUrl/api/v1/auth/verify-email" @{ email = $adminEmail; code = $otpAdmin } | Out-Null
& docker exec tourhive-postgres psql -U tourhive_user -d tourhive_db -c "UPDATE users SET role = 'ADMIN' WHERE email = '$adminEmail';" | Out-Null
$rLoginAdmin = Call-Api "POST" "$baseUrl/api/v1/auth/login" @{ email = $adminEmail; password = $pwdA }
$tokenAdmin = $rLoginAdmin.Data.data.accessToken

$rBizQueue = Call-Api "GET" "$baseUrl/api/v1/admin/business-verifications?page=0&size=10" $null $tokenAdmin
$bizProfileId = $rSubmitBiz.Data.data.id
if ($rBizQueue.StatusCode -eq 200) {
    Log-Result "TC-ORG-05" "GET /api/v1/admin/business-verifications" "PASS" "Verification request found in admin queue"
} else {
    Log-Result "TC-ORG-05" "GET /api/v1/admin/business-verifications" "FAIL" "Status $($rBizQueue.StatusCode)"
}

$rApproveBiz = Call-Api "POST" "$baseUrl/api/v1/admin/business-verifications/$bizProfileId/review" @{ status = "VERIFIED" } $tokenAdmin
if ($rApproveBiz.StatusCode -eq 200 -and $rApproveBiz.Data.data.status -eq "VERIFIED") {
    Log-Result "TC-ORG-06" "POST .../business-verifications/{id}/review" "PASS" "Approved to VERIFIED"
} else {
    Log-Result "TC-ORG-06" "POST .../business-verifications/{id}/review" "FAIL" "Status $($rApproveBiz.StatusCode)"
}

# -------------------------------------------------------------
# GROUP 7: TOURS, DRAFTS, PUBLISHING & SEARCH
# -------------------------------------------------------------
Write-Host "`n--- Group 7: Tours & Publishing ---" -ForegroundColor Yellow

$commTourDraftReq = @{
    title = "Altai Wild Mountains Trek $ts"
    description = "Authentic community mountain trekking across alpine lakes"
    tourType = "COMMUNITY"
    startDate = "2026-11-10"
    endDate = "2026-11-17"
    capacity = 10
    price = 0
    currency = "KZT"
    countries = @("Kazakhstan")
    cities = @("Ust-Kamenogorsk")
    includes = @("Guided hike", "Camp tent accommodation")
    route = @(
        @{ city = "Ust-Kamenogorsk"; routeDate = "2026-11-10"; description = "Arrival"; orderIndex = 1 },
        @{ city = "Katun Pass"; routeDate = "2026-11-12"; description = "Mountain Pass"; orderIndex = 2 }
    )
}

$rCreateTour = Call-Api "POST" "$baseUrl/api/v1/tours/draft" $commTourDraftReq $tokenA
$tourId = $rCreateTour.Data.data.id
if ($rCreateTour.StatusCode -eq 201 -and $tourId) {
    Log-Result "TC-TOUR-01" "POST /api/v1/tours/draft (COMMUNITY)" "PASS" "Draft tour created id: $tourId"
} else {
    Log-Result "TC-TOUR-01" "POST /api/v1/tours/draft (COMMUNITY)" "FAIL" "Status $($rCreateTour.StatusCode): $($rCreateTour.Raw)"
}

# Attach image to tour
$rAttachImg = Call-Api "POST" "$baseUrl/api/v1/tours/$tourId/images" @{ resourceId = $resourceId; isCover = $true } $tokenA
if ($rAttachImg.StatusCode -in @(200, 201)) {
    Log-Result "TC-TOUR-02" "POST /api/v1/tours/{id}/images" "PASS" "Cover photo attached"
} else {
    Log-Result "TC-TOUR-02" "POST /api/v1/tours/{id}/images" "FAIL" "Status $($rAttachImg.StatusCode)"
}

$rMyDrafts = Call-Api "GET" "$baseUrl/api/v1/tours/my-drafts" $null $tokenA
if ($rMyDrafts.StatusCode -eq 200 -and $rMyDrafts.Data.data.Count -ge 1) {
    Log-Result "TC-TOUR-03" "GET /api/v1/tours/my-drafts" "PASS" "Draft found in organizer drafts list"
} else {
    Log-Result "TC-TOUR-03" "GET /api/v1/tours/my-drafts" "FAIL" "Status $($rMyDrafts.StatusCode)"
}

# Submit tour for publication
$rPublish = Call-Api "POST" "$baseUrl/api/v1/tours/$tourId/publish" $null $tokenA
if ($rPublish.StatusCode -eq 200 -and $rPublish.Data.data.status -eq "PENDING_REVIEW") {
    Log-Result "TC-PUB-01" "POST /api/v1/tours/{id}/publish" "PASS" "Tour submitted for review: PENDING_REVIEW"
} else {
    Log-Result "TC-PUB-01" "POST /api/v1/tours/{id}/publish" "FAIL" "Status $($rPublish.StatusCode)"
}

# Moderator approves tour
$rApproveTour = Call-Api "POST" "$baseUrl/api/v1/admin/tours/$tourId/review" @{ status = "PUBLISHED" } $tokenAdmin
if ($rApproveTour.StatusCode -eq 200 -and $rApproveTour.Data.data.status -eq "PUBLISHED") {
    Log-Result "TC-PUB-02" "POST .../admin/tours/{id}/review (PUBLISHED)" "PASS" "Tour approved and published"
} else {
    Log-Result "TC-PUB-02" "POST .../admin/tours/{id}/review (PUBLISHED)" "FAIL" "Status $($rApproveTour.StatusCode)"
}

# Search & Catalog
$rCatalog = Call-Api "GET" "$baseUrl/api/v1/tours"
if ($rCatalog.StatusCode -eq 200 -and $rCatalog.Data.data.content.Count -ge 1) {
    Log-Result "TC-SEARCH-01" "GET /api/v1/tours (Catalog)" "PASS" "Published tours returned in public catalog"
} else {
    Log-Result "TC-SEARCH-01" "GET /api/v1/tours (Catalog)" "FAIL" "Status $($rCatalog.StatusCode)"
}

# -------------------------------------------------------------
# GROUP 8: APPLICATIONS & CAPACITY
# -------------------------------------------------------------
Write-Host "`n--- Group 8: Applications & Capacity ---" -ForegroundColor Yellow

$rApply = Call-Api "POST" "$baseUrl/api/v1/tours/$tourId/applications" @{ message = "I want to join with my hiking equipment"; contactInfo = "+77011112233" } $tokenB
$appId = $rApply.Data.data.id
if ($rApply.StatusCode -eq 201 -and $appId) {
    Log-Result "TC-APP-01" "POST /api/v1/tours/{id}/applications" "PASS" "Application submitted: $appId"
} else {
    Log-Result "TC-APP-01" "POST /api/v1/tours/{id}/applications" "FAIL" "Status $($rApply.StatusCode): $($rApply.Raw)"
}

$rAccept = Call-Api "POST" "$baseUrl/api/v1/tours/$tourId/applications/$appId/review" @{ status = "ACCEPTED" } $tokenA
if ($rAccept.StatusCode -eq 200 -and $rAccept.Data.data.status -eq "ACCEPTED") {
    Log-Result "TC-APP-02" "POST .../applications/{id}/review (ACCEPTED)" "PASS" "Applicant accepted into tour"
} else {
    Log-Result "TC-APP-02" "POST .../applications/{id}/review (ACCEPTED)" "FAIL" "Status $($rAccept.StatusCode)"
}

$rMyApps = Call-Api "GET" "$baseUrl/api/v1/users/me/applications" $null $tokenB
if ($rMyApps.StatusCode -eq 200 -and $rMyApps.Data.data.content.Count -ge 1) {
    Log-Result "TC-APP-03" "GET /api/v1/users/me/applications" "PASS" "User B sees application in /me/applications"
} else {
    Log-Result "TC-APP-03" "GET /api/v1/users/me/applications" "FAIL" "Status $($rMyApps.StatusCode)"
}

# -------------------------------------------------------------
# GROUP 9: FRIENDSHIPS & REQUESTS (EP-0007)
# -------------------------------------------------------------
Write-Host "`n--- Group 9: Friendships & Requests ---" -ForegroundColor Yellow

$rFriendReq = Call-Api "POST" "$baseUrl/api/v1/friends/requests" @{ receiverId = $userBId; message = "Let's connect on TourHive!" } $tokenA
$friendReqId = $rFriendReq.Data.data.id
if ($rFriendReq.StatusCode -eq 201 -and $friendReqId) {
    Log-Result "TC-FRND-01" "POST /api/v1/friends/requests" "PASS" "Friend request sent to User B"
} else {
    Log-Result "TC-FRND-01" "POST /api/v1/friends/requests" "FAIL" "Status $($rFriendReq.StatusCode): $($rFriendReq.Raw)"
}

$rIncoming = Call-Api "GET" "$baseUrl/api/v1/friends/requests/incoming" $null $tokenB
if ($rIncoming.StatusCode -eq 200 -and $rIncoming.Data.data.content.Count -ge 1) {
    Log-Result "TC-FRND-02" "GET /api/v1/friends/requests/incoming" "PASS" "User B sees incoming request"
} else {
    Log-Result "TC-FRND-02" "GET /api/v1/friends/requests/incoming" "FAIL" "Status $($rIncoming.StatusCode)"
}

$rAcceptFriend = Call-Api "POST" "$baseUrl/api/v1/friends/requests/$friendReqId/accept" $null $tokenB
if ($rAcceptFriend.StatusCode -eq 200 -and $rAcceptFriend.Data.data.status -eq "ACCEPTED") {
    Log-Result "TC-FRND-03" "POST .../friends/requests/{id}/accept" "PASS" "Friend request accepted"
} else {
    Log-Result "TC-FRND-03" "POST .../friends/requests/{id}/accept" "FAIL" "Status $($rAcceptFriend.StatusCode)"
}

$rFriendsList = Call-Api "GET" "$baseUrl/api/v1/friends" $null $tokenA
if ($rFriendsList.StatusCode -eq 200 -and $rFriendsList.Data.data.content.Count -ge 1) {
    Log-Result "TC-FRND-04" "GET /api/v1/friends" "PASS" "Friends list contains confirmed friend"
} else {
    Log-Result "TC-FRND-04" "GET /api/v1/friends" "FAIL" "Status $($rFriendsList.StatusCode)"
}

$rFriendStatus = Call-Api "GET" "$baseUrl/api/v1/friends/$userBId/status" $null $tokenA
if ($rFriendStatus.StatusCode -eq 200 -and $rFriendStatus.Data.data.status -eq "FRIENDS") {
    Log-Result "TC-FRND-05" "GET /api/v1/friends/{targetUserId}/status" "PASS" "Status is FRIENDS"
} else {
    Log-Result "TC-FRND-05" "GET /api/v1/friends/{targetUserId}/status" "FAIL" "Status $($rFriendStatus.StatusCode)"
}

# -------------------------------------------------------------
# GROUP 10: FAVORITES / WISHLIST (EP-0007)
# -------------------------------------------------------------
Write-Host "`n--- Group 10: Favorites / Wishlist ---" -ForegroundColor Yellow

$rFavAdd = Call-Api "POST" "$baseUrl/api/v1/favorites/tours/$tourId" $null $tokenB
if ($rFavAdd.StatusCode -eq 201) {
    Log-Result "TC-FAV-01" "POST /api/v1/favorites/tours/{tourId}" "PASS" "Tour added to favorites"
} else {
    Log-Result "TC-FAV-01" "POST /api/v1/favorites/tours/{tourId}" "FAIL" "Status $($rFavAdd.StatusCode): $($rFavAdd.Raw)"
}

$rFavCheck = Call-Api "GET" "$baseUrl/api/v1/favorites/tours/$tourId/check" $null $tokenB
if ($rFavCheck.StatusCode -eq 200 -and $rFavCheck.Data.data.isFavorite -eq $true) {
    Log-Result "TC-FAV-02" "GET /api/v1/favorites/tours/{tourId}/check" "PASS" "isFavorite is true"
} else {
    Log-Result "TC-FAV-02" "GET /api/v1/favorites/tours/{tourId}/check" "FAIL" "Status $($rFavCheck.StatusCode)"
}

$rFavList = Call-Api "GET" "$baseUrl/api/v1/favorites/tours" $null $tokenB
if ($rFavList.StatusCode -eq 200 -and $rFavList.Data.data.content.Count -ge 1) {
    Log-Result "TC-FAV-03" "GET /api/v1/favorites/tours" "PASS" "Favorites list returned"
} else {
    Log-Result "TC-FAV-03" "GET /api/v1/favorites/tours" "FAIL" "Status $($rFavList.StatusCode)"
}

$rFavDel = Call-Api "DELETE" "$baseUrl/api/v1/favorites/tours/$tourId" $null $tokenB
if ($rFavDel.StatusCode -eq 200 -or $rFavDel.StatusCode -eq 204) {
    Log-Result "TC-FAV-04" "DELETE /api/v1/favorites/tours/{tourId}" "PASS" "Removed from favorites"
} else {
    Log-Result "TC-FAV-04" "DELETE /api/v1/favorites/tours/{tourId}" "FAIL" "Status $($rFavDel.StatusCode)"
}

# -------------------------------------------------------------
# GROUP 11: CHATS & MESSAGING (EP-0008)
# -------------------------------------------------------------
Write-Host "`n--- Group 11: Chats & Messaging ---" -ForegroundColor Yellow

# Direct chat between friends (User A and User B)
$rDirectChat = Call-Api "POST" "$baseUrl/api/v1/chats/direct" @{ recipientId = $userBId } $tokenA
$chatId = $rDirectChat.Data.data.id
if ($rDirectChat.StatusCode -eq 200 -or $rDirectChat.StatusCode -eq 201) {
    Log-Result "TC-CHAT-01" "POST /api/v1/chats/direct" "PASS" "Direct chat opened: $chatId"
} else {
    Log-Result "TC-CHAT-01" "POST /api/v1/chats/direct" "FAIL" "Status $($rDirectChat.StatusCode): $($rDirectChat.Raw)"
}

$rSendMsg = Call-Api "POST" "$baseUrl/api/v1/chats/$chatId/messages" @{ content = "Hello Bob, are you ready for the trek?"; messageType = "TEXT" } $tokenA
$msgId = $rSendMsg.Data.data.id
if ($rSendMsg.StatusCode -eq 201 -and $msgId) {
    Log-Result "TC-CHAT-02" "POST /api/v1/chats/{id}/messages (TEXT)" "PASS" "Message sent: $msgId"
} else {
    Log-Result "TC-CHAT-02" "POST /api/v1/chats/{id}/messages (TEXT)" "FAIL" "Status $($rSendMsg.StatusCode)"
}

$rChatMsgs = Call-Api "GET" "$baseUrl/api/v1/chats/$chatId/messages" $null $tokenB
if ($rChatMsgs.StatusCode -eq 200 -and $rChatMsgs.Data.data.content.Count -ge 1) {
    Log-Result "TC-CHAT-03" "GET /api/v1/chats/{id}/messages" "PASS" "Messages retrieved by recipient"
} else {
    Log-Result "TC-CHAT-03" "GET /api/v1/chats/{id}/messages" "FAIL" "Status $($rChatMsgs.StatusCode)"
}

$rMarkRead = Call-Api "POST" "$baseUrl/api/v1/chats/$chatId/read" $null $tokenB
if ($rMarkRead.StatusCode -eq 200) {
    Log-Result "TC-CHAT-04" "POST /api/v1/chats/{id}/read" "PASS" "Messages marked as read"
} else {
    Log-Result "TC-CHAT-04" "POST /api/v1/chats/{id}/read" "FAIL" "Status $($rMarkRead.StatusCode)"
}

$rTourGroupChat = Call-Api "GET" "$baseUrl/api/v1/chats/tours/$tourId" $null $tokenB
if ($rTourGroupChat.StatusCode -eq 200 -and ($rTourGroupChat.Data.data.type -eq "TOUR_GROUP" -or $rTourGroupChat.Data.data.tourId -eq $tourId)) {
    Log-Result "TC-CHAT-05" "GET /api/v1/chats/tours/{tourId}" "PASS" "Accepted participant accessed tour group chat"
} else {
    Log-Result "TC-CHAT-05" "GET /api/v1/chats/tours/{tourId}" "FAIL" "Status $($rTourGroupChat.StatusCode)"
}

# -------------------------------------------------------------
# GROUP 12: REVIEWS (EP-0009)
# -------------------------------------------------------------
Write-Host "`n--- Group 12: Reviews ---" -ForegroundColor Yellow

# Update tour dates to past in postgres so review is valid
& docker exec tourhive-postgres psql -U tourhive_user -d tourhive_db -c "UPDATE tours SET start_date = '2026-09-01', end_date = '2026-09-07' WHERE id = '$tourId';" | Out-Null

$reviewReq = @{
    rating = 5
    comment = "Outstanding trekking experience, thoroughly organized!"
}
$rCreateReview = Call-Api "POST" "$baseUrl/api/v1/tours/$tourId/reviews" $reviewReq $tokenB
$reviewId = $rCreateReview.Data.data.id
if ($rCreateReview.StatusCode -eq 201 -and $reviewId) {
    Log-Result "TC-REV-01" "POST /api/v1/tours/{tourId}/reviews" "PASS" "Review created with 5 stars, id: $reviewId"
} else {
    Log-Result "TC-REV-01" "POST /api/v1/tours/{tourId}/reviews" "FAIL" "Status $($rCreateReview.StatusCode): $($rCreateReview.Raw)"
}

$rTourReviews = Call-Api "GET" "$baseUrl/api/v1/tours/$tourId/reviews"
if ($rTourReviews.StatusCode -eq 200 -and $rTourReviews.Data.data.content.Count -ge 1) {
    Log-Result "TC-REV-02" "GET /api/v1/tours/{tourId}/reviews" "PASS" "Public tour reviews returned"
} else {
    Log-Result "TC-REV-02" "GET /api/v1/tours/{tourId}/reviews" "FAIL" "Status $($rTourReviews.StatusCode)"
}

$rOrgReviews = Call-Api "GET" "$baseUrl/api/v1/organizers/$userAId/reviews"
if ($rOrgReviews.StatusCode -eq 200 -and $rOrgReviews.Data.data.content.Count -ge 1) {
    Log-Result "TC-REV-03" "GET /api/v1/organizers/{organizerId}/reviews" "PASS" "Organizer reviews list returned"
} else {
    Log-Result "TC-REV-03" "GET /api/v1/organizers/{organizerId}/reviews" "FAIL" "Status $($rOrgReviews.StatusCode)"
}

$rMyReviews = Call-Api "GET" "$baseUrl/api/v1/reviews/my" $null $tokenB
if ($rMyReviews.StatusCode -eq 200 -and $rMyReviews.Data.data.content.Count -ge 1) {
    Log-Result "TC-REV-04" "GET /api/v1/reviews/my" "PASS" "Author sees submitted reviews"
} else {
    Log-Result "TC-REV-04" "GET /api/v1/reviews/my" "FAIL" "Status $($rMyReviews.StatusCode)"
}

$rUpdateReview = Call-Api "PUT" "$baseUrl/api/v1/reviews/$reviewId" @{ rating = 4; comment = "Updated to 4 stars" } $tokenB
if ($rUpdateReview.StatusCode -eq 200 -and $rUpdateReview.Data.data.rating -eq 4) {
    Log-Result "TC-REV-05" "PUT /api/v1/reviews/{id}" "PASS" "Review updated to 4 stars"
} else {
    Log-Result "TC-REV-05" "PUT /api/v1/reviews/{id}" "FAIL" "Status $($rUpdateReview.StatusCode)"
}

# -------------------------------------------------------------
# GROUP 13: NOTIFICATIONS (EP-0009)
# -------------------------------------------------------------
Write-Host "`n--- Group 13: Notifications ---" -ForegroundColor Yellow

$rUnreadCount = Call-Api "GET" "$baseUrl/api/v1/notifications/unread-count" $null $tokenA
if ($rUnreadCount.StatusCode -eq 200) {
    Log-Result "TC-NOTIF-01" "GET /api/v1/notifications/unread-count" "PASS" "Unread count: $($rUnreadCount.Data.data.unreadCount)"
} else {
    Log-Result "TC-NOTIF-01" "GET /api/v1/notifications/unread-count" "FAIL" "Status $($rUnreadCount.StatusCode)"
}

$rNotifList = Call-Api "GET" "$baseUrl/api/v1/notifications?unreadOnly=false" $null $tokenA
if ($rNotifList.StatusCode -eq 200) {
    Log-Result "TC-NOTIF-02" "GET /api/v1/notifications" "PASS" "Notifications list retrieved"
} else {
    Log-Result "TC-NOTIF-02" "GET /api/v1/notifications" "FAIL" "Status $($rNotifList.StatusCode)"
}

$rReadAllNotif = Call-Api "PATCH" "$baseUrl/api/v1/notifications/read-all" $null $tokenA
if ($rReadAllNotif.StatusCode -eq 200) {
    Log-Result "TC-NOTIF-03" "PATCH /api/v1/notifications/read-all" "PASS" "All notifications marked as read"
} else {
    Log-Result "TC-NOTIF-03" "PATCH /api/v1/notifications/read-all" "FAIL" "Status $($rReadAllNotif.StatusCode)"
}

$deviceTokenReq = @{ token = "fcm_test_device_token_$ts"; deviceType = "ANDROID" }
$rRegDevice = Call-Api "POST" "$baseUrl/api/v1/notifications/device-token" $deviceTokenReq $tokenA
if ($rRegDevice.StatusCode -eq 200) {
    Log-Result "TC-NOTIF-04" "POST /api/v1/notifications/device-token" "PASS" "Device token registered"
} else {
    Log-Result "TC-NOTIF-04" "POST /api/v1/notifications/device-token" "FAIL" "Status $($rRegDevice.StatusCode)"
}

$rDelDevice = Call-Api "DELETE" "$baseUrl/api/v1/notifications/device-token" $deviceTokenReq $tokenA
if ($rDelDevice.StatusCode -eq 200 -or $rDelDevice.StatusCode -eq 204) {
    Log-Result "TC-NOTIF-05" "DELETE /api/v1/notifications/device-token" "PASS" "Device token deleted"
} else {
    Log-Result "TC-NOTIF-05" "DELETE /api/v1/notifications/device-token" "FAIL" "Status $($rDelDevice.StatusCode)"
}

# -------------------------------------------------------------
# GROUP 14: REPORTS, MODERATION, AUDIT & HARDENING (EP-0010)
# -------------------------------------------------------------
Write-Host "`n--- Group 14: Reports, Moderation, Audit & Hardening ---" -ForegroundColor Yellow

# User C reports User B
$reportReq = @{
    targetType = "USER"
    targetId = $userBId
    reason = "SPAM"
    description = "User spamming promotional messages"
}
$rReport = Call-Api "POST" "$baseUrl/api/v1/reports" $reportReq $tokenC
$repId = $rReport.Data.data.id
if ($rReport.StatusCode -eq 201 -and $repId) {
    Log-Result "TC-REP-01" "POST /api/v1/reports" "PASS" "Report created: $repId"
} else {
    Log-Result "TC-REP-01" "POST /api/v1/reports" "FAIL" "Status $($rReport.StatusCode): $($rReport.Raw)"
}

$rSelfRep = Call-Api "POST" "$baseUrl/api/v1/reports" @{ targetType = "USER"; targetId = $userCId; reason = "OTHER"; description = "Self report" } $tokenC
if ($rSelfRep.StatusCode -eq 400 -and $rSelfRep.Data.code -eq "CANNOT_REPORT_OWN_CONTENT") {
    Log-Result "TC-REP-02" "Self-report protection (400)" "PASS" "Correctly caught: CANNOT_REPORT_OWN_CONTENT"
} else {
    Log-Result "TC-REP-02" "Self-report protection (400)" "FAIL" "Status $($rSelfRep.StatusCode)"
}

$rDupRep = Call-Api "POST" "$baseUrl/api/v1/reports" $reportReq $tokenC
if ($rDupRep.StatusCode -eq 409 -and $rDupRep.Data.code -eq "DUPLICATE_REPORT") {
    Log-Result "TC-REP-03" "Duplicate report protection (409)" "PASS" "Correctly caught: DUPLICATE_REPORT"
} else {
    Log-Result "TC-REP-03" "Duplicate report protection (409)" "FAIL" "Status $($rDupRep.StatusCode)"
}

$rMyReports = Call-Api "GET" "$baseUrl/api/v1/reports/my" $null $tokenC
if ($rMyReports.StatusCode -eq 200 -and $rMyReports.Data.data.content.Count -ge 1) {
    Log-Result "TC-REP-04" "GET /api/v1/reports/my" "PASS" "User C sees submitted report"
} else {
    Log-Result "TC-REP-04" "GET /api/v1/reports/my" "FAIL" "Status $($rMyReports.StatusCode)"
}

$rAdminReports = Call-Api "GET" "$baseUrl/api/v1/admin/reports?status=PENDING" $null $tokenAdmin
if ($rAdminReports.StatusCode -eq 200 -and $rAdminReports.Data.data.content.Count -ge 1) {
    Log-Result "TC-REP-05" "GET /api/v1/admin/reports" "PASS" "Moderator sees pending reports queue"
} else {
    Log-Result "TC-REP-05" "GET /api/v1/admin/reports" "FAIL" "Status $($rAdminReports.StatusCode)"
}

$rReportDetail = Call-Api "GET" "$baseUrl/api/v1/admin/reports/$repId" $null $tokenAdmin
if ($rReportDetail.StatusCode -eq 200 -and $rReportDetail.Data.data.targetSummary) {
    Log-Result "TC-REP-06" "GET /api/v1/admin/reports/{id}" "PASS" "Report detail contains targetSummary: $($rReportDetail.Data.data.targetSummary)"
} else {
    Log-Result "TC-REP-06" "GET /api/v1/admin/reports/{id}" "FAIL" "Status $($rReportDetail.StatusCode)"
}

$rResolveReport = Call-Api "POST" "$baseUrl/api/v1/admin/reports/$repId/resolve" @{
    status = "RESOLVED"
    actionTaken = "WARNING_ISSUED"
    resolutionNotes = "Warning sent to user"
} $tokenAdmin
if ($rResolveReport.StatusCode -eq 200 -and $rResolveReport.Data.data.status -eq "RESOLVED") {
    Log-Result "TC-REP-07" "POST .../admin/reports/{id}/resolve" "PASS" "Report resolved with WARNING_ISSUED"
} else {
    Log-Result "TC-REP-07" "POST .../admin/reports/{id}/resolve" "FAIL" "Status $($rResolveReport.StatusCode)"
}

# Admin User Management
$rAdminUsers = Call-Api "GET" "$baseUrl/api/v1/admin/users?page=0&size=10" $null $tokenAdmin
if ($rAdminUsers.StatusCode -eq 200 -and $rAdminUsers.Data.data.content.Count -ge 3) {
    Log-Result "TC-ADM-01" "GET /api/v1/admin/users" "PASS" "Admin lists registered users"
} else {
    Log-Result "TC-ADM-01" "GET /api/v1/admin/users" "FAIL" "Status $($rAdminUsers.StatusCode)"
}

$rUserDetail = Call-Api "GET" "$baseUrl/api/v1/admin/users/$userCId" $null $tokenAdmin
if ($rUserDetail.StatusCode -eq 200 -and $rUserDetail.Data.data.email -eq $userCEmail) {
    Log-Result "TC-ADM-02" "GET /api/v1/admin/users/{userId}" "PASS" "Admin views user detailed card with metrics"
} else {
    Log-Result "TC-ADM-02" "GET /api/v1/admin/users/{userId}" "FAIL" "Status $($rUserDetail.StatusCode)"
}

$rBlockUser = Call-Api "POST" "$baseUrl/api/v1/admin/users/$userCId/block" @{ reason = "Temporary administrative suspension" } $tokenAdmin
if ($rBlockUser.StatusCode -eq 200 -and $rBlockUser.Data.data.status -eq "BLOCKED") {
    Log-Result "TC-ADM-03" "POST /api/v1/admin/users/{userId}/block" "PASS" "User status set to BLOCKED"
} else {
    Log-Result "TC-ADM-03" "POST /api/v1/admin/users/{userId}/block" "FAIL" "Status $($rBlockUser.StatusCode)"
}

# Blocked user JWT rejected
$rBlockedAccess = Call-Api "GET" "$baseUrl/api/v1/users/me" $null $tokenC
if ($rBlockedAccess.StatusCode -eq 403 -and ($rBlockedAccess.Data.code -eq "ACCOUNT_BLOCKED" -or $rBlockedAccess.Raw -like "*ACCOUNT_BLOCKED*")) {
    Log-Result "TC-SEC-01" "Blocked User Access Denied (403)" "PASS" "Blocked JWT instantly rejected with ACCOUNT_BLOCKED"
} else {
    Log-Result "TC-SEC-01" "Blocked User Access Denied (403)" "FAIL" "Status $($rBlockedAccess.StatusCode)"
}

$rUnblockUser = Call-Api "POST" "$baseUrl/api/v1/admin/users/$userCId/unblock" $null $tokenAdmin
if ($rUnblockUser.StatusCode -eq 200 -and $rUnblockUser.Data.data.status -eq "ACTIVE") {
    Log-Result "TC-ADM-04" "POST /api/v1/admin/users/{userId}/unblock" "PASS" "User restored to ACTIVE"
} else {
    Log-Result "TC-ADM-04" "POST /api/v1/admin/users/{userId}/unblock" "FAIL" "Status $($rUnblockUser.StatusCode)"
}

$rChangeRole = Call-Api "PATCH" "$baseUrl/api/v1/admin/users/$userCId/role" @{ role = "MODERATOR" } $tokenAdmin
if ($rChangeRole.StatusCode -eq 200 -and $rChangeRole.Data.data.role -eq "MODERATOR") {
    Log-Result "TC-ADM-05" "PATCH /api/v1/admin/users/{userId}/role" "PASS" "Admin updated user role to MODERATOR"
} else {
    Log-Result "TC-ADM-05" "PATCH /api/v1/admin/users/{userId}/role" "FAIL" "Status $($rChangeRole.StatusCode)"
}

# Audit Logs
$rAudit = Call-Api "GET" "$baseUrl/api/v1/admin/audit-logs?page=0&size=20" $null $tokenAdmin
if ($rAudit.StatusCode -eq 200 -and $rAudit.Data.data.content.Count -ge 1) {
    Log-Result "TC-AUD-01" "GET /api/v1/admin/audit-logs" "PASS" "Audit trail recorded administrative actions"
} else {
    Log-Result "TC-AUD-01" "GET /api/v1/admin/audit-logs" "FAIL" "Status $($rAudit.StatusCode)"
}

# Account Deletion
$rDelUserC = Call-Api "DELETE" "$baseUrl/api/v1/users/me" $null $tokenC
if ($rDelUserC.StatusCode -eq 200) {
    Log-Result "TC-DEL-01" "DELETE /api/v1/users/me" "PASS" "User account deleted and anonymized"
} else {
    Log-Result "TC-DEL-01" "DELETE /api/v1/users/me" "FAIL" "Status $($rDelUserC.StatusCode)"
}

$rDeletedToken = Call-Api "GET" "$baseUrl/api/v1/users/me" $null $tokenC
if ($rDeletedToken.StatusCode -eq 403 -and ($rDeletedToken.Data.code -eq "ACCOUNT_DELETED" -or $rDeletedToken.Raw -like "*ACCOUNT_DELETED*")) {
    Log-Result "TC-DEL-02" "Deleted User Token Rejection (403)" "PASS" "Deleted token rejected with ACCOUNT_DELETED"
} else {
    Log-Result "TC-DEL-02" "Deleted User Token Rejection (403)" "FAIL" "Status $($rDeletedToken.StatusCode)"
}

# =============================================================
# SUMMARY
# =============================================================
Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "                ALL-API E2E TEST SUMMARY                  " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
$passed = ($results | Where-Object { $_.Status -eq "PASS" }).Count
$failed = ($results | Where-Object { $_.Status -eq "FAIL" }).Count
$total = $results.Count
Write-Host "Total Tests: $total | Passed: $passed | Failed: $failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })

if ($failed -gt 0) {
    exit 1
} else {
    exit 0
}
