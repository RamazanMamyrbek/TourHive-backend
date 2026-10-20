# TourHive Backend Complete Master E2E Test Suite
# Tests all endpoints across Groups 1 - 9 (79 Test Cases) against live environment

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   TourHive Backend Complete Master E2E Test Suite        " -ForegroundColor Cyan
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
        if ($_.Exception.Response) {
            $statusCode = $_.Exception.Response.StatusCode.value__
            $respStream = $_.Exception.Response.GetResponseStream()
            if ($respStream) {
                $reader = New-Object System.IO.StreamReader($respStream)
                $respBody = $reader.ReadToEnd()
            }
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
        $msgs = Invoke-RestMethod -Uri "http://localhost:8025/api/v1/messages" -Method Get -UseBasicParsing
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
        $msgs = Invoke-RestMethod -Uri "http://localhost:8025/api/v1/messages" -Method Get -UseBasicParsing
        $userMsg = $msgs.messages | Where-Object { 
            $addrs = $_.To | ForEach-Object { $_.Address }
            ($addrs -contains $email) -and ($_.Subject -like "*Password Reset*")
        } | Select-Object -First 1
        if ($userMsg) {
            $msgDetail = Invoke-RestMethod -Uri "http://localhost:8025/api/v1/message/$($userMsg.ID)" -Method Get -UseBasicParsing
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

# =============================================================
# GROUP 1: SYSTEM ENDPOINTS & OPENAPI DOCS
# =============================================================
Write-Host "`n--- Group 1: System Endpoints ---" -ForegroundColor Yellow

$h = Call-Api "GET" "http://localhost:8080/actuator/health"
if ($h.StatusCode -eq 200 -and $h.Data.status -eq "UP") {
    Log-Result "TC-SYS-01" "Health Check /actuator/health" "PASS" "Status UP (DB & disk healthy)"
} else {
    Log-Result "TC-SYS-01" "Health Check /actuator/health" "FAIL" "Status $($h.StatusCode)"
}

$docs = Call-Api "GET" "http://localhost:8080/v3/api-docs"
if ($docs.StatusCode -eq 200 -and $docs.Raw -match "TourHive") {
    Log-Result "TC-SYS-02" "OpenAPI Docs /v3/api-docs" "PASS" "OpenAPI specification generated successfully"
} else {
    Log-Result "TC-SYS-02" "OpenAPI Docs /v3/api-docs" "FAIL" "Status $($docs.StatusCode)"
}

# =============================================================
# GROUP 2: AUTHENTICATION & EMAIL VERIFICATION
# =============================================================
Write-Host "`n--- Group 2: Registration & Verification ---" -ForegroundColor Yellow

$userAEmail = "e2e_user_a_$ts@tourhive.com"
$pwdA = "SecureP@ssw0rd123"

$r1 = Call-Api "POST" "http://localhost:8080/api/v1/auth/register" @{ email = $userAEmail; password = $pwdA }
if ($r1.StatusCode -eq 201) {
    Log-Result "TC-AUTH-01" "POST /api/v1/auth/register (User A)" "PASS" "User registered: $userAEmail"
} else {
    Log-Result "TC-AUTH-01" "POST /api/v1/auth/register (User A)" "FAIL" "Status $($r1.StatusCode): $($r1.Raw)"
}

$otpA = Get-MailpitOtp $userAEmail
if ($otpA) {
    Log-Result "TC-AUTH-02" "Fetch OTP from Mailpit" "PASS" "OTP: $otpA"
} else {
    Log-Result "TC-AUTH-02" "Fetch OTP from Mailpit" "FAIL" "OTP not found in Mailpit"
}

$r2 = Call-Api "POST" "http://localhost:8080/api/v1/auth/verify-email" @{ email = $userAEmail; code = $otpA }
if ($r2.StatusCode -eq 200) {
    Log-Result "TC-AUTH-03" "POST /api/v1/auth/verify-email" "PASS" "Email verified successfully"
} else {
    Log-Result "TC-AUTH-03" "POST /api/v1/auth/verify-email" "FAIL" "Status $($r2.StatusCode): $($r2.Raw)"
}

$r3 = Call-Api "POST" "http://localhost:8080/api/v1/auth/login" @{ email = $userAEmail; password = $pwdA }
$tokenA = $r3.Data.data.accessToken
$refreshA = $r3.Data.data.refreshToken
if ($r3.StatusCode -eq 200 -and $tokenA -and $refreshA) {
    Log-Result "TC-AUTH-04" "POST /api/v1/auth/login" "PASS" "Tokens issued successfully"
} else {
    Log-Result "TC-AUTH-04" "POST /api/v1/auth/login" "FAIL" "Status $($r3.StatusCode)"
}

$r4 = Call-Api "GET" "http://localhost:8080/api/v1/auth/me" $null $tokenA
$userAId = $r4.Data.data.id
if ($r4.StatusCode -eq 200 -and $r4.Data.data.email -eq $userAEmail -and $userAId) {
    Log-Result "TC-AUTH-05" "GET /api/v1/auth/me" "PASS" "Validated token for: $($r4.Data.data.email), userId: $userAId"
} else {
    Log-Result "TC-AUTH-05" "GET /api/v1/auth/me" "FAIL" "Status $($r4.StatusCode)"
}

$r5 = Call-Api "POST" "http://localhost:8080/api/v1/auth/refresh" @{ refreshToken = $refreshA }
$newAccessTokenA = $r5.Data.data.accessToken
$newRefreshTokenA = $r5.Data.data.refreshToken
if ($r5.StatusCode -eq 200 -and $newAccessTokenA) {
    Log-Result "TC-AUTH-06" "POST /api/v1/auth/refresh" "PASS" "Refreshed tokens successfully"
    $tokenA = $newAccessTokenA
    $refreshA = $newRefreshTokenA
} else {
    Log-Result "TC-AUTH-06" "POST /api/v1/auth/refresh" "FAIL" "Status $($r5.StatusCode)"
}

# =============================================================
# GROUP 3: FORGOT PASSWORD, RESET, LOGOUT & TOKEN REVOCATION
# =============================================================
Write-Host "`n--- Group 3: Password Reset & Security ---" -ForegroundColor Yellow

$r6 = Call-Api "POST" "http://localhost:8080/api/v1/auth/forgot-password" @{ email = $userAEmail }
$resetToken = Get-MailpitResetToken $userAEmail
if ($r6.StatusCode -eq 200 -and $resetToken) {
    Log-Result "TC-AUTH-07" "POST /api/v1/auth/forgot-password" "PASS" "Reset token received: $resetToken"
} else {
    Log-Result "TC-AUTH-07" "POST /api/v1/auth/forgot-password" "FAIL" "Status $($r6.StatusCode)"
}

$newPwdA = "NewSecureP@ssw0rd456"
$r7 = Call-Api "POST" "http://localhost:8080/api/v1/auth/reset-password" @{ token = $resetToken; newPassword = $newPwdA }
$r7Login = Call-Api "POST" "http://localhost:8080/api/v1/auth/login" @{ email = $userAEmail; password = $newPwdA }
if ($r7.StatusCode -eq 200 -and $r7Login.StatusCode -eq 200) {
    Log-Result "TC-AUTH-08" "POST /api/v1/auth/reset-password & Login" "PASS" "Password updated and verified"
    $tokenA = $r7Login.Data.data.accessToken
    $refreshA = $r7Login.Data.data.refreshToken
} else {
    Log-Result "TC-AUTH-08" "POST /api/v1/auth/reset-password & Login" "FAIL" "Status $($r7.StatusCode)"
}

$tempEmail = "e2e_temp_$ts@tourhive.com"
Call-Api "POST" "http://localhost:8080/api/v1/auth/register" @{ email = $tempEmail; password = $pwdA } | Out-Null
$otpTemp = Get-MailpitOtp $tempEmail
Call-Api "POST" "http://localhost:8080/api/v1/auth/verify-email" @{ email = $tempEmail; code = $otpTemp } | Out-Null
$rTempLogin = Call-Api "POST" "http://localhost:8080/api/v1/auth/login" @{ email = $tempEmail; password = $pwdA }
$tempRefresh = $rTempLogin.Data.data.refreshToken
$tempAccess = $rTempLogin.Data.data.accessToken

$r8 = Call-Api "POST" "http://localhost:8080/api/v1/auth/logout" @{ refreshToken = $tempRefresh } $tempAccess
if ($r8.StatusCode -eq 200 -or $r8.StatusCode -eq 204) {
    Log-Result "TC-AUTH-09" "POST /api/v1/auth/logout" "PASS" "Logged out"
} else {
    Log-Result "TC-AUTH-09" "POST /api/v1/auth/logout" "FAIL" "Status $($r8.StatusCode)"
}

$r9 = Call-Api "POST" "http://localhost:8080/api/v1/auth/refresh" @{ refreshToken = $tempRefresh }
if ($r9.StatusCode -eq 401 -and $r9.Data.code -eq "INVALID_TOKEN") {
    Log-Result "TC-AUTH-10" "Revoked Token Refresh (401)" "PASS" "Correctly rejected: $($r9.Data.code)"
} else {
    Log-Result "TC-AUTH-10" "Revoked Token Refresh (401)" "FAIL" "Status $($r9.StatusCode)"
}

# =============================================================
# GROUP 4: USER PROFILES & AVATARS
# =============================================================
Write-Host "`n--- Group 4: Profiles & Avatars ---" -ForegroundColor Yellow

$u1 = Call-Api "GET" "http://localhost:8080/api/v1/users/me" $null $tokenA
if ($u1.StatusCode -eq 200 -and $u1.Data.data.userId -eq $userAId) {
    Log-Result "TC-USER-01" "GET /api/v1/users/me" "PASS" "Retrieved profile for User A"
} else {
    Log-Result "TC-USER-01" "GET /api/v1/users/me" "FAIL" "Status $($u1.StatusCode)"
}

$u2Req = @{
    nickname = "nomad_guide_$ts"
    firstName = "Almas"
    lastName = "Kassymov"
    bio = "Certified Tien Shan trekker"
    city = "Almaty"
    country = "Kazakhstan"
}
$u2 = Call-Api "PATCH" "http://localhost:8080/api/v1/users/me" $u2Req $tokenA
if ($u2.StatusCode -eq 200 -and $u2.Data.data.nickname -eq "nomad_guide_$ts") {
    Log-Result "TC-USER-02" "PATCH /api/v1/users/me" "PASS" "Profile updated with nickname nomad_guide_$ts"
} else {
    Log-Result "TC-USER-02" "PATCH /api/v1/users/me" "FAIL" "Status $($u2.StatusCode)"
}

$u3 = Call-Api "POST" "http://localhost:8080/api/v1/users/me/avatar" @{ avatarUrl = "https://cdn.tourhive.com/avatars/almas.jpg" } $tokenA
if ($u3.StatusCode -eq 200 -and $u3.Data.data.avatarUrl) {
    Log-Result "TC-USER-03" "POST /api/v1/users/me/avatar" "PASS" "Avatar updated: $($u3.Data.data.avatarUrl)"
} else {
    Log-Result "TC-USER-03" "POST /api/v1/users/me/avatar" "FAIL" "Status $($u3.StatusCode)"
}

$u4 = Call-Api "GET" "http://localhost:8080/api/v1/users/$userAId"
if ($u4.StatusCode -eq 200 -and $u4.Data.data.nickname -eq "nomad_guide_$ts") {
    Log-Result "TC-USER-04" "GET /api/v1/users/{id} (Public)" "PASS" "Public profile contains nickname"
} else {
    Log-Result "TC-USER-04" "GET /api/v1/users/{id} (Public)" "FAIL" "Status $($u4.StatusCode)"
}

# User C to test conflicts and blocks
$userCEmail = "e2e_user_c_$ts@tourhive.com"
Call-Api "POST" "http://localhost:8080/api/v1/auth/register" @{ email = $userCEmail; password = $pwdA } | Out-Null
$otpC = Get-MailpitOtp $userCEmail
Call-Api "POST" "http://localhost:8080/api/v1/auth/verify-email" @{ email = $userCEmail; code = $otpC } | Out-Null
$rLoginC = Call-Api "POST" "http://localhost:8080/api/v1/auth/login" @{ email = $userCEmail; password = $pwdA }
$tokenC = $rLoginC.Data.data.accessToken
$rMeC = Call-Api "GET" "http://localhost:8080/api/v1/users/me" $null $tokenC
$userCId = $rMeC.Data.data.userId

$u5 = Call-Api "PATCH" "http://localhost:8080/api/v1/users/me" @{ nickname = "nomad_guide_$ts" } $tokenC
if ($u5.StatusCode -eq 409 -and $u5.Data.code -eq "NICKNAME_ALREADY_TAKEN") {
    Log-Result "TC-USER-05" "Duplicate Nickname Conflict (409)" "PASS" "Correctly caught: $($u5.Data.code)"
} else {
    Log-Result "TC-USER-05" "Duplicate Nickname Conflict (409)" "FAIL" "Status $($u5.StatusCode)"
}

# =============================================================
# GROUP 5: BLACKLIST / BLOCK
# =============================================================
Write-Host "`n--- Group 5: Blacklist & Block ---" -ForegroundColor Yellow

$b1 = Call-Api "POST" "http://localhost:8080/api/v1/users/$userCId/block" $null $tokenA
if ($b1.StatusCode -eq 200 -and $b1.Data.success) {
    Log-Result "TC-BLK-01" "POST /api/v1/users/{id}/block" "PASS" "User A blocked User C"
} else {
    Log-Result "TC-BLK-01" "POST /api/v1/users/{id}/block" "FAIL" "Status $($b1.StatusCode)"
}

$b2 = Call-Api "GET" "http://localhost:8080/api/v1/users/me/blocks" $null $tokenA
$isBlockedFound = ($b2.Data.data | Where-Object { $_.blockedUserId -eq $userCId }) -ne $null
if ($b2.StatusCode -eq 200 -and $isBlockedFound) {
    Log-Result "TC-BLK-02" "GET /api/v1/users/me/blocks" "PASS" "User C in User A block list"
} else {
    Log-Result "TC-BLK-02" "GET /api/v1/users/me/blocks" "FAIL" "Status $($b2.StatusCode)"
}

$b3 = Call-Api "POST" "http://localhost:8080/api/v1/users/$userAId/block" $null $tokenA
if ($b3.StatusCode -eq 400 -and $b3.Data.code -eq "CANNOT_BLOCK_SELF") {
    Log-Result "TC-BLK-03" "Self-block prevention (400)" "PASS" "Correctly caught: $($b3.Data.code)"
} else {
    Log-Result "TC-BLK-03" "Self-block prevention (400)" "FAIL" "Status $($b3.StatusCode)"
}

$b4 = Call-Api "DELETE" "http://localhost:8080/api/v1/users/$userCId/block" $null $tokenA
if ($b4.StatusCode -eq 204 -or $b4.StatusCode -eq 200) {
    Log-Result "TC-BLK-04" "DELETE /api/v1/users/{id}/block" "PASS" "User C unblocked"
} else {
    Log-Result "TC-BLK-04" "DELETE /api/v1/users/{id}/block" "FAIL" "Status $($b4.StatusCode)"
}

# =============================================================
# GROUP 6: MINIO STORAGE & RESOURCE STREAMING
# =============================================================
Write-Host "`n--- Group 6: MinIO Resources & Media ---" -ForegroundColor Yellow

$testFile = [System.IO.Path]::GetTempFileName() + ".txt"
Set-Content -Path $testFile -Value "TourHive Resource Storage Test Content $ts"
$uploadResp = Upload-File "http://localhost:8080/api/v1/resources/upload" $testFile $tokenA
Remove-Item $testFile -ErrorAction SilentlyContinue
$resourceId = $uploadResp.data.id

if ($uploadResp.success -and $resourceId) {
    Log-Result "TC-RES-01" "POST /api/v1/resources/upload" "PASS" "Uploaded to MinIO, Resource ID: $resourceId"
} else {
    Log-Result "TC-RES-01" "POST /api/v1/resources/upload" "FAIL" "Upload failed: $($uploadResp | ConvertTo-Json -Compress)"
}

$streamResp = Call-Api "GET" "http://localhost:8080/api/v1/resources/$resourceId"
if ($streamResp.StatusCode -eq 200 -and $streamResp.Raw.Contains("TourHive Resource Storage Test Content")) {
    Log-Result "TC-RES-02" "GET /api/v1/resources/{id} (Stream)" "PASS" "Successfully streamed binary from MinIO"
} else {
    Log-Result "TC-RES-02" "GET /api/v1/resources/{id} (Stream)" "FAIL" "Status $($streamResp.StatusCode)"
}

$mediaFile = [System.IO.Path]::GetTempFileName() + ".jpg"
[System.IO.File]::WriteAllBytes($mediaFile, [byte[]]@(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46))
$mediaResp = Upload-File "http://localhost:8080/api/v1/media/upload" $mediaFile $tokenA
Remove-Item $mediaFile -ErrorAction SilentlyContinue
if ($mediaResp.success -and $mediaResp.data.fileUrl) {
    Log-Result "TC-RES-03" "POST /api/v1/media/upload" "PASS" "Media URL returned: $($mediaResp.data.fileUrl)"
} else {
    Log-Result "TC-RES-03" "POST /api/v1/media/upload" "FAIL" "Failed: $($mediaResp | ConvertTo-Json -Compress)"
}

$fakeResId = [System.Guid]::NewGuid().ToString()
$fakeResResp = Call-Api "GET" "http://localhost:8080/api/v1/resources/$fakeResId"
if ($fakeResResp.StatusCode -eq 404 -and $fakeResResp.Data.code -eq "RESOURCE_NOT_FOUND") {
    Log-Result "TC-RES-04" "Non-existent Resource (404)" "PASS" "Correctly caught: $($fakeResResp.Data.code)"
} else {
    Log-Result "TC-RES-04" "Non-existent Resource (404)" "FAIL" "Status $($fakeResResp.StatusCode)"
}

# =============================================================
# GROUP 7: ORGANIZER PROFILES & BUSINESS VERIFICATION
# =============================================================
Write-Host "`n--- Group 7: Organizer & Business Verification ---" -ForegroundColor Yellow

$o1 = Call-Api "POST" "http://localhost:8080/api/v1/organizers/me/activate" $null $tokenA
if ($o1.StatusCode -eq 200 -and $o1.Data.data.isOrganizer -eq $true) {
    Log-Result "TC-ORG-01" "POST /api/v1/organizers/me/activate (User A)" "PASS" "Activated organizer role"
} else {
    Log-Result "TC-ORG-01" "POST /api/v1/organizers/me/activate (User A)" "FAIL" "Status $($o1.StatusCode)"
}

$o2 = Call-Api "GET" "http://localhost:8080/api/v1/organizers/me" $null $tokenA
if ($o2.StatusCode -eq 200 -and $o2.Data.data.isOrganizer -eq $true) {
    Log-Result "TC-ORG-02" "GET /api/v1/organizers/me" "PASS" "Retrieved organizer profile"
} else {
    Log-Result "TC-ORG-02" "GET /api/v1/organizers/me" "FAIL" "Status $($o2.StatusCode)"
}

$o3 = Call-Api "GET" "http://localhost:8080/api/v1/organizers/$userAId"
if ($o3.StatusCode -eq 200 -and $o3.Data.data.isOrganizer -eq $true) {
    Log-Result "TC-ORG-03" "GET /api/v1/organizers/{userId} (Public)" "PASS" "Public organizer info retrieved"
} else {
    Log-Result "TC-ORG-03" "GET /api/v1/organizers/{userId} (Public)" "FAIL" "Status $($o3.StatusCode)"
}

# User B for Business Verification
$userBEmail = "e2e_biz_org_$ts@tourhive.com"
Call-Api "POST" "http://localhost:8080/api/v1/auth/register" @{ email = $userBEmail; password = $pwdA } | Out-Null
$otpB = Get-MailpitOtp $userBEmail
Call-Api "POST" "http://localhost:8080/api/v1/auth/verify-email" @{ email = $userBEmail; code = $otpB } | Out-Null
$rLoginB = Call-Api "POST" "http://localhost:8080/api/v1/auth/login" @{ email = $userBEmail; password = $pwdA }
$tokenB = $rLoginB.Data.data.accessToken
$rMeB = Call-Api "GET" "http://localhost:8080/api/v1/users/me" $null $tokenB
$userBId = $rMeB.Data.data.userId

$o4 = Call-Api "POST" "http://localhost:8080/api/v1/organizers/me/activate" $null $tokenB
if ($o4.StatusCode -eq 200 -and $o4.Data.data.isOrganizer -eq $true) {
    Log-Result "TC-ORG-04" "Activate Organizer User B" "PASS" "Activated"
} else {
    Log-Result "TC-ORG-04" "Activate Organizer User B" "FAIL" "Status $($o4.StatusCode)"
}

$certFile = [System.IO.Path]::GetTempFileName() + ".pdf"
[System.IO.File]::WriteAllBytes($certFile, [byte[]]@(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x35))
$certUpload = Upload-File "http://localhost:8080/api/v1/resources/upload" $certFile $tokenB
Remove-Item $certFile -ErrorAction SilentlyContinue
$certResourceId = $certUpload.data.id

if ($certUpload.success -and $certResourceId) {
    Log-Result "TC-ORG-05" "Upload Business Certificate PDF" "PASS" "Resource ID: $certResourceId"
} else {
    Log-Result "TC-ORG-05" "Upload Business Certificate PDF" "FAIL" "Upload failed"
}

$bizReq = @{
    legalName = "Nomad Trails LLC"
    registrationNumber = "BIN123456789012"
    country = "Kazakhstan"
    contactEmail = $userBEmail
    contactPhone = "+77015554433"
    description = "Official travel and hiking agency in Kazakhstan"
    documents = @(
        @{
            documentName = "Registration Certificate"
            documentUrl = "/api/v1/resources/$certResourceId"
        }
    )
}
$o6 = Call-Api "POST" "http://localhost:8080/api/v1/organizers/me/business" $bizReq $tokenB
if ($o6.StatusCode -eq 201 -and $o6.Data.data.legalName -eq "Nomad Trails LLC") {
    Log-Result "TC-ORG-06" "POST /api/v1/organizers/me/business" "PASS" "Business verification submitted"
} else {
    Log-Result "TC-ORG-06" "POST /api/v1/organizers/me/business" "FAIL" "Status $($o6.StatusCode): $($o6.Raw)"
}

$o7 = Call-Api "GET" "http://localhost:8080/api/v1/organizers/me/business" $null $tokenB
if ($o7.StatusCode -eq 200 -and $o7.Data.data.status -eq "PENDING") {
    Log-Result "TC-ORG-07" "GET /api/v1/organizers/me/business" "PASS" "Status PENDING"
} else {
    Log-Result "TC-ORG-07" "GET /api/v1/organizers/me/business" "FAIL" "Status $($o7.StatusCode)"
}

# Moderator Admin Setup
$adminEmail = "e2e_admin_$ts@tourhive.com"
Call-Api "POST" "http://localhost:8080/api/v1/auth/register" @{ email = $adminEmail; password = $pwdA } | Out-Null
$otpAdmin = Get-MailpitOtp $adminEmail
Call-Api "POST" "http://localhost:8080/api/v1/auth/verify-email" @{ email = $adminEmail; code = $otpAdmin } | Out-Null
& docker exec tourhive-postgres psql -U tourhive_user -d tourhive_db -c "UPDATE users SET role = 'MODERATOR' WHERE email = '$adminEmail';" | Out-Null
$rLoginAdmin = Call-Api "POST" "http://localhost:8080/api/v1/auth/login" @{ email = $adminEmail; password = $pwdA }
$tokenAdmin = $rLoginAdmin.Data.data.accessToken

$o8 = Call-Api "GET" "http://localhost:8080/api/v1/admin/business-verifications?page=0&size=10" $null $tokenAdmin
$pendingBiz = $o8.Data.data.content | Where-Object { $_.contactEmail -eq $userBEmail } | Select-Object -First 1
$bizProfileId = $pendingBiz.id
if ($o8.StatusCode -eq 200 -and $bizProfileId) {
    Log-Result "TC-ORG-08" "GET /api/v1/admin/business-verifications" "PASS" "Verification request found by moderator, id: $bizProfileId"
} else {
    Log-Result "TC-ORG-08" "GET /api/v1/admin/business-verifications" "FAIL" "Status $($o8.StatusCode)"
}

$o9 = Call-Api "GET" "http://localhost:8080/api/v1/admin/business-verifications/$bizProfileId" $null $tokenAdmin
if ($o9.StatusCode -eq 200 -and $o9.Data.data.legalName -eq "Nomad Trails LLC") {
    Log-Result "TC-ORG-09" "GET /api/v1/admin/business-verifications/{id}" "PASS" "Detailed verification info retrieved"
} else {
    Log-Result "TC-ORG-09" "GET /api/v1/admin/business-verifications/{id}" "FAIL" "Status $($o9.StatusCode)"
}

$o10 = Call-Api "POST" "http://localhost:8080/api/v1/admin/business-verifications/$bizProfileId/review" @{ status = "VERIFIED" } $tokenAdmin
if ($o10.StatusCode -eq 200 -and $o10.Data.data.status -eq "VERIFIED") {
    Log-Result "TC-ORG-10" "POST .../business-verifications/{id}/review" "PASS" "Approved to VERIFIED"
} else {
    Log-Result "TC-ORG-10" "POST .../business-verifications/{id}/review" "FAIL" "Status $($o10.StatusCode)"
}

$uCheckB = Call-Api "GET" "http://localhost:8080/api/v1/users/me" $null $tokenB
if ($uCheckB.StatusCode -eq 200 -and $uCheckB.Data.data.isBusinessVerified -eq $true) {
    Log-Result "TC-ORG-11" "Verify isBusinessVerified is true" "PASS" "User B is now a verified business"
} else {
    Log-Result "TC-ORG-11" "Verify isBusinessVerified is true" "FAIL" "isBusinessVerified: $($uCheckB.Data.data.isBusinessVerified)"
}

# =============================================================
# GROUP 8: TOURS, DRAFTS, PUBLISHING & DYNAMIC SEARCH
# =============================================================
Write-Host "`n--- Group 8: Tours, Drafts, Publishing & Search ---" -ForegroundColor Yellow

$commFailReq = @{
    title = "Commercial Tour By Non Business"
    tourType = "COMMERCIAL"
    startDate = "2026-12-01"
    endDate = "2026-12-05"
    capacity = 10
}
$t1 = Call-Api "POST" "http://localhost:8080/api/v1/tours/draft" $commFailReq $tokenA
if ($t1.StatusCode -eq 403 -and $t1.Data.code -eq "COMMERCIAL_TOUR_NOT_ALLOWED") {
    Log-Result "TC-TOUR-01" "Commercial Tour by Non-Business (403)" "PASS" "Correctly rejected: $($t1.Data.code)"
} else {
    Log-Result "TC-TOUR-01" "Commercial Tour by Non-Business (403)" "FAIL" "Status $($t1.StatusCode)"
}

$draftAReq = @{
    title = "Tien Shan Wilderness Hike $ts"
    description = "Authentic hiking across alpine meadows and passes"
    tourType = "COMMUNITY"
    startDate = "2026-12-10"
    endDate = "2026-12-14"
    capacity = 12
    price = 30000
    currency = "KZT"
    countries = @("Kazakhstan")
    cities = @("Almaty", "Kolsay")
    includes = @("Tents", "Gas burners", "Food supplies")
    route = @(
        @{ city = "Almaty"; routeDate = "2026-12-10"; description = "Departure"; orderIndex = 0 },
        @{ city = "Kolsay"; routeDate = "2026-12-11"; description = "Lower Lake Camp"; orderIndex = 1 }
    )
}
$t2 = Call-Api "POST" "http://localhost:8080/api/v1/tours/draft" $draftAReq $tokenA
$tourAId = $t2.Data.data.id
if ($t2.StatusCode -eq 201 -and $tourAId) {
    Log-Result "TC-TOUR-02" "Create Community Tour Draft" "PASS" "Draft id: $tourAId"
} else {
    Log-Result "TC-TOUR-02" "Create Community Tour Draft" "FAIL" "Status $($t2.StatusCode)"
}

$tourImgFile = [System.IO.Path]::GetTempFileName() + ".jpg"
[System.IO.File]::WriteAllBytes($tourImgFile, [byte[]]@(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46))
$tourImgUpload = Upload-File "http://localhost:8080/api/v1/resources/upload" $tourImgFile $tokenA
Remove-Item $tourImgFile -ErrorAction SilentlyContinue
$tourImgResId = $tourImgUpload.data.id
if ($tourImgUpload.success -and $tourImgResId) {
    Log-Result "TC-TOUR-03" "Upload Tour Image Resource" "PASS" "Resource ID: $tourImgResId"
} else {
    Log-Result "TC-TOUR-03" "Upload Tour Image Resource" "FAIL" "Upload failed"
}

$t4 = Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourAId/images" @{ resourceId = $tourImgResId; isCover = $true; orderIndex = 0 } $tokenA
$tourImageId = $t4.Data.data.id
if ($t4.StatusCode -eq 201 -and $tourImageId -and $t4.Data.data.isCover -eq $true) {
    Log-Result "TC-TOUR-04" "POST /api/v1/tours/{id}/images" "PASS" "Image attached as cover"
} else {
    Log-Result "TC-TOUR-04" "POST /api/v1/tours/{id}/images" "FAIL" "Status $($t4.StatusCode)"
}

$t5Req = @{
    title = "Tien Shan Wilderness Hike $ts - Updated"
    capacity = 15
    route = @(
        @{ city = "Almaty"; routeDate = "2026-12-10"; description = "Departure"; orderIndex = 0 },
        @{ city = "Kolsay"; routeDate = "2026-12-11"; description = "Lower Lake Camp"; orderIndex = 1 },
        @{ city = "Kaindy"; routeDate = "2026-12-12"; description = "Submerged Forest Trek"; orderIndex = 2 }
    )
}
$t5 = Call-Api "PUT" "http://localhost:8080/api/v1/tours/$tourAId" $t5Req $tokenA
if ($t5.StatusCode -eq 200 -and $t5.Data.data.capacity -eq 15 -and $t5.Data.data.route.Count -eq 3) {
    Log-Result "TC-TOUR-05" "PUT /api/v1/tours/{id}" "PASS" "Draft updated: capacity=15, route stops=3"
} else {
    Log-Result "TC-TOUR-05" "PUT /api/v1/tours/{id}" "FAIL" "Status $($t5.StatusCode)"
}

$t6 = Call-Api "GET" "http://localhost:8080/api/v1/tours/my-drafts" $null $tokenA
$foundInDrafts = ($t6.Data.data | Where-Object { $_.id -eq $tourAId }) -ne $null
if ($t6.StatusCode -eq 200 -and $foundInDrafts) {
    Log-Result "TC-TOUR-06" "GET /api/v1/tours/my-drafts" "PASS" "Tour found in organizer drafts list"
} else {
    Log-Result "TC-TOUR-06" "GET /api/v1/tours/my-drafts" "FAIL" "Status $($t6.StatusCode)"
}

$t7 = Call-Api "GET" "http://localhost:8080/api/v1/tours/$tourAId" $null $tokenA
if ($t7.StatusCode -eq 200 -and $t7.Data.data.id -eq $tourAId -and $t7.Data.data.coverImageUrl) {
    Log-Result "TC-TOUR-07" "GET /api/v1/tours/{id} (Owner)" "PASS" "Owner retrieved tour details with cover image"
} else {
    Log-Result "TC-TOUR-07" "GET /api/v1/tours/{id} (Owner)" "FAIL" "Status $($t7.StatusCode)"
}

$t8 = Call-Api "GET" "http://localhost:8080/api/v1/tours/$tourAId" $null $tokenC
if ($t8.StatusCode -eq 403 -and $t8.Data.code -eq "FORBIDDEN_TOUR_ACCESS") {
    Log-Result "TC-TOUR-08" "Stranger Access to Draft (403)" "PASS" "Forbidden with 403 FORBIDDEN_TOUR_ACCESS"
} else {
    Log-Result "TC-TOUR-08" "Stranger Access to Draft (403)" "FAIL" "Status $($t8.StatusCode)"
}

$t9Req = @{
    title = "Official Charyn Canyon Commercial Excursion $ts"
    description = "Comfortable tourist bus and canyon tour"
    tourType = "COMMERCIAL"
    startDate = "2026-11-20"
    endDate = "2026-11-22"
    capacity = 25
    price = 45000
    currency = "KZT"
    countries = @("Kazakhstan")
    cities = @("Almaty", "Charyn")
    includes = @("Premium Bus", "Guide", "Lunch")
}
$t9 = Call-Api "POST" "http://localhost:8080/api/v1/tours/draft" $t9Req $tokenB
$tourBId = $t9.Data.data.id
if ($t9.StatusCode -eq 201 -and $tourBId -and $t9.Data.data.tourType -eq "COMMERCIAL") {
    Log-Result "TC-TOUR-09" "Commercial Tour by Verified Business" "PASS" "Created commercial tour id: $tourBId"
} else {
    Log-Result "TC-TOUR-09" "Commercial Tour by Verified Business" "FAIL" "Status $($t9.StatusCode)"
}

# Delete extra photo & test draft deletion
$extraImgFile = [System.IO.Path]::GetTempFileName() + ".jpg"
[System.IO.File]::WriteAllBytes($extraImgFile, [byte[]]@(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46))
$extraImgUpload = Upload-File "http://localhost:8080/api/v1/resources/upload" $extraImgFile $tokenA
Remove-Item $extraImgFile -ErrorAction SilentlyContinue
$extraImgAttach = Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourAId/images" @{ resourceId = $extraImgUpload.data.id; isCover = $false; orderIndex = 1 } $tokenA
$extraImageId = $extraImgAttach.Data.data.id

$t10 = Call-Api "DELETE" "http://localhost:8080/api/v1/tours/$tourAId/images/$extraImageId" $null $tokenA
if ($t10.StatusCode -eq 204 -or $t10.StatusCode -eq 200) {
    Log-Result "TC-TOUR-10" "DELETE /api/v1/tours/{id}/images/{imageId}" "PASS" "Tour image deleted"
} else {
    Log-Result "TC-TOUR-10" "DELETE /api/v1/tours/{id}/images/{imageId}" "FAIL" "Status: $($t10.StatusCode)"
}

# Draft deletion test using temporary tour
$tempDraftReq = @{
    title = "Draft To Delete $ts"
    tourType = "COMMUNITY"
    startDate = "2026-12-01"
    endDate = "2026-12-02"
    capacity = 5
}
$rTempDraft = Call-Api "POST" "http://localhost:8080/api/v1/tours/draft" $tempDraftReq $tokenA
$tempDraftId = $rTempDraft.Data.data.id
$t11 = Call-Api "DELETE" "http://localhost:8080/api/v1/tours/$tempDraftId" $null $tokenA
$t11check = Call-Api "GET" "http://localhost:8080/api/v1/tours/$tempDraftId" $null $tokenA
if (($t11.StatusCode -eq 204 -or $t11.StatusCode -eq 200) -and $t11check.StatusCode -eq 404) {
    Log-Result "TC-TOUR-11" "DELETE /api/v1/tours/{id}" "PASS" "Draft tour deleted and returns 404"
} else {
    Log-Result "TC-TOUR-11" "DELETE /api/v1/tours/{id}" "FAIL" "Status: $($t11.StatusCode)"
}

# Publishing lifecycle on Tour with search & filters
$searchTourReq = @{
    title = "Ili River Valley Rafting Expedition $ts"
    description = "Exciting 4-day rafting and camping journey along Ili River"
    tourType = "COMMUNITY"
    startDate = "2026-11-15"
    endDate = "2026-11-19"
    capacity = 14
    price = 35000
    currency = "KZT"
    countries = @("Kazakhstan")
    cities = @("Almaty", "Kapchagay")
    includes = @("Rafts", "Lifejackets", "Meals")
    route = @(
        @{ city = "Almaty"; routeDate = "2026-11-15"; description = "Departure"; orderIndex = 0 },
        @{ city = "Kapchagay"; routeDate = "2026-11-19"; description = "Finish"; orderIndex = 1 }
    )
}
$rSearchTour = Call-Api "POST" "http://localhost:8080/api/v1/tours/draft" $searchTourReq $tokenA
$searchTourId = $rSearchTour.Data.data.id

$p1 = Call-Api "POST" "http://localhost:8080/api/v1/tours/$searchTourId/publish" $null $tokenA
if ($p1.StatusCode -eq 400) {
    Log-Result "TC-PUB-01" "Publish without photos (400)" "PASS" "Correctly rejected: $($p1.Data.message)"
} else {
    Log-Result "TC-PUB-01" "Publish without photos (400)" "FAIL" "Status $($p1.StatusCode)"
}

# Attach photo and submit
$searchPhotoFile = [System.IO.Path]::GetTempFileName() + ".jpg"
[System.IO.File]::WriteAllBytes($searchPhotoFile, [byte[]]@(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46))
$searchPhotoUpload = Upload-File "http://localhost:8080/api/v1/resources/upload" $searchPhotoFile $tokenA
Remove-Item $searchPhotoFile -ErrorAction SilentlyContinue
Call-Api "POST" "http://localhost:8080/api/v1/tours/$searchTourId/images" @{ resourceId = $searchPhotoUpload.data.id; isCover = $true; orderIndex = 0 } $tokenA | Out-Null

$p2 = Call-Api "POST" "http://localhost:8080/api/v1/tours/$searchTourId/publish" $null $tokenA
if ($p2.StatusCode -eq 200 -and $p2.Data.data.status -eq "PENDING_REVIEW") {
    Log-Result "TC-PUB-02" "POST /api/v1/tours/{id}/publish" "PASS" "Tour submitted to PENDING_REVIEW"
} else {
    Log-Result "TC-PUB-02" "POST /api/v1/tours/{id}/publish" "FAIL" "Status $($p2.StatusCode)"
}

$p3 = Call-Api "GET" "http://localhost:8080/api/v1/admin/tours/pending" $null $tokenAdmin
$foundPending = ($p3.Data.data.content | Where-Object { $_.id -eq $searchTourId }) -ne $null
if ($p3.StatusCode -eq 200 -and $foundPending) {
    Log-Result "TC-PUB-03" "GET /api/v1/admin/tours/pending" "PASS" "Tour in moderator review queue"
} else {
    Log-Result "TC-PUB-03" "GET /api/v1/admin/tours/pending" "FAIL" "Status $($p3.StatusCode)"
}

$p4 = Call-Api "POST" "http://localhost:8080/api/v1/admin/tours/$searchTourId/review" @{ status = "REJECTED"; rejectionReason = "Description needs safety gear details" } $tokenAdmin
if ($p4.StatusCode -eq 200 -and $p4.Data.data.status -eq "REJECTED" -and $p4.Data.data.rejectionReason -eq "Description needs safety gear details") {
    Log-Result "TC-PUB-04" "POST .../admin/tours/{id}/review (REJECTED)" "PASS" "Tour rejected with reason"
} else {
    Log-Result "TC-PUB-04" "POST .../admin/tours/{id}/review (REJECTED)" "FAIL" "Status $($p4.StatusCode)"
}

# Update and Approve
Call-Api "PUT" "http://localhost:8080/api/v1/tours/$searchTourId" @{ description = "Updated: full certified lifejackets and guide included." } $tokenA | Out-Null
Call-Api "POST" "http://localhost:8080/api/v1/tours/$searchTourId/publish" $null $tokenA | Out-Null
$p5 = Call-Api "POST" "http://localhost:8080/api/v1/admin/tours/$searchTourId/review" @{ status = "PUBLISHED" } $tokenAdmin
if ($p5.StatusCode -eq 200 -and $p5.Data.data.status -eq "PUBLISHED" -and $p5.Data.data.publishedAt) {
    Log-Result "TC-PUB-05" "POST .../admin/tours/{id}/review (PUBLISHED)" "PASS" "Tour approved and published"
} else {
    Log-Result "TC-PUB-05" "POST .../admin/tours/{id}/review (PUBLISHED)" "FAIL" "Status $($p5.StatusCode)"
}

# Public search tests
$s1 = Call-Api "GET" "http://localhost:8080/api/v1/tours"
$s1Found = ($s1.Data.data.content | Where-Object { $_.id -eq $searchTourId }) -ne $null
if ($s1.StatusCode -eq 200 -and $s1Found) {
    Log-Result "TC-SEARCH-01" "Public Catalog (Guest)" "PASS" "Found published tour in catalog"
} else {
    Log-Result "TC-SEARCH-01" "Public Catalog (Guest)" "FAIL" "Status $($s1.StatusCode)"
}

$s2 = Call-Api "GET" "http://localhost:8080/api/v1/tours?search=Rafting"
$s2Found = ($s2.Data.data.content | Where-Object { $_.id -eq $searchTourId }) -ne $null
if ($s2.StatusCode -eq 200 -and $s2Found) {
    Log-Result "TC-SEARCH-02" "Search by Keyword ?search=Rafting" "PASS" "Tour found by text search"
} else {
    Log-Result "TC-SEARCH-02" "Search by Keyword ?search=Rafting" "FAIL" "Status $($s2.StatusCode)"
}

$s3 = Call-Api "GET" "http://localhost:8080/api/v1/tours?country=Kazakhstan&city=Almaty"
$s3Found = ($s3.Data.data.content | Where-Object { $_.id -eq $searchTourId }) -ne $null
if ($s3.StatusCode -eq 200 -and $s3Found) {
    Log-Result "TC-SEARCH-03" "Filter by Country & City" "PASS" "Tour found with distinct results"
} else {
    Log-Result "TC-SEARCH-03" "Filter by Country & City" "FAIL" "Status $($s3.StatusCode)"
}

$s4 = Call-Api "GET" "http://localhost:8080/api/v1/tours?tourType=COMMUNITY"
$s4Found = ($s4.Data.data.content | Where-Object { $_.id -eq $searchTourId }) -ne $null
if ($s4.StatusCode -eq 200 -and $s4Found) {
    Log-Result "TC-SEARCH-04" "Filter by Tour Type ?tourType=COMMUNITY" "PASS" "Community tour filtered"
} else {
    Log-Result "TC-SEARCH-04" "Filter by Tour Type ?tourType=COMMUNITY" "FAIL" "Status $($s4.StatusCode)"
}

$s5 = Call-Api "GET" "http://localhost:8080/api/v1/tours?minPrice=20000&maxPrice=50000"
$s5Found = ($s5.Data.data.content | Where-Object { $_.id -eq $searchTourId }) -ne $null
if ($s5.StatusCode -eq 200 -and $s5Found) {
    Log-Result "TC-SEARCH-05" "Filter by Price Range (20k - 50k)" "PASS" "Tour with 35k price matched"
} else {
    Log-Result "TC-SEARCH-05" "Filter by Price Range (20k - 50k)" "FAIL" "Status $($s5.StatusCode)"
}

$s6 = Call-Api "GET" "http://localhost:8080/api/v1/tours?country=Iceland"
if ($s6.StatusCode -eq 200 -and $s6.Data.data.content.Count -eq 0) {
    Log-Result "TC-SEARCH-06" "Filter No Matches (?country=Iceland)" "PASS" "Correctly returned empty page"
} else {
    Log-Result "TC-SEARCH-06" "Filter No Matches (?country=Iceland)" "FAIL" "Found: $($s6.Data.data.content.Count)"
}

$s7 = Call-Api "GET" "http://localhost:8080/api/v1/tours?dateFrom=2026-12-01&dateTo=2026-11-01"
if ($s7.StatusCode -eq 400 -and $s7.Data.code -eq "INVALID_TOUR_DATA") {
    Log-Result "TC-SEARCH-07" "Validation dateFrom > dateTo (400)" "PASS" "Correctly caught: $($s7.Data.code)"
} else {
    Log-Result "TC-SEARCH-07" "Validation dateFrom > dateTo (400)" "FAIL" "Status $($s7.StatusCode)"
}

$s8 = Call-Api "GET" "http://localhost:8080/api/v1/tours?minPrice=50000&maxPrice=20000"
if ($s8.StatusCode -eq 400 -and $s8.Data.code -eq "INVALID_TOUR_DATA") {
    Log-Result "TC-SEARCH-08" "Validation minPrice > maxPrice (400)" "PASS" "Correctly caught: $($s8.Data.code)"
} else {
    Log-Result "TC-SEARCH-08" "Validation minPrice > maxPrice (400)" "FAIL" "Status $($s8.StatusCode)"
}

$p6 = Call-Api "GET" "http://localhost:8080/api/v1/tours/$searchTourId"
if ($p6.StatusCode -eq 200 -and $p6.Data.data.status -eq "PUBLISHED" -and $p6.Data.data.coverImageUrl) {
    Log-Result "TC-PUB-06" "Public Tour Card View (Guest)" "PASS" "Full published tour details retrieved"
} else {
    Log-Result "TC-PUB-06" "Public Tour Card View (Guest)" "FAIL" "Status $($p6.StatusCode)"
}

$p7 = Call-Api "POST" "http://localhost:8080/api/v1/tours/$searchTourId/cancel" @{ reason = "Severe weather warning on river" } $tokenA
if ($p7.StatusCode -eq 200 -and $p7.Data.data.status -eq "CANCELLED" -and $p7.Data.data.cancellationReason) {
    Log-Result "TC-PUB-07" "POST /api/v1/tours/{id}/cancel" "PASS" "Tour cancelled with reason"
} else {
    Log-Result "TC-PUB-07" "POST /api/v1/tours/{id}/cancel" "FAIL" "Status $($p7.StatusCode)"
}

$s9 = Call-Api "GET" "http://localhost:8080/api/v1/tours"
$s9Found = ($s9.Data.data.content | Where-Object { $_.id -eq $searchTourId }) -ne $null
if ($s9.StatusCode -eq 200 -and (-not $s9Found)) {
    Log-Result "TC-PUB-08" "Cancelled Tour Excluded From Search" "PASS" "Cancelled tour hidden from public catalog"
} else {
    Log-Result "TC-PUB-08" "Cancelled Tour Excluded From Search" "FAIL" "Cancelled tour was still found"
}

# =============================================================
# GROUP 9: PARTICIPATION REQUESTS & CAPACITY (EP-0006)
# =============================================================
Write-Host "`n--- Group 9: Applications & Capacity (EP-0006) ---" -ForegroundColor Yellow

# Dedicated Tour for Applications with capacity = 1
$appTourReq = @{
    title = "Kaindy Lake Alpine Trek $ts"
    description = "Scenic hike to sunken forest of Lake Kaindy"
    tourType = "COMMUNITY"
    startDate = "2026-12-01"
    endDate = "2026-12-03"
    capacity = 1
    price = 25000
    currency = "KZT"
    countries = @("Kazakhstan")
    cities = @("Almaty", "Saty")
    includes = @("Transfer", "Guide", "Eco-fees")
    route = @(
        @{ city = "Almaty"; routeDate = "2026-12-01"; description = "Morning departure"; orderIndex = 0 },
        @{ city = "Saty"; routeDate = "2026-12-02"; description = "Kaindy Lake Excursion"; orderIndex = 1 },
        @{ city = "Almaty"; routeDate = "2026-12-03"; description = "Evening return"; orderIndex = 2 }
    )
}
$rAppTour = Call-Api "POST" "http://localhost:8080/api/v1/tours/draft" $appTourReq $tokenA
$appTourId = $rAppTour.Data.data.id

$appTourPhotoFile = [System.IO.Path]::GetTempFileName() + ".jpg"
[System.IO.File]::WriteAllBytes($appTourPhotoFile, [byte[]]@(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46))
$appTourPhotoUpload = Upload-File "http://localhost:8080/api/v1/resources/upload" $appTourPhotoFile $tokenA
Remove-Item $appTourPhotoFile -ErrorAction SilentlyContinue
Call-Api "POST" "http://localhost:8080/api/v1/tours/$appTourId/images" @{ resourceId = $appTourPhotoUpload.data.id; isCover = $true; orderIndex = 0 } $tokenA | Out-Null
Call-Api "POST" "http://localhost:8080/api/v1/tours/$appTourId/publish" $null $tokenA | Out-Null
Call-Api "POST" "http://localhost:8080/api/v1/admin/tours/$appTourId/review" @{ status = "PUBLISHED" } $tokenAdmin | Out-Null

# Traveler 1 applies
$applyReq1 = @{ message = "I want to join this trip with my tent!"; contactInfo = "+77015551234" }
$rApply1 = Call-Api "POST" "http://localhost:8080/api/v1/tours/$appTourId/applications" $applyReq1 $tokenC
$app1Id = $rApply1.Data.data.id
$app1Status = $rApply1.Data.data.status
$app1HistoryCount = $rApply1.Data.data.history.Count

if ($rApply1.StatusCode -eq 201 -and $app1Status -eq "PENDING" -and $app1HistoryCount -eq 1) {
    Log-Result "TC-APP-01" "Apply for published tour" "PASS" "Created application: id=$app1Id, status=$app1Status"
} else {
    Log-Result "TC-APP-01" "Apply for published tour" "FAIL" "Status $($rApply1.StatusCode)"
}

$rApplyDup = Call-Api "POST" "http://localhost:8080/api/v1/tours/$appTourId/applications" $applyReq1 $tokenC
if ($rApplyDup.StatusCode -eq 409) {
    Log-Result "TC-APP-02" "Duplicate application prevented" "PASS" "Correctly rejected with 409 Conflict"
} else {
    Log-Result "TC-APP-02" "Duplicate application prevented" "FAIL" "Expected 409, got $($rApplyDup.StatusCode)"
}

$rApplyOrg = Call-Api "POST" "http://localhost:8080/api/v1/tours/$appTourId/applications" $applyReq1 $tokenA
if ($rApplyOrg.StatusCode -eq 400 -or $rApplyOrg.StatusCode -eq 409) {
    Log-Result "TC-APP-03" "Organizer cannot apply to own tour" "PASS" "Correctly rejected with status $($rApplyOrg.StatusCode)"
} else {
    Log-Result "TC-APP-03" "Organizer cannot apply to own tour" "FAIL" "Expected 400, got $($rApplyOrg.StatusCode)"
}

$fakeTourId = [System.Guid]::NewGuid().ToString()
$rApplyNotFound = Call-Api "POST" "http://localhost:8080/api/v1/tours/$fakeTourId/applications" $applyReq1 $tokenC
if ($rApplyNotFound.StatusCode -eq 404) {
    Log-Result "TC-APP-04" "Apply to non-existent tour" "PASS" "Correctly returned 404 Not Found"
} else {
    Log-Result "TC-APP-04" "Apply to non-existent tour" "FAIL" "Expected 404, got $($rApplyNotFound.StatusCode)"
}

# Second Traveler applies (User B)
$applyReq2 = @{ message = "Second applicant ready to go"; contactInfo = "+77015555678" }
$rApply2 = Call-Api "POST" "http://localhost:8080/api/v1/tours/$appTourId/applications" $applyReq2 $tokenB
$app2Id = $rApply2.Data.data.id
if ($rApply2.StatusCode -eq 201 -and $rApply2.Data.data.status -eq "PENDING") {
    Log-Result "TC-APP-05" "Second traveler applies" "PASS" "Application 2 created: id=$app2Id"
} else {
    Log-Result "TC-APP-05" "Second traveler applies" "FAIL" "Status $($rApply2.StatusCode)"
}

$rMeApps = Call-Api "GET" "http://localhost:8080/api/v1/users/me/applications" $null $tokenC
$hasApp1 = ($rMeApps.Data.data.content | Where-Object { $_.id -eq $app1Id }) -ne $null
if ($rMeApps.StatusCode -eq 200 -and $hasApp1) {
    Log-Result "TC-APP-06" "Traveler views own applications" "PASS" "Found application in list"
} else {
    Log-Result "TC-APP-06" "Traveler views own applications" "FAIL" "Status $($rMeApps.StatusCode)"
}

$rOrgList = Call-Api "GET" "http://localhost:8080/api/v1/tours/$appTourId/applications?status=PENDING" $null $tokenA
$appsFound = $rOrgList.Data.data.content.Count
if ($rOrgList.StatusCode -eq 200 -and $appsFound -ge 2) {
    Log-Result "TC-APP-07" "Organizer views tour applications" "PASS" "Found $appsFound pending applications"
} else {
    Log-Result "TC-APP-07" "Organizer views tour applications" "FAIL" "Status $($rOrgList.StatusCode)"
}

$rTravForbidden = Call-Api "GET" "http://localhost:8080/api/v1/tours/$appTourId/applications" $null $tokenC
if ($rTravForbidden.StatusCode -eq 403) {
    Log-Result "TC-APP-08" "Traveler forbidden to view all tour applications" "PASS" "Correctly returned 403 Forbidden"
} else {
    Log-Result "TC-APP-08" "Traveler forbidden to view all tour applications" "FAIL" "Expected 403, got $($rTravForbidden.StatusCode)"
}

$rDetailOwner = Call-Api "GET" "http://localhost:8080/api/v1/tours/$appTourId/applications/$app1Id" $null $tokenC
$rDetailOrg = Call-Api "GET" "http://localhost:8080/api/v1/tours/$appTourId/applications/$app1Id" $null $tokenA
$rDetailStranger = Call-Api "GET" "http://localhost:8080/api/v1/tours/$appTourId/applications/$app1Id" $null $tempAccess
if ($rDetailOwner.StatusCode -eq 200 -and $rDetailOrg.StatusCode -eq 200 -and $rDetailStranger.StatusCode -eq 403) {
    Log-Result "TC-APP-09" "Application details access control" "PASS" "Owner=200, Org=200, Stranger=403"
} else {
    Log-Result "TC-APP-09" "Application details access control" "FAIL" "Owner=$($rDetailOwner.StatusCode), Org=$($rDetailOrg.StatusCode), Stranger=$($rDetailStranger.StatusCode)"
}

$rReview1 = Call-Api "POST" "http://localhost:8080/api/v1/tours/$appTourId/applications/$app1Id/review" @{ status = "ACCEPTED" } $tokenA
$tourAfterAccept = (Call-Api "GET" "http://localhost:8080/api/v1/tours/$appTourId" $null $tokenC).Data.data
if ($rReview1.StatusCode -eq 200 -and $rReview1.Data.data.status -eq "ACCEPTED" -and $tourAfterAccept.availablePlaces -eq 0) {
    Log-Result "TC-APP-10" "Organizer accepts application (capacity decreases)" "PASS" "Status=ACCEPTED, availablePlaces=0"
} else {
    Log-Result "TC-APP-10" "Organizer accepts application (capacity decreases)" "FAIL" "ReviewStatus=$($rReview1.StatusCode), Places=$($tourAfterAccept.availablePlaces)"
}

$rReviewExceed = Call-Api "POST" "http://localhost:8080/api/v1/tours/$appTourId/applications/$app2Id/review" @{ status = "ACCEPTED" } $tokenA
if ($rReviewExceed.StatusCode -eq 409 -or $rReviewExceed.StatusCode -eq 400) {
    Log-Result "TC-APP-11" "Capacity limit prevents overbooking" "PASS" "Correctly rejected with status $($rReviewExceed.StatusCode)"
} else {
    Log-Result "TC-APP-11" "Capacity limit prevents overbooking" "FAIL" "Expected 409/400, got $($rReviewExceed.StatusCode)"
}

$cancelReq = @{ reason = "Sudden schedule conflict, cannot attend" }
$rCancel1 = Call-Api "POST" "http://localhost:8080/api/v1/tours/$appTourId/applications/$app1Id/cancel" $cancelReq $tokenC
$tourAfterCancel = (Call-Api "GET" "http://localhost:8080/api/v1/tours/$appTourId" $null $tokenC).Data.data
if ($rCancel1.StatusCode -eq 200 -and $rCancel1.Data.data.status -eq "CANCELLED_BY_TRAVELER" -and $tourAfterCancel.availablePlaces -eq 1) {
    Log-Result "TC-APP-12" "Traveler cancels accepted application (capacity restored)" "PASS" "Status=CANCELLED_BY_TRAVELER, availablePlaces=1 (restored)"
} else {
    Log-Result "TC-APP-12" "Traveler cancels accepted application (capacity restored)" "FAIL" "Status=$($rCancel1.StatusCode)"
}

$rCancelAgain = Call-Api "POST" "http://localhost:8080/api/v1/tours/$appTourId/applications/$app1Id/cancel" $cancelReq $tokenC
if ($rCancelAgain.StatusCode -eq 400 -or $rCancelAgain.StatusCode -eq 409) {
    Log-Result "TC-APP-13" "Cannot cancel already cancelled application" "PASS" "Correctly rejected with status $($rCancelAgain.StatusCode)"
} else {
    Log-Result "TC-APP-13" "Cannot cancel already cancelled application" "FAIL" "Status $($rCancelAgain.StatusCode)"
}

$rReview2 = Call-Api "POST" "http://localhost:8080/api/v1/tours/$appTourId/applications/$app2Id/review" @{ status = "ACCEPTED" } $tokenA
if ($rReview2.StatusCode -eq 200 -and $rReview2.Data.data.status -eq "ACCEPTED") {
    Log-Result "TC-APP-14" "Organizer accepts application 2 after place freed" "PASS" "Application 2 successfully accepted: status=ACCEPTED"
} else {
    Log-Result "TC-APP-14" "Organizer accepts application 2 after place freed" "FAIL" "Status $($rReview2.StatusCode)"
}

# Third traveler applies and gets rejected
$userDEmail = "e2e_user_d_$ts@tourhive.com"
Call-Api "POST" "http://localhost:8080/api/v1/auth/register" @{ email = $userDEmail; password = $pwdA } | Out-Null
$otpD = Get-MailpitOtp $userDEmail
Call-Api "POST" "http://localhost:8080/api/v1/auth/verify-email" @{ email = $userDEmail; code = $otpD } | Out-Null
$rLoginD = Call-Api "POST" "http://localhost:8080/api/v1/auth/login" @{ email = $userDEmail; password = $pwdA }
$tokenD = $rLoginD.Data.data.accessToken

$applyReq3 = @{ message = "Third applicant"; contactInfo = "+77019998877" }
$rApply3 = Call-Api "POST" "http://localhost:8080/api/v1/tours/$appTourId/applications" $applyReq3 $tokenD
$app3Id = $rApply3.Data.data.id

$rejectReq = @{ status = "REJECTED"; rejectionReason = "Group is full and gear does not match route requirements" }
$rReject = Call-Api "POST" "http://localhost:8080/api/v1/tours/$appTourId/applications/$app3Id/review" $rejectReq $tokenA
if ($rReject.StatusCode -eq 200 -and $rReject.Data.data.status -eq "REJECTED" -and $rReject.Data.data.rejectionReason -eq $rejectReq.rejectionReason) {
    Log-Result "TC-APP-15" "Organizer rejects application with reason" "PASS" "Application rejected, reason saved"
} else {
    Log-Result "TC-APP-15" "Organizer rejects application with reason" "FAIL" "Status $($rReject.StatusCode)"
}

# Cascade cancellation
$rCancelTour = Call-Api "POST" "http://localhost:8080/api/v1/tours/$appTourId/cancel" @{ reason = "Severe weather warning in mountain canyon" } $tokenA
$rApp2AfterCascade = Call-Api "GET" "http://localhost:8080/api/v1/tours/$appTourId/applications/$app2Id" $null $tokenB
$statusAfterCascade = $rApp2AfterCascade.Data.data.status

if ($rCancelTour.StatusCode -eq 200 -and $statusAfterCascade -eq "CANCELLED_BY_ORGANIZER") {
    Log-Result "TC-APP-16" "Tour cancellation cascades to active applications" "PASS" "App 2 status=$statusAfterCascade"
} else {
    Log-Result "TC-APP-16" "Tour cancellation cascades to active applications" "FAIL" "TourCancel=$($rCancelTour.StatusCode), App2Status=$statusAfterCascade"
}

# =============================================================
# SUMMARY
# =============================================================
Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "                MASTER E2E TEST SUMMARY                   " -ForegroundColor Cyan
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
