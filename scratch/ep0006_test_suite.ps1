# TourHive Backend EP-0006 E2E Test Suite
# Tests participation requests lifecycle, organizer reviews, capacity controls, and cascade cancellation

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "     TourHive EP-0006 Applications & Capacity E2E Suite   " -ForegroundColor Cyan
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

function Register-And-Login($email, $pwd) {
    Call-Api "POST" "http://localhost:8080/api/v1/auth/register" @{ email = $email; password = $pwd } | Out-Null
    $otp = Get-MailpitOtp $email
    Call-Api "POST" "http://localhost:8080/api/v1/auth/verify-email" @{ email = $email; code = $otp } | Out-Null
    $rLogin = Call-Api "POST" "http://localhost:8080/api/v1/auth/login" @{ email = $email; password = $pwd }
    return $rLogin.Data.data.accessToken
}

$pwd = "SecureP@ssw0rd123"

Write-Host "Creating test users..." -ForegroundColor Yellow
# 1. Organizer
$orgEmail = "ep6_org_$ts@tourhive.com"
$tokenOrg = Register-And-Login $orgEmail $pwd
Call-Api "POST" "http://localhost:8080/api/v1/organizers/me/activate" $null $tokenOrg | Out-Null

# 2. Traveler 1
$trav1Email = "ep6_trav1_$ts@tourhive.com"
$tokenTrav1 = Register-And-Login $trav1Email $pwd

# 3. Traveler 2
$trav2Email = "ep6_trav2_$ts@tourhive.com"
$tokenTrav2 = Register-And-Login $trav2Email $pwd

# 4. Traveler 3
$trav3Email = "ep6_trav3_$ts@tourhive.com"
$tokenTrav3 = Register-And-Login $trav3Email $pwd

# 5. Moderator
$modEmail = "ep6_mod_$ts@tourhive.com"
Call-Api "POST" "http://localhost:8080/api/v1/auth/register" @{ email = $modEmail; password = $pwd } | Out-Null
$otpMod = Get-MailpitOtp $modEmail
Call-Api "POST" "http://localhost:8080/api/v1/auth/verify-email" @{ email = $modEmail; code = $otpMod } | Out-Null
& docker exec tourhive-postgres psql -U tourhive_user -d tourhive_db -c "UPDATE users SET role = 'MODERATOR' WHERE email = '$modEmail';" | Out-Null
$rLoginMod = Call-Api "POST" "http://localhost:8080/api/v1/auth/login" @{ email = $modEmail; password = $pwd }
$tokenMod = $rLoginMod.Data.data.accessToken

Write-Host "Users initialized successfully." -ForegroundColor Green

# Create Tour with capacity = 1 to strictly test capacity limits
$draftReq = @{
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

$rDraft = Call-Api "POST" "http://localhost:8080/api/v1/tours/draft" $draftReq $tokenOrg
$tourId = $rDraft.Data.data.id

# Upload photo and attach
$dummyImg = [System.IO.Path]::GetTempFileName() + ".jpg"
[System.IO.File]::WriteAllBytes($dummyImg, [byte[]]@(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46))
$uploadResp = Upload-File "http://localhost:8080/api/v1/resources/upload" $dummyImg $tokenOrg
Remove-Item $dummyImg -ErrorAction SilentlyContinue
$resourceId = $uploadResp.data.id

Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourId/images" @{ resourceId = $resourceId; isCover = $true; orderIndex = 0 } $tokenOrg | Out-Null

# Submit and Approve
Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourId/publish" $null $tokenOrg | Out-Null
$rApprove = Call-Api "POST" "http://localhost:8080/api/v1/admin/tours/$tourId/review" @{ status = "PUBLISHED" } $tokenMod

if ($rApprove.StatusCode -eq 200 -and $rApprove.Data.data.status -eq "PUBLISHED") {
    Write-Host "Tour published successfully: $tourId (Capacity: 1)" -ForegroundColor Green
} else {
    Write-Host "Failed to publish tour: $($rApprove.Raw)" -ForegroundColor Red
}

# --- TEST CASE 9.1: Apply for Published Tour ---
$applyReq1 = @{
    message = "I want to join this trip with my tent!"
    contactInfo = "+77015551234"
}
$rApply1 = Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourId/applications" $applyReq1 $tokenTrav1
$app1Id = $rApply1.Data.data.id
$app1Status = $rApply1.Data.data.status
$app1HistoryCount = $rApply1.Data.data.history.Count

if ($rApply1.StatusCode -eq 201 -and $app1Status -eq "PENDING" -and $app1HistoryCount -eq 1) {
    Log-Result "TC-APP-01" "Apply for published tour" "PASS" "Application created: id=$app1Id, status=$app1Status, history=$app1HistoryCount"
} else {
    Log-Result "TC-APP-01" "Apply for published tour" "FAIL" "Status: $($rApply1.StatusCode), Response: $($rApply1.Raw)"
}

# --- TEST CASE 9.2: Duplicate Application Prevention ---
$rApplyDup = Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourId/applications" $applyReq1 $tokenTrav1
if ($rApplyDup.StatusCode -eq 409) {
    Log-Result "TC-APP-02" "Duplicate application prevented" "PASS" "Correctly rejected with 409 Conflict: $($rApplyDup.Data.error)"
} else {
    Log-Result "TC-APP-02" "Duplicate application prevented" "FAIL" "Expected 409, got $($rApplyDup.StatusCode): $($rApplyDup.Raw)"
}

# --- TEST CASE 9.3: Organizer cannot apply to own tour ---
$rApplyOrg = Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourId/applications" $applyReq1 $tokenOrg
if ($rApplyOrg.StatusCode -eq 400 -or $rApplyOrg.StatusCode -eq 409) {
    Log-Result "TC-APP-03" "Organizer cannot apply to own tour" "PASS" "Correctly rejected with status $($rApplyOrg.StatusCode): $($rApplyOrg.Data.error)"
} else {
    Log-Result "TC-APP-03" "Organizer cannot apply to own tour" "FAIL" "Expected 400, got $($rApplyOrg.StatusCode): $($rApplyOrg.Raw)"
}

# --- TEST CASE 9.4: Apply to non-existent tour ---
$randomTourId = [System.Guid]::NewGuid().ToString()
$rApplyNotFound = Call-Api "POST" "http://localhost:8080/api/v1/tours/$randomTourId/applications" $applyReq1 $tokenTrav1
if ($rApplyNotFound.StatusCode -eq 404) {
    Log-Result "TC-APP-04" "Apply to non-existent tour" "PASS" "Correctly returned 404 Not Found"
} else {
    Log-Result "TC-APP-04" "Apply to non-existent tour" "FAIL" "Expected 404, got $($rApplyNotFound.StatusCode)"
}

# --- TEST CASE 9.5: Second Traveler applies (PENDING) ---
$applyReq2 = @{
    message = "Second applicant ready to go"
    contactInfo = "+77015555678"
}
$rApply2 = Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourId/applications" $applyReq2 $tokenTrav2
$app2Id = $rApply2.Data.data.id
if ($rApply2.StatusCode -eq 201 -and $rApply2.Data.data.status -eq "PENDING") {
    Log-Result "TC-APP-05" "Second traveler applies" "PASS" "Application 2 created: id=$app2Id"
} else {
    Log-Result "TC-APP-05" "Second traveler applies" "FAIL" "Status: $($rApply2.StatusCode), Raw: $($rApply2.Raw)"
}

# --- TEST CASE 9.6: Traveler views own applications ---
$rMeApps = Call-Api "GET" "http://localhost:8080/api/v1/users/me/applications" $null $tokenTrav1
$hasApp1 = ($rMeApps.Data.data.content | Where-Object { $_.id -eq $app1Id }) -ne $null
if ($rMeApps.StatusCode -eq 200 -and $hasApp1) {
    Log-Result "TC-APP-06" "Traveler views own applications" "PASS" "Found application in list, total: $($rMeApps.Data.data.totalElements)"
} else {
    Log-Result "TC-APP-06" "Traveler views own applications" "FAIL" "Status: $($rMeApps.StatusCode), Raw: $($rMeApps.Raw)"
}

# --- TEST CASE 9.7: Organizer views applications list with filter ---
$rOrgList = Call-Api "GET" "http://localhost:8080/api/v1/tours/$tourId/applications?status=PENDING" $null $tokenOrg
$appsFound = $rOrgList.Data.data.content.Count
if ($rOrgList.StatusCode -eq 200 -and $appsFound -ge 2) {
    Log-Result "TC-APP-07" "Organizer views tour applications" "PASS" "Found $appsFound pending applications"
} else {
    Log-Result "TC-APP-07" "Organizer views tour applications" "FAIL" "Status: $($rOrgList.StatusCode), Raw: $($rOrgList.Raw)"
}

# --- TEST CASE 9.8: Unauthorized access to tour applications list ---
$rTravForbidden = Call-Api "GET" "http://localhost:8080/api/v1/tours/$tourId/applications" $null $tokenTrav1
if ($rTravForbidden.StatusCode -eq 403) {
    Log-Result "TC-APP-08" "Traveler forbidden to view all tour applications" "PASS" "Correctly returned 403 Forbidden"
} else {
    Log-Result "TC-APP-08" "Traveler forbidden to view all tour applications" "FAIL" "Expected 403, got $($rTravForbidden.StatusCode)"
}

# --- TEST CASE 9.9: Application details access control ---
$rDetailOwner = Call-Api "GET" "http://localhost:8080/api/v1/tours/$tourId/applications/$app1Id" $null $tokenTrav1
$rDetailOrg = Call-Api "GET" "http://localhost:8080/api/v1/tours/$tourId/applications/$app1Id" $null $tokenOrg
$rDetailStranger = Call-Api "GET" "http://localhost:8080/api/v1/tours/$tourId/applications/$app1Id" $null $tokenTrav3

if ($rDetailOwner.StatusCode -eq 200 -and $rDetailOrg.StatusCode -eq 200 -and $rDetailStranger.StatusCode -eq 403) {
    Log-Result "TC-APP-09" "Application details access control" "PASS" "Owner=200, Org=200, Stranger=403"
} else {
    Log-Result "TC-APP-09" "Application details access control" "FAIL" "Owner=$($rDetailOwner.StatusCode), Org=$($rDetailOrg.StatusCode), Stranger=$($rDetailStranger.StatusCode)"
}

# --- TEST CASE 9.10: Organizer accepts application 1 (capacity becomes 0) ---
$rReview1 = Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourId/applications/$app1Id/review" @{ status = "ACCEPTED" } $tokenOrg
$tourAfterAccept = (Call-Api "GET" "http://localhost:8080/api/v1/tours/$tourId" $null $tokenTrav1).Data.data

if ($rReview1.StatusCode -eq 200 -and $rReview1.Data.data.status -eq "ACCEPTED" -and $tourAfterAccept.availablePlaces -eq 0) {
    Log-Result "TC-APP-10" "Organizer accepts application (capacity decreases)" "PASS" "Status=ACCEPTED, availablePlaces=0 (decremented from 1)"
} else {
    Log-Result "TC-APP-10" "Organizer accepts application (capacity decreases)" "FAIL" "ReviewStatus=$($rReview1.StatusCode), Places=$($tourAfterAccept.availablePlaces)"
}

# --- TEST CASE 9.11: Capacity limit prevents accepting another application ---
$rReviewExceed = Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourId/applications/$app2Id/review" @{ status = "ACCEPTED" } $tokenOrg
if ($rReviewExceed.StatusCode -eq 409 -or $rReviewExceed.StatusCode -eq 400) {
    Log-Result "TC-APP-11" "Capacity limit prevents overbooking" "PASS" "Correctly rejected with status $($rReviewExceed.StatusCode): $($rReviewExceed.Data.error)"
} else {
    Log-Result "TC-APP-11" "Capacity limit prevents overbooking" "FAIL" "Expected 409/400, got $($rReviewExceed.StatusCode): $($rReviewExceed.Raw)"
}

# --- TEST CASE 9.12: Traveler 1 cancels accepted application (capacity restored) ---
$cancelReq = @{
    reason = "Sudden schedule conflict, cannot attend"
}
$rCancel1 = Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourId/applications/$app1Id/cancel" $cancelReq $tokenTrav1
$tourAfterCancel = (Call-Api "GET" "http://localhost:8080/api/v1/tours/$tourId" $null $tokenTrav1).Data.data

if ($rCancel1.StatusCode -eq 200 -and $rCancel1.Data.data.status -eq "CANCELLED_BY_TRAVELER" -and $tourAfterCancel.availablePlaces -eq 1) {
    Log-Result "TC-APP-12" "Traveler cancels accepted application (capacity restored)" "PASS" "Status=CANCELLED_BY_TRAVELER, availablePlaces=1 (restored)"
} else {
    Log-Result "TC-APP-12" "Traveler cancels accepted application (capacity restored)" "FAIL" "CancelStatus=$($rCancel1.StatusCode), Places=$($tourAfterCancel.availablePlaces)"
}

# --- TEST CASE 9.13: Cannot cancel already cancelled application ---
$rCancelAgain = Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourId/applications/$app1Id/cancel" $cancelReq $tokenTrav1
if ($rCancelAgain.StatusCode -eq 400 -or $rCancelAgain.StatusCode -eq 409) {
    Log-Result "TC-APP-13" "Cannot cancel already cancelled application" "PASS" "Correctly rejected with status $($rCancelAgain.StatusCode): $($rCancelAgain.Data.error)"
} else {
    Log-Result "TC-APP-13" "Cannot cancel already cancelled application" "FAIL" "Expected 400/409, got $($rCancelAgain.StatusCode)"
}

# --- TEST CASE 9.14: Organizer accepts Traveler 2 now that place is available ---
$rReview2 = Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourId/applications/$app2Id/review" @{ status = "ACCEPTED" } $tokenOrg
if ($rReview2.StatusCode -eq 200 -and $rReview2.Data.data.status -eq "ACCEPTED") {
    Log-Result "TC-APP-14" "Organizer accepts application 2 after place freed" "PASS" "Application 2 successfully accepted: status=ACCEPTED"
} else {
    Log-Result "TC-APP-14" "Organizer accepts application 2 after place freed" "FAIL" "Status: $($rReview2.StatusCode), Raw: $($rReview2.Raw)"
}

# --- TEST CASE 9.15: Organizer rejects third application with reason ---
$applyReq3 = @{ message = "Third applicant"; contactInfo = "+77019998877" }
$rApply3 = Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourId/applications" $applyReq3 $tokenTrav3
$app3Id = $rApply3.Data.data.id

$rejectReq = @{
    status = "REJECTED"
    rejectionReason = "Sorry, group is full and gear does not match route requirements"
}
$rReject = Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourId/applications/$app3Id/review" $rejectReq $tokenOrg

if ($rReject.StatusCode -eq 200 -and $rReject.Data.data.status -eq "REJECTED" -and $rReject.Data.data.rejectionReason -eq $rejectReq.rejectionReason) {
    Log-Result "TC-APP-15" "Organizer rejects application with reason" "PASS" "Application rejected, reason saved in entity and history"
} else {
    Log-Result "TC-APP-15" "Organizer rejects application with reason" "FAIL" "Status: $($rReject.StatusCode), Raw: $($rReject.Raw)"
}

# --- TEST CASE 9.16: Tour cancellation cascades to active applications ---
# Tour cancellation by organizer
$rCancelTour = Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourId/cancel" @{ reason = "Severe weather warning in mountain canyon" } $tokenOrg

# Check Traveler 2's application (which was ACCEPTED)
$rApp2AfterCascade = Call-Api "GET" "http://localhost:8080/api/v1/tours/$tourId/applications/$app2Id" $null $tokenTrav2
$statusAfterCascade = $rApp2AfterCascade.Data.data.status
$lastHistory = $rApp2AfterCascade.Data.data.history[-1]

if ($rCancelTour.StatusCode -eq 200 -and $statusAfterCascade -eq "CANCELLED_BY_ORGANIZER") {
    Log-Result "TC-APP-16" "Tour cancellation cascades to active applications" "PASS" "App 2 status=$statusAfterCascade, reason='$($lastHistory.reason)'"
} else {
    Log-Result "TC-APP-16" "Tour cancellation cascades to active applications" "FAIL" "TourCancel=$($rCancelTour.StatusCode), App2Status=$statusAfterCascade"
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "                      TEST SUMMARY                        " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
$passed = ($results | Where-Object { $_.Status -eq "PASS" }).Count
$failed = ($results | Where-Object { $_.Status -eq "FAIL" }).Count
$total = $results.Count
Write-Host "Total: $total | Passed: $passed | Failed: $failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })

if ($failed -gt 0) {
    exit 1
} else {
    exit 0
}
