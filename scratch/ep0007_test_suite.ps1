# EP-0007 Friends & Favorites E2E Test Suite
$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080"
$mailpitUrl = "http://localhost:8025/api/v1/messages"

$totalTests = 0
$passedTests = 0
$failedTests = 0

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

function Run-Test {
    param(
        [string]$TestId,
        [string]$Description,
        [scriptblock]$Action
    )
    $script:totalTests++
    Write-Host -NoNewline "[$TestId] $Description ... "
    try {
        & $Action
        $script:passedTests++
        Write-Host -ForegroundColor Green "PASS"
    } catch {
        $script:failedTests++
        Write-Host -ForegroundColor Red "FAIL: $_"
    }
}

function Wait-For-Server {
    Write-Host "Waiting for server to become healthy..."
    for ($i = 0; $i -lt 30; $i++) {
        try {
            $resp = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get -TimeoutSec 3
            if ($resp.status -eq "UP") {
                Write-Host -ForegroundColor Green "Server is healthy!"
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    throw "Server failed to start within timeout"
}

Wait-For-Server

$uniqueId = [System.Guid]::NewGuid().ToString().Substring(0, 8)
$aliceEmail = "alice_$uniqueId@tourhive.com"
$bobEmail = "bob_$uniqueId@tourhive.com"
$charlieEmail = "charlie_$uniqueId@tourhive.com"
$password = "Secret123!"

function Register-User($email, $isOrganizer = $false) {
    $res = Call-Api "POST" "$baseUrl/api/v1/auth/register" @{ email = $email; password = $password; isOrganizer = $isOrganizer }
    if ($res.StatusCode -ne 201) { throw "Registration failed for $($email): $($res.Raw)" }
}

function Get-Verification-Code($email) {
    for ($attempt = 0; $attempt -lt 15; $attempt++) {
        Start-Sleep -Seconds 1
        $messages = Invoke-RestMethod -Uri $mailpitUrl -Method Get
        foreach ($msg in $messages.messages) {
            if ($msg.To[0].Address -eq $email) {
                if ($msg.Snippet -match "\b(\d{6})\b") {
                    return $matches[1]
                }
            }
        }
    }
    throw "Verification code not found in Mailpit for $email"
}

function Verify-User($email, $code) {
    $res = Call-Api "POST" "$baseUrl/api/v1/auth/verify-email" @{ email = $email; code = $code }
    if ($res.StatusCode -ne 200) { throw "Verification failed for $($email): $($res.Raw)" }
}

function Login-User($email) {
    $res = Call-Api "POST" "$baseUrl/api/v1/auth/login" @{ email = $email; password = $password }
    if ($res.StatusCode -ne 200) { throw "Login failed for $($email): $($res.Raw)" }
    return $res.Data.data.accessToken
}

Write-Host "Registering and verifying test users..."
Register-User $aliceEmail
$aliceCode = Get-Verification-Code $aliceEmail
Verify-User $aliceEmail $aliceCode
$aliceToken = Login-User $aliceEmail

Register-User $bobEmail
$bobCode = Get-Verification-Code $bobEmail
Verify-User $bobEmail $bobCode
$bobToken = Login-User $bobEmail

Register-User $charlieEmail $true
$charlieCode = Get-Verification-Code $charlieEmail
Verify-User $charlieEmail $charlieCode
$charlieToken = Login-User $charlieEmail

$aliceMe = Call-Api "GET" "$baseUrl/api/v1/users/me" $null $aliceToken
$aliceId = $aliceMe.Data.data.userId

$bobMe = Call-Api "GET" "$baseUrl/api/v1/users/me" $null $bobToken
$bobId = $bobMe.Data.data.userId

$charlieMe = Call-Api "GET" "$baseUrl/api/v1/users/me" $null $charlieToken
$charlieId = $charlieMe.Data.data.userId

Write-Host "Alice ID: $aliceId | Bob ID: $bobId | Charlie ID: $charlieId"

# Update profiles
Call-Api "PATCH" "$baseUrl/api/v1/users/me" @{ nickname = "alice_$uniqueId"; firstName = "Alice"; lastName = "Smith" } $aliceToken | Out-Null
Call-Api "PATCH" "$baseUrl/api/v1/users/me" @{ nickname = "bob_$uniqueId"; firstName = "Bob"; lastName = "Jones" } $bobToken | Out-Null
Call-Api "PATCH" "$baseUrl/api/v1/users/me" @{ nickname = "charlie_$uniqueId"; firstName = "Charlie"; lastName = "Guide" } $charlieToken | Out-Null

# Charlie activates organizer
Call-Api "POST" "$baseUrl/api/v1/organizers/me/activate" $null $charlieToken | Out-Null

# Charlie creates published and draft tours
$tourBody = @{
    title = "Kaindy Lake Adventure $uniqueId"
    description = "Sunken forest alpine expedition"
    tourType = "COMMUNITY"
    startDate = (Get-Date).AddDays(10).ToString("yyyy-MM-dd")
    endDate = (Get-Date).AddDays(12).ToString("yyyy-MM-dd")
    capacity = 10
    price = 25000
    currency = "KZT"
    countries = @("Kazakhstan")
    cities = @("Almaty")
    includes = @("Transport", "Tent")
}

$tourRes = Call-Api "POST" "$baseUrl/api/v1/tours/draft" $tourBody $charlieToken
$publishedTourId = $tourRes.Data.data.id

# Publish the tour directly in DB
& docker exec tourhive-postgres psql -U tourhive_user -d tourhive_db -c "UPDATE tours SET status = 'PUBLISHED', published_at = NOW() WHERE id = '$publishedTourId';" | Out-Null

# Create a draft tour
$draftTourBody = @{
    title = "Draft Tour $uniqueId"
    tourType = "COMMUNITY"
    startDate = (Get-Date).AddDays(20).ToString("yyyy-MM-dd")
    endDate = (Get-Date).AddDays(22).ToString("yyyy-MM-dd")
    capacity = 5
}

$draftRes = Call-Api "POST" "$baseUrl/api/v1/tours/draft" $draftTourBody $charlieToken
$draftTourId = $draftRes.Data.data.id

Write-Host "`n--- Group 10: Friends & Friend Requests ---"

Run-Test "TC-1001" "Reject self friend request with 400 CANNOT_BEFRIEND_SELF" {
    $res = Call-Api "POST" "$baseUrl/api/v1/friends/requests" @{ receiverId = $aliceId; message = "Hi self" } $aliceToken
    if ($res.StatusCode -ne 400 -or $res.Data.code -ne "CANNOT_BEFRIEND_SELF") {
        throw "Expected 400 CANNOT_BEFRIEND_SELF, got $($res.StatusCode) - $($res.Raw)"
    }
}

$script:reqId = $null
Run-Test "TC-1002" "Alice sends friend request to Bob" {
    $res = Call-Api "POST" "$baseUrl/api/v1/friends/requests" @{ receiverId = $bobId; message = "Let's be friends!" } $aliceToken
    if ($res.StatusCode -ne 201) { throw "Expected 201, got $($res.StatusCode) - $($res.Raw)" }
    if ($res.Data.data.status -ne "PENDING") { throw "Status was not PENDING: $($res.Data.data.status)" }
    if ($res.Data.data.receiverId -ne $bobId) { throw "ReceiverId mismatch" }
    $script:reqId = $res.Data.data.id
}

Run-Test "TC-1003" "Reject duplicate outgoing friend request with 409 DUPLICATE_FRIEND_REQUEST" {
    $res = Call-Api "POST" "$baseUrl/api/v1/friends/requests" @{ receiverId = $bobId; message = "Second request" } $aliceToken
    if ($res.StatusCode -ne 409 -or $res.Data.code -ne "DUPLICATE_FRIEND_REQUEST") {
        throw "Expected 409 DUPLICATE_FRIEND_REQUEST, got $($res.StatusCode) - $($res.Raw)"
    }
}

Run-Test "TC-1004" "Reject reverse friend request with 409 INCOMING_REQUEST_ALREADY_EXISTS" {
    $res = Call-Api "POST" "$baseUrl/api/v1/friends/requests" @{ receiverId = $aliceId; message = "Bob asks Alice" } $bobToken
    if ($res.StatusCode -ne 409 -or $res.Data.code -ne "INCOMING_REQUEST_ALREADY_EXISTS") {
        throw "Expected 409 INCOMING_REQUEST_ALREADY_EXISTS, got $($res.StatusCode) - $($res.Raw)"
    }
}

Run-Test "TC-1005" "Bob sees Alice's request in incoming requests" {
    $res = Call-Api "GET" "$baseUrl/api/v1/friends/requests/incoming" $null $bobToken
    if ($res.StatusCode -ne 200) { throw "Expected 200, got $($res.StatusCode)" }
    $found = $res.Data.data.content | Where-Object { $_.id -eq $script:reqId }
    if (-not $found) { throw "Request not found in incoming" }
}

Run-Test "TC-1006" "Alice sees her request in outgoing requests" {
    $res = Call-Api "GET" "$baseUrl/api/v1/friends/requests/outgoing" $null $aliceToken
    if ($res.StatusCode -ne 200) { throw "Expected 200, got $($res.StatusCode)" }
    $found = $res.Data.data.content | Where-Object { $_.id -eq $script:reqId }
    if (-not $found) { throw "Request not found in outgoing" }
}

Run-Test "TC-1007" "Alice cancels her friend request" {
    $res = Call-Api "POST" "$baseUrl/api/v1/friends/requests/$($script:reqId)/cancel" $null $aliceToken
    if ($res.StatusCode -ne 200 -or $res.Data.data.status -ne "CANCELLED") {
        throw "Expected 200 CANCELLED, got $($res.StatusCode) - $($res.Raw)"
    }
}

Run-Test "TC-1008" "Friendship status after cancel is NONE" {
    $res = Call-Api "GET" "$baseUrl/api/v1/friends/$bobId/status" $null $aliceToken
    if ($res.StatusCode -ne 200 -or $res.Data.data.status -ne "NONE") {
        throw "Expected 200 NONE, got $($res.StatusCode) - $($res.Raw)"
    }
}

Run-Test "TC-1009" "Alice sends new friend request to Bob" {
    $res = Call-Api "POST" "$baseUrl/api/v1/friends/requests" @{ receiverId = $bobId; message = "Trying again!" } $aliceToken
    if ($res.StatusCode -ne 201 -or $res.Data.data.status -ne "PENDING") {
        throw "Expected 201 PENDING, got $($res.StatusCode) - $($res.Raw)"
    }
    $script:reqId = $res.Data.data.id
}

Run-Test "TC-1010" "Bob declines friend request" {
    $res = Call-Api "POST" "$baseUrl/api/v1/friends/requests/$($script:reqId)/decline" $null $bobToken
    if ($res.StatusCode -ne 200 -or $res.Data.data.status -ne "DECLINED") {
        throw "Expected 200 DECLINED, got $($res.StatusCode) - $($res.Raw)"
    }
}

Run-Test "TC-1011" "Friendship status after decline is NONE" {
    $res = Call-Api "GET" "$baseUrl/api/v1/friends/$bobId/status" $null $aliceToken
    if ($res.StatusCode -ne 200 -or $res.Data.data.status -ne "NONE") {
        throw "Expected 200 NONE, got $($res.StatusCode) - $($res.Raw)"
    }
}

Run-Test "TC-1012" "Alice sends third friend request to Bob" {
    $res = Call-Api "POST" "$baseUrl/api/v1/friends/requests" @{ receiverId = $bobId; message = "Third time's the charm!" } $aliceToken
    if ($res.StatusCode -ne 201 -or $res.Data.data.status -ne "PENDING") {
        throw "Expected 201 PENDING, got $($res.StatusCode) - $($res.Raw)"
    }
    $script:reqId = $res.Data.data.id
}

Run-Test "TC-1013" "Reject unauthorized user (Charlie) accepting request with 403" {
    $res = Call-Api "POST" "$baseUrl/api/v1/friends/requests/$($script:reqId)/accept" $null $charlieToken
    if ($res.StatusCode -ne 403 -or $res.Data.code -ne "FRIEND_REQUEST_ACCESS_DENIED") {
        throw "Expected 403 FRIEND_REQUEST_ACCESS_DENIED, got $($res.StatusCode) - $($res.Raw)"
    }
}

Run-Test "TC-1014" "Bob accepts friend request successfully" {
    $res = Call-Api "POST" "$baseUrl/api/v1/friends/requests/$($script:reqId)/accept" $null $bobToken
    if ($res.StatusCode -ne 200 -or $res.Data.data.status -ne "ACCEPTED") {
        throw "Expected 200 ACCEPTED, got $($res.StatusCode) - $($res.Raw)"
    }
}

Run-Test "TC-1015" "Friendship status from Alice to Bob is FRIENDS" {
    $res = Call-Api "GET" "$baseUrl/api/v1/friends/$bobId/status" $null $aliceToken
    if ($res.StatusCode -ne 200 -or $res.Data.data.status -ne "FRIENDS") {
        throw "Expected 200 FRIENDS, got $($res.StatusCode) - $($res.Raw)"
    }
}

Run-Test "TC-1016" "Friendship status from Bob to Alice is FRIENDS" {
    $res = Call-Api "GET" "$baseUrl/api/v1/friends/$aliceId/status" $null $bobToken
    if ($res.StatusCode -ne 200 -or $res.Data.data.status -ne "FRIENDS") {
        throw "Expected 200 FRIENDS, got $($res.StatusCode) - $($res.Raw)"
    }
}

Run-Test "TC-1017" "Alice views friends list containing Bob" {
    $res = Call-Api "GET" "$baseUrl/api/v1/friends" $null $aliceToken
    if ($res.StatusCode -ne 200) { throw "Expected 200, got $($res.StatusCode)" }
    $found = $res.Data.data.content | Where-Object { $_.userId -eq $bobId }
    if (-not $found) { throw "Bob not found in Alice's friends list" }
}

Run-Test "TC-1018" "Bob views friends list containing Alice" {
    $res = Call-Api "GET" "$baseUrl/api/v1/friends" $null $bobToken
    if ($res.StatusCode -ne 200) { throw "Expected 200, got $($res.StatusCode)" }
    $found = $res.Data.data.content | Where-Object { $_.userId -eq $aliceId }
    if (-not $found) { throw "Alice not found in Bob's friends list" }
}

Run-Test "TC-1019" "Reject friend request when already friends with 409 ALREADY_FRIENDS" {
    $res = Call-Api "POST" "$baseUrl/api/v1/friends/requests" @{ receiverId = $bobId } $aliceToken
    if ($res.StatusCode -ne 409 -or $res.Data.code -ne "ALREADY_FRIENDS") {
        throw "Expected 409 ALREADY_FRIENDS, got $($res.StatusCode) - $($res.Raw)"
    }
}

Run-Test "TC-1020" "Bob blocks Alice and dissolves friendship" {
    $res = Call-Api "POST" "$baseUrl/api/v1/users/$aliceId/block" $null $bobToken
    if ($res.StatusCode -ne 200) { throw "Expected 200, got $($res.StatusCode) - $($res.Raw)" }
}

Run-Test "TC-1021" "Friendship status after block is BLOCKED" {
    $res = Call-Api "GET" "$baseUrl/api/v1/friends/$bobId/status" $null $aliceToken
    if ($res.StatusCode -ne 200 -or $res.Data.data.status -ne "BLOCKED") {
        throw "Expected 200 BLOCKED, got $($res.StatusCode) - $($res.Raw)"
    }
}

Run-Test "TC-1022" "Reject friend request when blocked with 403 USER_BLOCKED" {
    $res = Call-Api "POST" "$baseUrl/api/v1/friends/requests" @{ receiverId = $bobId } $aliceToken
    if ($res.StatusCode -ne 403 -or $res.Data.code -ne "USER_BLOCKED") {
        throw "Expected 403 USER_BLOCKED, got $($res.StatusCode) - $($res.Raw)"
    }
}

Run-Test "TC-1023" "Bob unblocks Alice" {
    $res = Call-Api "DELETE" "$baseUrl/api/v1/users/$aliceId/block" $null $bobToken
    if ($res.StatusCode -ne 204) { throw "Expected 204, got $($res.StatusCode)" }
}

Run-Test "TC-1024" "Status after unblock is NONE" {
    $res = Call-Api "GET" "$baseUrl/api/v1/friends/$bobId/status" $null $aliceToken
    if ($res.StatusCode -ne 200 -or $res.Data.data.status -ne "NONE") {
        throw "Expected 200 NONE, got $($res.StatusCode) - $($res.Raw)"
    }
}

Run-Test "TC-1025" "Re-establish friendship (request and accept)" {
    $req = Call-Api "POST" "$baseUrl/api/v1/friends/requests" @{ receiverId = $bobId; message = "Friends again?" } $aliceToken
    Call-Api "POST" "$baseUrl/api/v1/friends/requests/$($req.Data.data.id)/accept" $null $bobToken | Out-Null
    $res = Call-Api "GET" "$baseUrl/api/v1/friends/$bobId/status" $null $aliceToken
    if ($res.StatusCode -ne 200 -or $res.Data.data.status -ne "FRIENDS") {
        throw "Expected 200 FRIENDS, got $($res.StatusCode) - $($res.Raw)"
    }
}

Run-Test "TC-1026" "Alice unfriends Bob" {
    $res = Call-Api "DELETE" "$baseUrl/api/v1/friends/$bobId" $null $aliceToken
    if ($res.StatusCode -ne 204) { throw "Expected 204, got $($res.StatusCode)" }
}

Run-Test "TC-1027" "Friendship status after unfriend is NONE" {
    $res = Call-Api "GET" "$baseUrl/api/v1/friends/$bobId/status" $null $aliceToken
    if ($res.StatusCode -ne 200 -or $res.Data.data.status -ne "NONE") {
        throw "Expected 200 NONE, got $($res.StatusCode) - $($res.Raw)"
    }
}

Write-Host "`n--- Group 11: Favorites (Wishlist) ---"

Run-Test "TC-1028" "Check favorite status before adding is false" {
    $res = Call-Api "GET" "$baseUrl/api/v1/favorites/tours/$publishedTourId/check" $null $aliceToken
    if ($res.StatusCode -ne 200 -or $res.Data.data.isFavorite -ne $false) {
        throw "Expected false, got $($res.Raw)"
    }
}

Run-Test "TC-1029" "Alice adds published tour to favorites" {
    $res = Call-Api "POST" "$baseUrl/api/v1/favorites/tours/$publishedTourId" $null $aliceToken
    if ($res.StatusCode -ne 201 -or $res.Data.message -ne "Tour added to favorites successfully") {
        throw "Expected 201, got $($res.StatusCode) - $($res.Raw)"
    }
}

Run-Test "TC-1030" "Check favorite status after adding is true" {
    $res = Call-Api "GET" "$baseUrl/api/v1/favorites/tours/$publishedTourId/check" $null $aliceToken
    if ($res.StatusCode -ne 200 -or $res.Data.data.isFavorite -ne $true) {
        throw "Expected true, got $($res.Raw)"
    }
}

Run-Test "TC-1031" "Reject duplicate favorite addition with 409 DUPLICATE_FAVORITE" {
    $res = Call-Api "POST" "$baseUrl/api/v1/favorites/tours/$publishedTourId" $null $aliceToken
    if ($res.StatusCode -ne 409 -or $res.Data.code -ne "DUPLICATE_FAVORITE") {
        throw "Expected 409 DUPLICATE_FAVORITE, got $($res.StatusCode) - $($res.Raw)"
    }
}

Run-Test "TC-1032" "Reject adding draft tour to favorites with 400 INVALID_TOUR_DATA" {
    $res = Call-Api "POST" "$baseUrl/api/v1/favorites/tours/$draftTourId" $null $aliceToken
    if ($res.StatusCode -ne 400 -or $res.Data.code -ne "INVALID_TOUR_DATA") {
        throw "Expected 400 INVALID_TOUR_DATA, got $($res.StatusCode) - $($res.Raw)"
    }
}

Run-Test "TC-1033" "Alice lists favorite tours" {
    $res = Call-Api "GET" "$baseUrl/api/v1/favorites/tours" $null $aliceToken
    if ($res.StatusCode -ne 200) { throw "Expected 200, got $($res.StatusCode)" }
    if ($res.Data.data.content.Count -lt 1) { throw "Favorite tours list empty" }
    if ($res.Data.data.content[0].id -ne $publishedTourId) { throw "Tour ID mismatch in favorites" }
}

Run-Test "TC-1034" "Alice removes tour from favorites" {
    $res = Call-Api "DELETE" "$baseUrl/api/v1/favorites/tours/$publishedTourId" $null $aliceToken
    if ($res.StatusCode -ne 204) { throw "Expected 204, got $($res.StatusCode)" }
}

Run-Test "TC-1035" "Check favorite status after removal is false" {
    $res = Call-Api "GET" "$baseUrl/api/v1/favorites/tours/$publishedTourId/check" $null $aliceToken
    if ($res.StatusCode -ne 200 -or $res.Data.data.isFavorite -ne $false) {
        throw "Expected false, got $($res.Raw)"
    }
}

Run-Test "TC-1036" "Reject removing non-favorite tour with 404 FAVORITE_NOT_FOUND" {
    $res = Call-Api "DELETE" "$baseUrl/api/v1/favorites/tours/$publishedTourId" $null $aliceToken
    if ($res.StatusCode -ne 404 -or $res.Data.code -ne "FAVORITE_NOT_FOUND") {
        throw "Expected 404 FAVORITE_NOT_FOUND, got $($res.StatusCode) - $($res.Raw)"
    }
}

Write-Host "`n=========================================="
Write-Host "E2E Test Results: Total: $totalTests | Passed: $passedTests | Failed: $failedTests"
Write-Host "=========================================="

if ($failedTests -gt 0) {
    exit 1
} else {
    exit 0
}
