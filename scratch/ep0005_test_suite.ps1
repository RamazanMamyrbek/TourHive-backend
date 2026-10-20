# TourHive Backend EP-0005 E2E Test Suite
# Tests publishing lifecycle, moderation, cancellation, and dynamic search (Aqyly Turaq Specification)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "     TourHive EP-0005 Publishing & Search E2E Suite       " -ForegroundColor Cyan
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

$pwd = "SecureP@ssw0rd123"

# Setup Organizer User
$orgEmail = "ep5_org_$ts@tourhive.com"
Call-Api "POST" "http://localhost:8080/api/v1/auth/register" @{ email = $orgEmail; password = $pwd } | Out-Null
$otpOrg = Get-MailpitOtp $orgEmail
Call-Api "POST" "http://localhost:8080/api/v1/auth/verify-email" @{ email = $orgEmail; code = $otpOrg } | Out-Null
$rLoginOrg = Call-Api "POST" "http://localhost:8080/api/v1/auth/login" @{ email = $orgEmail; password = $pwd }
$tokenOrg = $rLoginOrg.Data.data.accessToken
Call-Api "POST" "http://localhost:8080/api/v1/organizers/me/activate" $null $tokenOrg | Out-Null

# Setup Moderator User
$modEmail = "ep5_mod_$ts@tourhive.com"
Call-Api "POST" "http://localhost:8080/api/v1/auth/register" @{ email = $modEmail; password = $pwd } | Out-Null
$otpMod = Get-MailpitOtp $modEmail
Call-Api "POST" "http://localhost:8080/api/v1/auth/verify-email" @{ email = $modEmail; code = $otpMod } | Out-Null
& docker exec tourhive-postgres psql -U tourhive_user -d tourhive_db -c "UPDATE users SET role = 'MODERATOR' WHERE email = '$modEmail';" | Out-Null
$rLoginMod = Call-Api "POST" "http://localhost:8080/api/v1/auth/login" @{ email = $modEmail; password = $pwd }
$tokenMod = $rLoginMod.Data.data.accessToken

# Step 1: Create a Draft Tour
$draftReq = @{
    title = "Ili River Valley Rafting $ts"
    description = "Exciting 4-day rafting and camping journey along Ili River"
    tourType = "COMMUNITY"
    startDate = "2026-11-15"
    endDate = "2026-11-19"
    capacity = 14
    price = 35000
    currency = "KZT"
    countries = @("Kazakhstan")
    cities = @("Almaty", "Kapchagay")
    includes = @("Rafts", "Lifejackets", "Meals", "Camp Equipment")
    route = @(
        @{ city = "Almaty"; routeDate = "2026-11-15"; description = "Departure"; orderIndex = 0 },
        @{ city = "Tamgaly Tas"; routeDate = "2026-11-16"; description = "Buddhist Rock Art & Camp"; orderIndex = 1 },
        @{ city = "Kapchagay"; routeDate = "2026-11-19"; description = "Finish and return"; orderIndex = 2 }
    )
}
$draftRes = Call-Api "POST" "http://localhost:8080/api/v1/tours/draft" $draftReq $tokenOrg
$tourId = $draftRes.Data.data.id

# TC-PUB-01: Submit for publication without photos -> 400
$p1 = Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourId/publish" $null $tokenOrg
if ($p1.StatusCode -eq 400) {
    Log-Result "TC-PUB-01" "Publish without photos (400)" "PASS" "Correctly rejected: $($p1.Data.message)"
} else {
    Log-Result "TC-PUB-01" "Publish without photos (400)" "FAIL" "Status $($p1.StatusCode)"
}

# Attach photo to tour
$photoFile = [System.IO.Path]::GetTempFileName() + ".jpg"
[System.IO.File]::WriteAllBytes($photoFile, [byte[]]@(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46))
$photoUpload = Upload-File "http://localhost:8080/api/v1/resources/upload" $photoFile $tokenOrg
$photoResourceId = $photoUpload.data.id
Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourId/images" @{ resourceId = $photoResourceId; isCover = $true; orderIndex = 0 } $tokenOrg | Out-Null

# TC-PUB-02: Submit for publication with valid data -> PENDING_REVIEW (200)
$p2 = Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourId/publish" $null $tokenOrg
if ($p2.StatusCode -eq 200 -and $p2.Data.data.status -eq "PENDING_REVIEW") {
    Log-Result "TC-PUB-02" "POST /api/v1/tours/{id}/publish" "PASS" "Tour submitted to PENDING_REVIEW"
} else {
    Log-Result "TC-PUB-02" "POST /api/v1/tours/{id}/publish" "FAIL" "Status $($p2.StatusCode): $($p2.Raw)"
}

# TC-PUB-03: Moderator Lists Pending Tours
$p3 = Call-Api "GET" "http://localhost:8080/api/v1/admin/tours/pending" $null $tokenMod
$foundPending = ($p3.Data.data.content | Where-Object { $_.id -eq $tourId }) -ne $null
if ($p3.StatusCode -eq 200 -and $foundPending) {
    Log-Result "TC-PUB-03" "GET /api/v1/admin/tours/pending" "PASS" "Tour found in moderator review queue"
} else {
    Log-Result "TC-PUB-03" "GET /api/v1/admin/tours/pending" "FAIL" "Status $($p3.StatusCode)"
}

# TC-PUB-04: Moderator Rejects Tour with Reason
$p4 = Call-Api "POST" "http://localhost:8080/api/v1/admin/tours/$tourId/review" @{ status = "REJECTED"; rejectionReason = "Description needs more safety gear details" } $tokenMod
if ($p4.StatusCode -eq 200 -and $p4.Data.data.status -eq "REJECTED" -and $p4.Data.data.rejectionReason -eq "Description needs more safety gear details") {
    Log-Result "TC-PUB-04" "POST /api/v1/admin/tours/{id}/review (REJECTED)" "PASS" "Tour rejected with reason"
} else {
    Log-Result "TC-PUB-04" "POST /api/v1/admin/tours/{id}/review (REJECTED)" "FAIL" "Status $($p4.StatusCode): $($p4.Raw)"
}

# Fix and Resubmit
Call-Api "PUT" "http://localhost:8080/api/v1/tours/$tourId" @{ description = "Updated: full certified lifejackets and experienced river guide included." } $tokenOrg | Out-Null
$p4resubmit = Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourId/publish" $null $tokenOrg

# TC-PUB-05: Moderator Approves Tour -> PUBLISHED
$p5 = Call-Api "POST" "http://localhost:8080/api/v1/admin/tours/$tourId/review" @{ status = "PUBLISHED" } $tokenMod
if ($p5.StatusCode -eq 200 -and $p5.Data.data.status -eq "PUBLISHED" -and $p5.Data.data.publishedAt) {
    Log-Result "TC-PUB-05" "POST /api/v1/admin/tours/{id}/review (PUBLISHED)" "PASS" "Tour approved and published at: $($p5.Data.data.publishedAt)"
} else {
    Log-Result "TC-PUB-05" "POST /api/v1/admin/tours/{id}/review (PUBLISHED)" "FAIL" "Status $($p5.StatusCode): $($p5.Raw)"
}

# -------------------------------------------------------------
# DYNAMIC SEARCH TESTS (Aqyly Turaq Specification pattern)
# -------------------------------------------------------------
Write-Host "`n--- Search and Catalog Tests ---" -ForegroundColor Yellow

# TC-SEARCH-01: Public Catalog (Unauthenticated Guest)
$s1 = Call-Api "GET" "http://localhost:8080/api/v1/tours"
$hasOurTour = ($s1.Data.data.content | Where-Object { $_.id -eq $tourId }) -ne $null
if ($s1.StatusCode -eq 200 -and $hasOurTour) {
    Log-Result "TC-SEARCH-01" "GET /api/v1/tours (Public Guest Catalog)" "PASS" "Guest retrieved published tour in catalog"
} else {
    Log-Result "TC-SEARCH-01" "GET /api/v1/tours (Public Guest Catalog)" "FAIL" "Status $($s1.StatusCode)"
}

# TC-SEARCH-02: Search by Query in Title/Description
$s2 = Call-Api "GET" "http://localhost:8080/api/v1/tours?search=rafting"
$matchSearch = ($s2.Data.data.content | Where-Object { $_.id -eq $tourId }) -ne $null
if ($s2.StatusCode -eq 200 -and $matchSearch) {
    Log-Result "TC-SEARCH-02" "Search by text query ('rafting')" "PASS" "Found tour matching 'rafting'"
} else {
    Log-Result "TC-SEARCH-02" "Search by text query ('rafting')" "FAIL" "Status $($s2.StatusCode)"
}

# TC-SEARCH-03: Filter by Country and City
$s3 = Call-Api "GET" "http://localhost:8080/api/v1/tours?country=Kazakhstan&city=Almaty"
$matchGeo = ($s3.Data.data.content | Where-Object { $_.id -eq $tourId }) -ne $null
if ($s3.StatusCode -eq 200 -and $matchGeo) {
    Log-Result "TC-SEARCH-03" "Filter by Country and City" "PASS" "Found tour in Kazakhstan / Almaty"
} else {
    Log-Result "TC-SEARCH-03" "Filter by Country and City" "FAIL" "Status $($s3.StatusCode)"
}

# TC-SEARCH-04: Filter by TourType (COMMUNITY)
$s4 = Call-Api "GET" "http://localhost:8080/api/v1/tours?tourType=COMMUNITY"
$matchType = ($s4.Data.data.content | Where-Object { $_.id -eq $tourId }) -ne $null
if ($s4.StatusCode -eq 200 -and $matchType) {
    Log-Result "TC-SEARCH-04" "Filter by tourType=COMMUNITY" "PASS" "Found community tour"
} else {
    Log-Result "TC-SEARCH-04" "Filter by tourType=COMMUNITY" "FAIL" "Status $($s4.StatusCode)"
}

# TC-SEARCH-05: Filter by Price Range
$s5 = Call-Api "GET" "http://localhost:8080/api/v1/tours?minPrice=20000&maxPrice=50000"
$matchPrice = ($s5.Data.data.content | Where-Object { $_.id -eq $tourId }) -ne $null
if ($s5.StatusCode -eq 200 -and $matchPrice) {
    Log-Result "TC-SEARCH-05" "Filter by Price Range (20000 - 50000)" "PASS" "Tour price 35000 matched range"
} else {
    Log-Result "TC-SEARCH-05" "Filter by Price Range" "FAIL" "Status $($s5.StatusCode)"
}

# TC-SEARCH-06: Non-matching filter returns empty page
$s6 = Call-Api "GET" "http://localhost:8080/api/v1/tours?country=Iceland"
$noMatch = ($s6.Data.data.content | Where-Object { $_.id -eq $tourId }) -eq $null
if ($s6.StatusCode -eq 200 -and $noMatch) {
    Log-Result "TC-SEARCH-06" "Non-matching filter (country=Iceland)" "PASS" "Correctly excluded out-of-scope tour"
} else {
    Log-Result "TC-SEARCH-06" "Non-matching filter" "FAIL" "Status $($s6.StatusCode)"
}

# TC-SEARCH-07: Invalid date range (dateFrom > dateTo) returns 400
$s7 = Call-Api "GET" "http://localhost:8080/api/v1/tours?dateFrom=2026-11-20&dateTo=2026-11-10"
if ($s7.StatusCode -eq 400) {
    Log-Result "TC-SEARCH-07" "Invalid date range validation (dateFrom > dateTo)" "PASS" "Correctly rejected: $($s7.Data.message)"
} else {
    Log-Result "TC-SEARCH-07" "Invalid date range validation" "FAIL" "Status $($s7.StatusCode)"
}

# TC-SEARCH-08: Invalid price range (minPrice > maxPrice) returns 400
$s8 = Call-Api "GET" "http://localhost:8080/api/v1/tours?minPrice=50000&maxPrice=10000"
if ($s8.StatusCode -eq 400) {
    Log-Result "TC-SEARCH-08" "Invalid price range validation (minPrice > maxPrice)" "PASS" "Correctly rejected: $($s8.Data.message)"
} else {
    Log-Result "TC-SEARCH-08" "Invalid price range validation" "FAIL" "Status $($s8.StatusCode)"
}

# TC-PUB-06: Guest View Full Tour Card (Unauthenticated)
$p6 = Call-Api "GET" "http://localhost:8080/api/v1/tours/$tourId"
if ($p6.StatusCode -eq 200 -and $p6.Data.data.id -eq $tourId -and $p6.Data.data.coverImageUrl) {
    Log-Result "TC-PUB-06" "GET /api/v1/tours/{id} (Unauthenticated Guest)" "PASS" "Guest retrieved published tour details with cover: $($p6.Data.data.coverImageUrl)"
} else {
    Log-Result "TC-PUB-06" "GET /api/v1/tours/{id} (Unauthenticated Guest)" "FAIL" "Status $($p6.StatusCode)"
}

# TC-PUB-07: Cancel Published Tour by Organizer
$p7 = Call-Api "POST" "http://localhost:8080/api/v1/tours/$tourId/cancel" @{ reason = "River flood danger alert" } $tokenOrg
if ($p7.StatusCode -eq 200 -and $p7.Data.data.status -eq "CANCELLED" -and $p7.Data.data.cancellationReason -eq "River flood danger alert") {
    Log-Result "TC-PUB-07" "POST /api/v1/tours/{id}/cancel" "PASS" "Tour cancelled with reason saved"
} else {
    Log-Result "TC-PUB-07" "POST /api/v1/tours/{id}/cancel" "FAIL" "Status $($p7.StatusCode)"
}

# TC-PUB-08: Cancelled Tour Excluded from Public Catalog
$p8 = Call-Api "GET" "http://localhost:8080/api/v1/tours"
$excludedFromCatalog = ($p8.Data.data.content | Where-Object { $_.id -eq $tourId }) -eq $null
if ($p8.StatusCode -eq 200 -and $excludedFromCatalog) {
    Log-Result "TC-PUB-08" "Cancelled tour excluded from catalog" "PASS" "Cancelled tour correctly omitted from public search"
} else {
    Log-Result "TC-PUB-08" "Cancelled tour excluded from catalog" "FAIL" "Status $($p8.StatusCode)"
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "             EP-0005 E2E TESTS COMPLETED                  " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
$passed = ($results | Where-Object { $_.Status -eq "PASS" }).Count
$total = $results.Count
Write-Host "Total Tests: $total | Passed: $passed | Failed: $($total - $passed)" -ForegroundColor $(if ($passed -eq $total) { "Green" } else { "Red" })

$results | Format-Table -AutoSize
