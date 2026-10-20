# EP-0008 End-to-End Test Suite: Chats & Messaging
# Tests Direct 1-on-1 chats, Tour Group chats, participant lifecycle, message history, read status, and block enforcement

$ErrorActionPreference = "Stop"
$BaseUrl = "http://localhost:8080"
$MailpitUrl = "http://localhost:8025"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "       STARTING EP-0008 E2E TEST SUITE: CHATS & MESSAGES   " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$PassedCount = 0
$FailedCount = 0

function Report-Result($testId, $testName, $expected, $actual, $passed) {
    if ($passed) {
        Write-Host " [PASS] $testId - $testName" -ForegroundColor Green
        $script:PassedCount++
    } else {
        Write-Host " [FAIL] $testId - $testName" -ForegroundColor Red
        Write-Host "        Expected: $expected" -ForegroundColor Yellow
        Write-Host "        Actual:   $actual" -ForegroundColor Red
        $script:FailedCount++
    }
}

function Register-And-Verify($email, $password, $nickname, $firstName, $lastName) {
    $regBody = @{
        email = $email
        password = $password
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/register" -Method Post -Body $regBody -ContentType "application/json" | Out-Null
    Start-Sleep -Milliseconds 600

    $messages = Invoke-RestMethod -Uri "$MailpitUrl/api/v1/messages"
    $msg = $messages.messages | Where-Object { $_.To[0].Address -eq $email } | Select-Object -First 1
    $msgDetails = Invoke-RestMethod -Uri "$MailpitUrl/api/v1/message/$($msg.ID)"
    $otp = [regex]::Match($msgDetails.Text, '\b\d{6}\b').Value

    $verifyBody = @{
        email = $email
        code = $otp
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/verify-email" -Method Post -Body $verifyBody -ContentType "application/json" | Out-Null

    $loginBody = @{
        email = $email
        password = $password
    } | ConvertTo-Json
    $loginResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $loginResp.data.accessToken

    $profileBody = @{
        firstName = $firstName
        lastName = $lastName
        nickname = $nickname
    } | ConvertTo-Json
    $headers = @{ Authorization = "Bearer $token" }
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/users/me" -Method Patch -Body $profileBody -ContentType "application/json" -Headers $headers | Out-Null

    $me = Invoke-RestMethod -Uri "$BaseUrl/api/v1/users/me" -Method Get -Headers $headers
    return @{
        Token = $token
        UserId = $me.data.userId
        Email = $email
    }
}

$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
Write-Host "Creating test users Alice, Bob, Charlie..." -ForegroundColor Yellow
$userAlice = Register-And-Verify "chat_alice_$suffix@tourhive.com" "Password123!" "alice_$suffix" "Alice" "Smith"
$userBob = Register-And-Verify "chat_bob_$suffix@tourhive.com" "Password123!" "bob_$suffix" "Bob" "Ivanov"
$userCharlie = Register-And-Verify "chat_charlie_$suffix@tourhive.com" "Password123!" "charlie_$suffix" "Charlie" "Brown"

$headersAlice = @{ Authorization = "Bearer $($userAlice.Token)" }
$headersBob = @{ Authorization = "Bearer $($userBob.Token)" }
$headersCharlie = @{ Authorization = "Bearer $($userCharlie.Token)" }

# TC-1101: Unauthorized direct chat creation
try {
    $body = @{ recipientId = $userBob.UserId } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/direct" -Method Post -Body $body -ContentType "application/json" | Out-Null
    Report-Result "TC-1101" "Create direct chat without auth" "401" "200" $false
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Report-Result "TC-1101" "Create direct chat without auth" "401" "$code" ($code -eq 401)
}

# TC-1102: Create direct chat with self
try {
    $body = @{ recipientId = $userAlice.UserId } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/direct" -Method Post -Body $body -ContentType "application/json" -Headers $headersAlice | Out-Null
    Report-Result "TC-1102" "Create direct chat with self" "400" "200" $false
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Report-Result "TC-1102" "Create direct chat with self" "400" "$code" ($code -eq 400)
}

# TC-1103: Create direct chat between strangers (not friends, no tour relation)
try {
    $body = @{ recipientId = $userBob.UserId } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/direct" -Method Post -Body $body -ContentType "application/json" -Headers $headersAlice | Out-Null
    Report-Result "TC-1103" "Create direct chat between strangers" "403" "200" $false
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Report-Result "TC-1103" "Create direct chat between strangers" "403" "$code" ($code -eq 403)
}

# Make Alice and Bob friends
$reqBody = @{ receiverId = $userBob.UserId; message = "Let us be friends" } | ConvertTo-Json
$reqResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/friends/requests" -Method Post -Body $reqBody -ContentType "application/json" -Headers $headersAlice
$friendReqId = $reqResp.data.id
Invoke-RestMethod -Uri "$BaseUrl/api/v1/friends/requests/$friendReqId/accept" -Method Post -Headers $headersBob | Out-Null

# TC-1104: Create direct chat between friends
$chatResp = $null
try {
    $body = @{ recipientId = $userBob.UserId } | ConvertTo-Json
    $chatResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/direct" -Method Post -Body $body -ContentType "application/json" -Headers $headersAlice
    $isDirect = $chatResp.data.type -eq "DIRECT"
    Report-Result "TC-1104" "Create direct chat between friends" "DIRECT type" "$($chatResp.data.type)" $isDirect
} catch {
    Report-Result "TC-1104" "Create direct chat between friends" "200 OK" "$_" $false
}
$directChatId = $chatResp.data.id

# TC-1105: Idempotent direct chat get-or-create returns same chat ID
try {
    $body = @{ recipientId = $userAlice.UserId } | ConvertTo-Json
    $chatResp2 = Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/direct" -Method Post -Body $body -ContentType "application/json" -Headers $headersBob
    $sameId = $chatResp2.data.id -eq $directChatId
    Report-Result "TC-1105" "Idempotent direct chat lookup" "Same chat ID $directChatId" "$($chatResp2.data.id)" $sameId
} catch {
    Report-Result "TC-1105" "Idempotent direct chat lookup" "200 OK" "$_" $false
}

# TC-1106: Send message from Alice to Bob in direct chat
$msg1Id = $null
try {
    $msgBody = @{ content = "Hello Bob! How are you?"; messageType = "TEXT" } | ConvertTo-Json
    $msgResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/$directChatId/messages" -Method Post -Body $msgBody -ContentType "application/json" -Headers $headersAlice
    $msg1Id = $msgResp.data.id
    $ok = $msgResp.data.content -eq "Hello Bob! How are you?" -and $msgResp.data.isMine -eq $true
    Report-Result "TC-1106" "Send text message in direct chat" "Content matched, isMine=true" "$ok" $ok
} catch {
    Report-Result "TC-1106" "Send text message in direct chat" "201 Created" "$_" $false
}

# TC-1107: Send message with attachment only
try {
    $msgBody = @{ attachmentUrl = "/api/v1/resources/test-image-123.jpg"; messageType = "IMAGE" } | ConvertTo-Json
    $msgResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/$directChatId/messages" -Method Post -Body $msgBody -ContentType "application/json" -Headers $headersAlice
    $ok = $msgResp.data.attachmentUrl -eq "/api/v1/resources/test-image-123.jpg" -and $msgResp.data.messageType -eq "IMAGE"
    Report-Result "TC-1107" "Send image message with attachment only" "IMAGE type with attachmentUrl" "$ok" $ok
} catch {
    Report-Result "TC-1107" "Send image message with attachment only" "201 Created" "$_" $false
}

# TC-1108: Send message with blank content and no attachment
try {
    $msgBody = @{ content = "   "; messageType = "TEXT" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/$directChatId/messages" -Method Post -Body $msgBody -ContentType "application/json" -Headers $headersAlice | Out-Null
    Report-Result "TC-1108" "Send empty message" "400" "200" $false
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Report-Result "TC-1108" "Send empty message" "400" "$code" ($code -eq 400)
}

# TC-1109: Stranger attempts to send message to Alice and Bob's chat
try {
    $msgBody = @{ content = "I am a stranger"; messageType = "TEXT" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/$directChatId/messages" -Method Post -Body $msgBody -ContentType "application/json" -Headers $headersCharlie | Out-Null
    Report-Result "TC-1109" "Stranger sends message to private chat" "403" "200" $false
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Report-Result "TC-1109" "Stranger sends message to private chat" "403" "$code" ($code -eq 403)
}

# TC-1110: Bob views his chat list with unread count
try {
    $myChats = Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats" -Method Get -Headers $headersBob
    $chatItem = $myChats.data.content | Where-Object { $_.id -eq $directChatId }
    $ok = $chatItem.unreadCount -eq 2 -and $chatItem.lastMessage.messageType -eq "IMAGE"
    Report-Result "TC-1110" "List user chats with unreadCount=2" "unreadCount=2" "unreadCount=$($chatItem.unreadCount)" $ok
} catch {
    Report-Result "TC-1110" "List user chats with unreadCount=2" "200 OK" "$_" $false
}

# TC-1111: Bob reads message history
try {
    $history = Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/$directChatId/messages" -Method Get -Headers $headersBob
    $count = $history.data.content.Count
    Report-Result "TC-1111" "Get chat message history" "2 messages" "$count messages" ($count -eq 2)
} catch {
    Report-Result "TC-1111" "Get chat message history" "200 OK" "$_" $false
}

# TC-1112: Bob marks messages as read
try {
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/$directChatId/read" -Method Post -Body "{}" -ContentType "application/json" -Headers $headersBob | Out-Null
    Report-Result "TC-1112" "Mark messages as read" "200 OK" "200 OK" $true
} catch {
    Report-Result "TC-1112" "Mark messages as read" "200 OK" "$_" $false
}

# TC-1113: Bob verifies unread count is now 0
try {
    $myChats = Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats" -Method Get -Headers $headersBob
    $chatItem = $myChats.data.content | Where-Object { $_.id -eq $directChatId }
    $ok = $chatItem.unreadCount -eq 0
    Report-Result "TC-1113" "Verify unreadCount is now 0 after read" "unreadCount=0" "unreadCount=$($chatItem.unreadCount)" $ok
} catch {
    Report-Result "TC-1113" "Verify unreadCount is now 0 after read" "200 OK" "$_" $false
}

# TC-1114: View chat details and member list
try {
    $details = Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/$directChatId" -Method Get -Headers $headersAlice
    $memberCount = $details.data.members.Count
    Report-Result "TC-1114" "Get chat details and members" "2 members" "$memberCount members" ($memberCount -eq 2)
} catch {
    Report-Result "TC-1114" "Get chat details and members" "200 OK" "$_" $false
}

# ================= TOUR GROUP CHAT SCENARIOS =================
Write-Host "Setting up Tour for Tour Group Chat testing..." -ForegroundColor Yellow

# Activate Bob as organizer and create/publish tour
Invoke-RestMethod -Uri "$BaseUrl/api/v1/organizers/me/activate" -Method Post -Headers $headersBob | Out-Null

$tourDraftBody = @{
    title = "Alatau Lake Trekking Tour"
    description = "Wonderful scenic mountain tour"
    tourType = "COMMUNITY"
    startDate = [DateTime]::UtcNow.AddDays(10).ToString("yyyy-MM-dd")
    endDate = [DateTime]::UtcNow.AddDays(14).ToString("yyyy-MM-dd")
    capacity = 5
    price = 25000
    currency = "KZT"
    countries = @("Kazakhstan")
    cities = @("Almaty")
    includes = @("Transfer", "Guide")
    routeItems = @(
        @{ dayNumber = 1; title = "Meeting"; description = "Gathering in city"; orderIndex = 0 }
    )
} | ConvertTo-Json -Depth 5

$draftResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/tours/draft" -Method Post -Body $tourDraftBody -ContentType "application/json" -Headers $headersBob
$tourId = $draftResp.data.id

# Upload photo & attach as cover
$dummyImgBytes = [byte[]](0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46)
$tempImgPath = "$PSScriptRoot\temp_tour_cover.jpg"
[System.IO.File]::WriteAllBytes($tempImgPath, $dummyImgBytes)

$uploadOutput = powershell -Command "curl.exe -s -X POST '$BaseUrl/api/v1/resources/upload' -H 'Authorization: Bearer $($userBob.Token)' -F 'file=@$tempImgPath'"
$uploadJson = $uploadOutput | ConvertFrom-Json
$resourceId = $uploadJson.data.id

$attachBody = @{
    resourceId = $resourceId
    isCover = $true
    orderIndex = 0
} | ConvertTo-Json
Invoke-RestMethod -Uri "$BaseUrl/api/v1/tours/$tourId/images" -Method Post -Body $attachBody -ContentType "application/json" -Headers $headersBob | Out-Null

# Submit for moderation
Invoke-RestMethod -Uri "$BaseUrl/api/v1/tours/$tourId/publish" -Method Post -Headers $headersBob | Out-Null

# Approve by Admin
$adminUser = Register-And-Verify "chat_admin_$suffix@tourhive.com" "Password123!" "admin_$suffix" "Admin" "Platform"
& docker exec tourhive-postgres psql -U tourhive_user -d tourhive_db -c "UPDATE users SET role = 'ADMIN' WHERE id = '$($adminUser.UserId)';" | Out-Null

# Re-login admin to refresh token with role
$adminLoginBody = @{ email = $adminUser.Email; password = "Password123!" } | ConvertTo-Json
$loginResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -Body $adminLoginBody -ContentType "application/json"
$adminHeaders = @{ Authorization = "Bearer $($loginResp.data.accessToken)" }

$reviewTourBody = @{ status = "PUBLISHED" } | ConvertTo-Json
Invoke-RestMethod -Uri "$BaseUrl/api/v1/admin/tours/$tourId/review" -Method Post -Body $reviewTourBody -ContentType "application/json" -Headers $adminHeaders | Out-Null

# TC-1115: Organizer accesses tour group chat
$tourChatResp = $null
try {
    $tourChatResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/tours/$tourId" -Method Get -Headers $headersBob
    $ok = $tourChatResp.data.type -eq "TOUR_GROUP" -and $tourChatResp.data.tourTitle -eq "Alatau Lake Trekking Tour"
    Report-Result "TC-1115" "Organizer accesses tour group chat" "TOUR_GROUP" "$($tourChatResp.data.type)" $ok
} catch {
    Report-Result "TC-1115" "Organizer accesses tour group chat" "200 OK" "$_" $false
}
$tourChatId = $tourChatResp.data.id

# TC-1116: Stranger tries to access tour group chat
try {
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/tours/$tourId" -Method Get -Headers $headersCharlie | Out-Null
    Report-Result "TC-1116" "Stranger accesses tour group chat" "403" "200" $false
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Report-Result "TC-1116" "Stranger accesses tour group chat" "403" "$code" ($code -eq 403)
}

# Alice applies for tour
$applyBody = @{ message = "I want to join!"; contactInfo = "+77011234567" } | ConvertTo-Json
$appResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/tours/$tourId/applications" -Method Post -Body $applyBody -ContentType "application/json" -Headers $headersAlice
$appId = $appResp.data.id

# Organizer Bob accepts Alice's application
$reviewAppBody = @{ status = "ACCEPTED" } | ConvertTo-Json
Invoke-RestMethod -Uri "$BaseUrl/api/v1/tours/$tourId/applications/$appId/review" -Method Post -Body $reviewAppBody -ContentType "application/json" -Headers $headersBob | Out-Null

# TC-1117: Accepted participant Alice accesses tour group chat
try {
    $tourChatAlice = Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/tours/$tourId" -Method Get -Headers $headersAlice
    $ok = $tourChatAlice.data.id -eq $tourChatId
    Report-Result "TC-1117" "Accepted participant accesses tour group chat" "Tour chat $tourChatId" "$($tourChatAlice.data.id)" $ok
} catch {
    Report-Result "TC-1117" "Accepted participant accesses tour group chat" "200 OK" "$_" $false
}

# TC-1118: Alice sends message in tour group chat
try {
    $msgBody = @{ content = "Hello everyone, excited to join!"; messageType = "TEXT" } | ConvertTo-Json
    $msgResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/$tourChatId/messages" -Method Post -Body $msgBody -ContentType "application/json" -Headers $headersAlice
    $ok = $msgResp.data.content -eq "Hello everyone, excited to join!"
    Report-Result "TC-1118" "Participant sends message in tour group chat" "Message sent" "$ok" $ok
} catch {
    Report-Result "TC-1118" "Participant sends message in tour group chat" "201 Created" "$_" $false
}

# TC-1119: Alice cancels participation
try {
    $cancelBody = @{ reason = "Plans changed" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/tours/$tourId/applications/$appId/cancel" -Method Post -Body $cancelBody -ContentType "application/json" -Headers $headersAlice | Out-Null
    Report-Result "TC-1119" "Participant cancels application" "200 OK" "200 OK" $true
} catch {
    Report-Result "TC-1119" "Participant cancels application" "200 OK" "$_" $false
}

# Organizer sends an update message in the tour chat AFTER Alice left
$orgMsgBody = @{ content = "New update for remaining participants"; messageType = "TEXT" } | ConvertTo-Json
Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/$tourChatId/messages" -Method Post -Body $orgMsgBody -ContentType "application/json" -Headers $headersBob | Out-Null

# TC-1120: Former participant tries to send new message in tour group chat
try {
    $msgBody = @{ content = "Can I still send something?"; messageType = "TEXT" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/$tourChatId/messages" -Method Post -Body $msgBody -ContentType "application/json" -Headers $headersAlice | Out-Null
    Report-Result "TC-1120" "Former participant tries to send message" "403" "200" $false
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Report-Result "TC-1120" "Former participant tries to send message" "403" "$code" ($code -eq 403)
}

# TC-1121: Former participant views messages cutoff at leftAt
try {
    $aliceHistory = Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/$tourChatId/messages" -Method Get -Headers $headersAlice
    $hasNewMessage = $aliceHistory.data.content | Where-Object { $_.content -like "*New update for remaining participants*" }
    $ok = ($hasNewMessage -eq $null) -and ($aliceHistory.data.content.Count -gt 0)
    Report-Result "TC-1121" "Former participant messages cut off after leftAt" "New message excluded" "$ok" $ok
} catch {
    Report-Result "TC-1121" "Former participant messages cut off after leftAt" "200 OK" "$_" $false
}

# TC-1122: Direct chat blocking: Alice blocks Bob
Invoke-RestMethod -Uri "$BaseUrl/api/v1/users/$($userBob.UserId)/block" -Method Post -Headers $headersAlice | Out-Null
try {
    $msgBody = @{ content = "Alice, why did you block me?"; messageType = "TEXT" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/$directChatId/messages" -Method Post -Body $msgBody -ContentType "application/json" -Headers $headersBob | Out-Null
    Report-Result "TC-1122" "Blocked user cannot send message in direct chat" "403" "200" $false
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Report-Result "TC-1122" "Blocked user cannot send message in direct chat" "403" "$code" ($code -eq 403)
}

# TC-1123: User cannot send message to user she blocked
try {
    $msgBody = @{ content = "I blocked you"; messageType = "TEXT" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/chats/$directChatId/messages" -Method Post -Body $msgBody -ContentType "application/json" -Headers $headersAlice | Out-Null
    Report-Result "TC-1123" "Blocker cannot send message in direct chat" "403" "200" $false
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Report-Result "TC-1123" "Blocker cannot send message in direct chat" "403" "$code" ($code -eq 403)
}

# Cleanup temp image
if (Test-Path $tempImgPath) {
    Remove-Item $tempImgPath -Force
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "       EP-0008 TEST SUITE SUMMARY: $PassedCount PASSED, $FailedCount FAILED" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

if ($FailedCount -gt 0) {
    exit 1
} else {
    exit 0
}
