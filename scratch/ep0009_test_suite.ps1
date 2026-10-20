# EP-0009 Reviews and Notifications E2E Verification Script
$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080"
$mailpitApi = "http://localhost:8025/api/v1"
$rnd = Get-Random -Minimum 100000 -Maximum 999999

Write-Host "=== EP-0009: REVIEWS & NOTIFICATIONS E2E TEST SUITE ===" -ForegroundColor Cyan

function Assert-Condition($condition, $message) {
    if (-not $condition) {
        Write-Host "[-] Assertion FAILED: $message" -ForegroundColor Red
        exit 1
    } else {
        Write-Host "[+] Assertion PASSED: $message" -ForegroundColor Green
    }
}

function Get-ErrorJson($ex) {
    if ($ex.Exception.Response) {
        $stream = $ex.Exception.Response.GetResponseStream()
        if ($stream) {
            $reader = New-Object System.IO.StreamReader($stream)
            $text = $reader.ReadToEnd()
            return ($text | ConvertFrom-Json)
        }
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

# 1. Health check
$health = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get
Assert-Condition ($health.status -eq "UP") "Backend is UP"

# 2. Register Organizer
$orgEmail = "ep9_org_$rnd@tourhive.com"
$pwd = "StrongPass123!"

$regOrgBody = @{
    email = $orgEmail
    password = $pwd
    role = "ORGANIZER"
    firstName = "Organizer"
    lastName = "Nine"
    nickname = "org_nine_$rnd"
} | ConvertTo-Json

$regOrgResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/register" -Method Post -Body $regOrgBody -ContentType "application/json"
Assert-Condition ($regOrgResp.success -eq $true) "Organizer registered"

$orgCode = Get-VerificationCode -email $orgEmail
$verifyOrgBody = @{ email = $orgEmail; code = $orgCode } | ConvertTo-Json
Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/verify-email" -Method Post -Body $verifyOrgBody -ContentType "application/json" | Out-Null

$loginOrgBody = @{ email = $orgEmail; password = $pwd } | ConvertTo-Json
$orgTokens = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method Post -Body $loginOrgBody -ContentType "application/json"
$orgHeaders = @{ Authorization = "Bearer $($orgTokens.data.accessToken)" }
$orgMe = Invoke-RestMethod -Uri "$baseUrl/api/v1/users/me" -Method Get -Headers $orgHeaders
$orgUserId = $orgMe.data.userId

# Activate organizer role & profile
Invoke-RestMethod -Uri "$baseUrl/api/v1/organizers/me/activate" -Method Post -Headers $orgHeaders | Out-Null
Write-Host "[+] Organizer profile activated: $orgUserId" -ForegroundColor DarkGray

# Register Traveler 1 (accepted participant)
$travEmail = "ep9_trav_$rnd@tourhive.com"
$regTravBody = @{
    email = $travEmail
    password = $pwd
    role = "USER"
    firstName = "Alice"
    lastName = "Traveler"
    nickname = "alice_trav_$rnd"
} | ConvertTo-Json

Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/register" -Method Post -Body $regTravBody -ContentType "application/json" | Out-Null
$travCode = Get-VerificationCode -email $travEmail
Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/verify-email" -Method Post -Body (@{ email = $travEmail; code = $travCode } | ConvertTo-Json) -ContentType "application/json" | Out-Null
$travTokens = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method Post -Body (@{ email = $travEmail; password = $pwd } | ConvertTo-Json) -ContentType "application/json"
$travHeaders = @{ Authorization = "Bearer $($travTokens.data.accessToken)" }
$travMe = Invoke-RestMethod -Uri "$baseUrl/api/v1/users/me" -Method Get -Headers $travHeaders
$travUserId = $travMe.data.userId
Write-Host "[+] Traveler 1 logged in: $travUserId" -ForegroundColor DarkGray

# Register Stranger Traveler (not participant)
$strangerEmail = "ep9_stranger_$rnd@tourhive.com"
$regStrBody = @{
    email = $strangerEmail
    password = $pwd
    role = "USER"
    firstName = "Bob"
    lastName = "Stranger"
    nickname = "bob_stranger_$rnd"
} | ConvertTo-Json

Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/register" -Method Post -Body $regStrBody -ContentType "application/json" | Out-Null
$strCode = Get-VerificationCode -email $strangerEmail
Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/verify-email" -Method Post -Body (@{ email = $strangerEmail; code = $strCode } | ConvertTo-Json) -ContentType "application/json" | Out-Null
$strTokens = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method Post -Body (@{ email = $strangerEmail; password = $pwd } | ConvertTo-Json) -ContentType "application/json"
$strHeaders = @{ Authorization = "Bearer $($strTokens.data.accessToken)" }
Write-Host "[+] Stranger Traveler logged in" -ForegroundColor DarkGray

# Register Moderator
$modEmail = "ep9_mod_$rnd@tourhive.com"
$regModBody = @{
    email = $modEmail
    password = $pwd
    role = "USER"
    firstName = "Mod"
    lastName = "Staff"
    nickname = "mod_staff_$rnd"
} | ConvertTo-Json

Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/register" -Method Post -Body $regModBody -ContentType "application/json" | Out-Null
$modCode = Get-VerificationCode -email $modEmail
Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/verify-email" -Method Post -Body (@{ email = $modEmail; code = $modCode } | ConvertTo-Json) -ContentType "application/json" | Out-Null

docker exec tourhive-postgres psql -U tourhive_user -d tourhive_db -c "UPDATE users SET role = 'MODERATOR' WHERE email = '$modEmail';" | Out-Null
$modTokens = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method Post -Body (@{ email = $modEmail; password = $pwd } | ConvertTo-Json) -ContentType "application/json"
$modHeaders = @{ Authorization = "Bearer $($modTokens.data.accessToken)" }
Write-Host "[+] Moderator promoted and logged in" -ForegroundColor DarkGray

# 3. Create Upcoming Tour (End date in future)
$tomorrow = (Get-Date).AddDays(1).ToString("yyyy-MM-dd")
$futureEnd = (Get-Date).AddDays(5).ToString("yyyy-MM-dd")

$upcomingTourBody = @{
    title = "Upcoming Mountain Hike $rnd"
    description = "A wonderful future trip"
    tourType = "COMMUNITY"
    startDate = $tomorrow
    endDate = $futureEnd
    capacity = 10
    price = 0
    currency = "KZT"
    countries = @("Kazakhstan")
    cities = @("Almaty")
    includes = @("Guide", "Water")
} | ConvertTo-Json

$upcomingTourResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/tours/draft" -Method Post -Body $upcomingTourBody -Headers $orgHeaders -ContentType "application/json"
$upcomingTourId = $upcomingTourResp.data.id
Write-Host "[+] Upcoming tour draft created: $upcomingTourId" -ForegroundColor DarkGray

# Attach photo to upcoming tour
$photoFile1 = [System.IO.Path]::GetTempFileName() + ".jpg"
[System.IO.File]::WriteAllBytes($photoFile1, [byte[]]@(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46))
$photoUpload1 = & curl.exe -s -X POST "$baseUrl/api/v1/resources/upload" -H "Authorization: Bearer $($orgTokens.data.accessToken)" -F "file=@$photoFile1" | ConvertFrom-Json
$resId1 = $photoUpload1.data.id
Invoke-RestMethod -Uri "$baseUrl/api/v1/tours/$upcomingTourId/images" -Method Post -Body (@{ resourceId = $resId1; isCover = $true; orderIndex = 0 } | ConvertTo-Json) -Headers $orgHeaders -ContentType "application/json" | Out-Null

# Submit & approve upcoming tour
Invoke-RestMethod -Uri "$baseUrl/api/v1/tours/$upcomingTourId/publish" -Method Post -Headers $orgHeaders | Out-Null
Invoke-RestMethod -Uri "$baseUrl/api/v1/admin/tours/$upcomingTourId/review" -Method Post -Body (@{ status = "PUBLISHED" } | ConvertTo-Json) -Headers $modHeaders -ContentType "application/json" | Out-Null
Write-Host "[+] Upcoming tour published" -ForegroundColor DarkGray

# Traveler 1 applies to upcoming tour
$applyResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/tours/$upcomingTourId/applications" -Method Post -Headers $travHeaders -ContentType "application/json"
$upcomingAppId = $applyResp.data.id
Assert-Condition ($applyResp.success -eq $true) "Traveler applied to upcoming tour"

# Organizer accepts application
$reviewAppBody = @{ status = "ACCEPTED" } | ConvertTo-Json
Invoke-RestMethod -Uri "$baseUrl/api/v1/tours/$upcomingTourId/applications/$upcomingAppId/review" -Method Post -Body $reviewAppBody -Headers $orgHeaders -ContentType "application/json" | Out-Null
Write-Host "[+] Application accepted" -ForegroundColor DarkGray

# Traveler 1 attempts to review upcoming tour -> MUST FAIL with 400 TOUR_NOT_COMPLETED
$reviewAttemptFailed = $false
try {
    $earlyReviewBody = @{ rating = 5; comment = "Too early" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/v1/tours/$upcomingTourId/reviews" -Method Post -Body $earlyReviewBody -Headers $travHeaders -ContentType "application/json"
} catch {
    $reviewAttemptFailed = $true
    $errResp = Get-ErrorJson $_
    Assert-Condition ($errResp.code -eq "TOUR_NOT_COMPLETED") "Cannot review unfinished tour (TOUR_NOT_COMPLETED)"
}
Assert-Condition ($reviewAttemptFailed -eq $true) "Review rejected on upcoming tour"

# 4. Create Completed Tour (start date today, end date today)
$today = (Get-Date).ToString("yyyy-MM-dd")
$completedTourBody = @{
    title = "Completed Canyon Day Tour $rnd"
    description = "One day canyon tour"
    tourType = "COMMUNITY"
    startDate = $today
    endDate = $today
    capacity = 5
    price = 0
    currency = "KZT"
    countries = @("Kazakhstan")
    cities = @("Almaty")
    includes = @("Transport", "Lunch")
} | ConvertTo-Json

$compTourResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/tours/draft" -Method Post -Body $completedTourBody -Headers $orgHeaders -ContentType "application/json"
$compTourId = $compTourResp.data.id

# Attach photo to completed tour
$photoFile2 = [System.IO.Path]::GetTempFileName() + ".jpg"
[System.IO.File]::WriteAllBytes($photoFile2, [byte[]]@(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46))
$photoUpload2 = & curl.exe -s -X POST "$baseUrl/api/v1/resources/upload" -H "Authorization: Bearer $($orgTokens.data.accessToken)" -F "file=@$photoFile2" | ConvertFrom-Json
$resId2 = $photoUpload2.data.id
Invoke-RestMethod -Uri "$baseUrl/api/v1/tours/$compTourId/images" -Method Post -Body (@{ resourceId = $resId2; isCover = $true; orderIndex = 0 } | ConvertTo-Json) -Headers $orgHeaders -ContentType "application/json" | Out-Null

Invoke-RestMethod -Uri "$baseUrl/api/v1/tours/$compTourId/publish" -Method Post -Headers $orgHeaders | Out-Null
Invoke-RestMethod -Uri "$baseUrl/api/v1/admin/tours/$compTourId/review" -Method Post -Body (@{ status = "PUBLISHED" } | ConvertTo-Json) -Headers $modHeaders -ContentType "application/json" | Out-Null
Write-Host "[+] Completed day tour published: $compTourId" -ForegroundColor DarkGray

# Traveler 1 applies & gets accepted
$app2Resp = Invoke-RestMethod -Uri "$baseUrl/api/v1/tours/$compTourId/applications" -Method Post -Headers $travHeaders -ContentType "application/json"
$compAppId = $app2Resp.data.id
Invoke-RestMethod -Uri "$baseUrl/api/v1/tours/$compTourId/applications/$compAppId/review" -Method Post -Body (@{ status = "ACCEPTED" } | ConvertTo-Json) -Headers $orgHeaders -ContentType "application/json" | Out-Null
Write-Host "[+] Traveler 1 accepted on completed tour" -ForegroundColor DarkGray

# Stranger attempts review -> MUST FAIL with 403 REVIEW_ACCESS_DENIED
$strangerFailed = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/api/v1/tours/$compTourId/reviews" -Method Post -Body (@{ rating = 5; comment = "Was not there" } | ConvertTo-Json) -Headers $strHeaders -ContentType "application/json"
} catch {
    $strangerFailed = $true
    $err = Get-ErrorJson $_
    Assert-Condition ($err.code -eq "REVIEW_ACCESS_DENIED") "Stranger blocked with REVIEW_ACCESS_DENIED"
}
Assert-Condition ($strangerFailed -eq $true) "Stranger cannot review"

# Organizer attempts to review own tour -> MUST FAIL with 400 CANNOT_REVIEW_OWN_TOUR
$orgSelfFailed = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/api/v1/tours/$compTourId/reviews" -Method Post -Body (@{ rating = 5; comment = "I am awesome" } | ConvertTo-Json) -Headers $orgHeaders -ContentType "application/json"
} catch {
    $orgSelfFailed = $true
    $err = Get-ErrorJson $_
    Assert-Condition ($err.code -eq "CANNOT_REVIEW_OWN_TOUR") "Organizer blocked with CANNOT_REVIEW_OWN_TOUR"
}
Assert-Condition ($orgSelfFailed -eq $true) "Organizer cannot review own tour"

# Traveler 1 successfully submits review
$reviewBody = @{
    rating = 5
    comment = "Breathtaking views and amazing guide!"
} | ConvertTo-Json

$createdReview = Invoke-RestMethod -Uri "$baseUrl/api/v1/tours/$compTourId/reviews" -Method Post -Body $reviewBody -Headers $travHeaders -ContentType "application/json"
Assert-Condition ($createdReview.success -eq $true) "Review created successfully"
Assert-Condition ($createdReview.data.rating -eq 5) "Review rating is 5"
Assert-Condition ($createdReview.data.authorName -ne $null) "Author name populated"
$reviewId = $createdReview.data.id

# Duplicate review -> MUST FAIL with 409 DUPLICATE_REVIEW
$dupFailed = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/api/v1/tours/$compTourId/reviews" -Method Post -Body $reviewBody -Headers $travHeaders -ContentType "application/json"
} catch {
    $dupFailed = $true
    $err = Get-ErrorJson $_
    Assert-Condition ($err.code -eq "DUPLICATE_REVIEW") "Duplicate review blocked with DUPLICATE_REVIEW"
}
Assert-Condition ($dupFailed -eq $true) "Duplicate review rejected"

# Update review to 4 stars
$updateReviewBody = @{
    rating = 4
    comment = "Updated: Great tour but slightly windy"
} | ConvertTo-Json
$updatedReview = Invoke-RestMethod -Uri "$baseUrl/api/v1/reviews/$reviewId" -Method Put -Body $updateReviewBody -Headers $travHeaders -ContentType "application/json"
Assert-Condition ($updatedReview.data.rating -eq 4) "Review updated to 4 stars"
Assert-Condition ($updatedReview.data.comment -match "windy") "Review comment updated"

# Public tour reviews
$tourReviews = Invoke-RestMethod -Uri "$baseUrl/api/v1/tours/$compTourId/reviews" -Method Get
Assert-Condition ($tourReviews.data.content.Count -eq 1) "Tour has exactly 1 review"
Assert-Condition ($tourReviews.data.content[0].rating -eq 4) "Tour review rating is 4"

# Public organizer reviews
$orgReviews = Invoke-RestMethod -Uri "$baseUrl/api/v1/organizers/$orgUserId/reviews" -Method Get
Assert-Condition ($orgReviews.data.content.Count -eq 1) "Organizer has exactly 1 review"
Assert-Condition ($orgReviews.data.content[0].tourTitle -ne $null) "Tour title populated in organizer reviews"

# Organizer Profile Rating recalculation
$orgProfile = Invoke-RestMethod -Uri "$baseUrl/api/v1/organizers/$orgUserId" -Method Get
Assert-Condition ($orgProfile.data.rating -eq 4.0) "Organizer profile rating updated to 4.0"
Assert-Condition ($orgProfile.data.reviewsCount -eq 1) "Organizer reviewsCount updated to 1"

# 5. In-App Notifications Verification
# Organizer should have received notifications for:
# - APPLICATION_RECEIVED (upcoming tour)
# - TOUR_APPROVED (upcoming tour)
# - APPLICATION_RECEIVED (completed tour)
# - TOUR_APPROVED (completed tour)
# - TOUR_REVIEWED (from Traveler 1)

$orgUnread = Invoke-RestMethod -Uri "$baseUrl/api/v1/notifications/unread-count" -Method Get -Headers $orgHeaders
Assert-Condition ($orgUnread.data.unreadCount -ge 3) "Organizer has unread notifications: $($orgUnread.data.unreadCount)"

$orgNotifs = Invoke-RestMethod -Uri "$baseUrl/api/v1/notifications?unreadOnly=true" -Method Get -Headers $orgHeaders
Assert-Condition ($orgNotifs.data.content.Count -ge 3) "Organizer can list unread notifications"

# Find a notification and mark it as read
$firstNotifId = $orgNotifs.data.content[0].id
$markReadResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/notifications/$firstNotifId/read" -Method Patch -Headers $orgHeaders
Assert-Condition ($markReadResp.success -eq $true) "Single notification marked as read"

# Mark all remaining notifications as read
$markAllResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/notifications/read-all" -Method Patch -Headers $orgHeaders
Assert-Condition ($markAllResp.success -eq $true) "All notifications marked as read"

$orgUnreadAfter = Invoke-RestMethod -Uri "$baseUrl/api/v1/notifications/unread-count" -Method Get -Headers $orgHeaders
Assert-Condition ($orgUnreadAfter.data.unreadCount -eq 0) "Organizer unread count is now 0"

# 6. Device Tokens Registration & Removal
$deviceTokenBody = @{
    token = "fcm_token_ep9_alice_phone"
    deviceType = "ANDROID"
} | ConvertTo-Json
$regTokenResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/notifications/device-token" -Method Post -Body $deviceTokenBody -Headers $travHeaders -ContentType "application/json"
Assert-Condition ($regTokenResp.success -eq $true) "Device token registered"

$delTokenResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/notifications/device-token?token=fcm_token_ep9_alice_phone" -Method Delete -Headers $travHeaders
Assert-Condition ($delTokenResp.success -eq $true) "Device token removed"

# 7. Delete Review & Verify Organizer Rating recalculated to 0
$delReviewResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/reviews/$reviewId" -Method Delete -Headers $travHeaders
Assert-Condition ($delReviewResp.success -eq $true) "Review deleted by author"

$tourReviewsAfter = Invoke-RestMethod -Uri "$baseUrl/api/v1/tours/$compTourId/reviews" -Method Get
Assert-Condition ($tourReviewsAfter.data.content.Count -eq 0) "Tour has 0 reviews after deletion"

$orgProfileAfter = Invoke-RestMethod -Uri "$baseUrl/api/v1/organizers/$orgUserId" -Method Get
Assert-Condition ($orgProfileAfter.data.reviewsCount -eq 0) "Organizer reviewsCount reset to 0"
Assert-Condition ($orgProfileAfter.data.rating -eq 0.0) "Organizer rating reset to 0.0"

Write-Host "`n=== ALL EP-0009 LIVE E2E TESTS PASSED SUCCESSFULLY! ===" -ForegroundColor Green
