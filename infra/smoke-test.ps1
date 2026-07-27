$ErrorActionPreference = 'Stop'
$tokenResponse = Invoke-RestMethod -Method Post -Uri 'http://localhost:8180/realms/monitoring/protocol/openid-connect/token' -ContentType 'application/x-www-form-urlencoded' -Body @{ client_id = 'monitoring-app'; username = 'demo'; password = 'demo'; grant_type = 'password' }
$headers = @{ Authorization = "Bearer $($tokenResponse.access_token)" }
$health = Invoke-RestMethod -Uri 'http://localhost:8082/api/system/health' -Headers $headers
if ($health.status -notin @('UP', 'DEGRADED')) { throw "Unexpected health status: $($health.status)" }
$event = @{ serviceName = 'smoke-test'; eventType = 'HTTP_REQUEST'; payload = @{ method = 'GET'; endpoint = '/smoke'; statusCode = 503; latencyMs = 3000 } } | ConvertTo-Json -Depth 4
$accepted = Invoke-RestMethod -Method Post -Uri 'http://localhost:8082/api/events' -Headers $headers -ContentType 'application/json' -Body $event
if (-not $accepted.eventId) { throw 'Event submission did not return an eventId' }
Write-Host "Smoke test passed. Event $($accepted.eventId) accepted; platform status $($health.status)."
