# =============================================================================
# Demo cổng LiteLLM của Trợ lý FixLink (chạy ở máy, cần Docker Desktop + LiteLLM đang chạy)
#
#   powershell -ExecutionPolicy Bypass -File litellm\demo-litellm.ps1
#
# Mỗi bước dừng lại chờ bấm Enter để thuyết trình. Tốn khoảng 2 lượt gọi Groq rất ngắn (bước 4 và 5).
# Khóa LITELLM_MASTER_KEY đọc từ file .env, KHÔNG in ra màn hình. Thêm -NoPause để chạy liền một mạch.
# =============================================================================
param([switch]$NoPause)
$ErrorActionPreference = 'Stop'
$Base = 'http://localhost:4000'
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root '.env'
[Console]::OutputEncoding = [Text.Encoding]::UTF8

$MasterKey = $null
if (Test-Path $envFile) {
    foreach ($line in Get-Content $envFile -Encoding UTF8) {
        if ($line -match '^\s*LITELLM_MASTER_KEY\s*=\s*(.+?)\s*$') { $MasterKey = $Matches[1].Trim('"') }
    }
}
if (-not $MasterKey) { $MasterKey = 'sk-fixlink-local' }

function Step($title) {
    Write-Host ''
    Write-Host ('=' * 78) -ForegroundColor DarkGray
    Write-Host $title -ForegroundColor Cyan
    Write-Host ('=' * 78) -ForegroundColor DarkGray
}

function Pause-Demo { if (-not $NoPause) { Read-Host 'Bấm Enter để sang bước tiếp theo' | Out-Null } }

function Call-Chat($body, $useKey = $true) {
    $headers = @{ 'Content-Type' = 'application/json' }
    if ($useKey) { $headers['Authorization'] = "Bearer $MasterKey" }
    $bytes = [Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json -Depth 6))
    return Invoke-WebRequest -Uri "$Base/chat/completions" -Method Post -Headers $headers -Body $bytes -UseBasicParsing -TimeoutSec 60
}

function Show-Answer($resp) {
    $json = [Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray()) | ConvertFrom-Json
    Write-Host ('Model thật đã trả lời : ' + $resp.Headers['x-litellm-model-name']) -ForegroundColor Green
    foreach ($h in 'x-litellm-model-group', 'x-litellm-attempted-fallbacks', 'x-litellm-attempted-retries', 'x-litellm-response-duration-ms', 'x-litellm-response-cost') {
        if ($resp.Headers[$h]) { Write-Host ("{0,-32}: {1}" -f $h, $resp.Headers[$h]) }
    }
    Write-Host ('Trả lời               : ' + $json.choices[0].message.content.Trim())
}

$question = @{ role = 'user'; content = 'Trả lời đúng 1 câu ngắn: FixLink là nền tảng gì?' }
$system = @{ role = 'system'; content = 'FixLink là nền tảng kết nối khách hàng với thợ sửa chữa thiết bị gia dụng tại TP.HCM.' }

Step '1. LiteLLM đang chạy? (GET /health/liveliness)'
$r = Invoke-WebRequest -Uri "$Base/health/liveliness" -UseBasicParsing -TimeoutSec 10
Write-Host ("HTTP {0}: {1}" -f $r.StatusCode, $r.Content) -ForegroundColor Green
Pause-Demo

Step '2. Bảo mật: gọi KHÔNG có khóa master key thì bị chặn'
try {
    Call-Chat @{ model = 'fixlink-chat'; messages = @($question) } $false | Out-Null
    Write-Host 'Không bị chặn (kiểm tra lại master_key trong config.yaml)' -ForegroundColor Yellow
} catch {
    Write-Host ('Bị chặn đúng như mong đợi: HTTP ' + [int]$_.Exception.Response.StatusCode) -ForegroundColor Green
}
Pause-Demo

Step '3. Các model backend được dùng (GET /v1/models): chỉ thấy tên chung, không thấy key Groq'
$m = Invoke-RestMethod -Uri "$Base/v1/models" -Headers @{ Authorization = "Bearer $MasterKey" } -TimeoutSec 10
$m.data | ForEach-Object { Write-Host (' - ' + $_.id) -ForegroundColor Green }
Write-Host 'fixlink-chat      = Groq openai/gpt-oss-120b (model chính, chia tải 2 key)'
Write-Host 'fixlink-groq-qwen = Groq qwen/qwen3.8-27b    (model dự phòng, hạn mức riêng)'
Pause-Demo

Step '4. Gọi bình thường: backend chỉ gửi model "fixlink-chat", LiteLLM chọn key và model thật'
Show-Answer (Call-Chat @{ model = 'fixlink-chat'; max_tokens = 300; messages = @($system, $question) })
Pause-Demo

Step '5. Model chính hỏng thật: nhóm "fixlink-demo-loi" trỏ tới model không tồn tại, LiteLLM TỰ chuyển sang dự phòng'
Write-Host 'Nhà cung cấp báo lỗi -> LiteLLM chuyển sang fixlink-groq-qwen (attempted-fallbacks = 1), người dùng vẫn nhận câu trả lời.'
Show-Answer (Call-Chat @{ model = 'fixlink-demo-loi'; max_tokens = 300; messages = @($system, $question) })
Write-Host ''
Write-Host 'Backend không phải sửa dòng code nào: chuỗi dự phòng nằm trong litellm/config.yaml (router_settings.fallbacks).' -ForegroundColor Cyan
Pause-Demo

Step '6. Cấu hình định tuyến đang dùng (litellm/config.yaml)'
Get-Content (Join-Path $PSScriptRoot 'config.yaml') -Encoding UTF8 |
    Select-String -Pattern '^\s*(- model_name|model:|fallbacks|- fixlink-chat|- fixlink-demo-loi|allowed_fails|cooldown_time|num_retries|request_timeout|drop_params|turn_off_message_logging)' |
    ForEach-Object { Write-Host $_.Line }
Write-Host ''
Write-Host 'Xong. Tiếp theo: mở http://localhost:5173 và hỏi chatbot, nhãn dưới câu trả lời ghi "LiteLLM → tên model".' -ForegroundColor Cyan
