<#
.SYNOPSIS
    Script giữ ấm máy chủ Render & Database Neon phục vụ buổi Demo Đồ án FixLink.
    Đảm bảo Web Service luôn ở trạng thái ACTIVE trên RAM, loại bỏ hoàn toàn Cold Start (độ trễ 50-90s).

.DESCRIPTION
    Script tự động gửi HTTP GET request định kỳ tới endpoint Health Check của Backend.
    Mặc định mỗi 3 phút (180 giây) gửi 1 request, ngăn chặn Render Free Tier rơi vào trạng thái ngủ đông (Sleep sau 15 phút).

.EXAMPLE
    .\keep_alive_render.ps1
    .\keep_alive_render.ps1 -IntervalSeconds 120
    .\keep_alive_render.ps1 -Url "https://fixlink-api.onrender.com/api/v1/health"
#>

param(
    [string]$Url = "https://fixlink-api.onrender.com/api/v1/health",
    [int]$IntervalSeconds = 180
)

$Host.UI.RawUI.WindowTitle = "FixLink - Render Keep-Alive Monitor (Demo Ready)"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "   FIXLINK RENDER & NEON KEEP-ALIVE SYSTEM - DEMO BOOSTER       " -ForegroundColor Yellow
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "🎯 Mục tiêu: Giữ backend Render & Neon luôn thức 100% trên RAM"
Write-Host "🔗 Target URL: $Url" -ForegroundColor Green
Write-Host "⏱️ Chu kỳ ping: Mỗi $IntervalSeconds giây (Render ngủ sau 15 phút không có request)"
Write-Host "💡 Nhấn Ctrl + C để dừng script khi kết thúc buổi demo." -ForegroundColor Gray
Write-Host "-----------------------------------------------------------------"

$count = 0

while ($true) {
    $count++
    $timestamp = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    
    try {
        $response = Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec 30 -ErrorAction Stop
        $sw.Stop()
        $latency = $sw.ElapsedMilliseconds

        Write-Host "[$timestamp] [PING #$count] " -NoNewline -ForegroundColor White
        Write-Host "[OK 200] " -NoNewline -ForegroundColor Green
        Write-Host "Server AWAKE! " -NoNewline -ForegroundColor Cyan
        Write-Host "Độ trễ: ${latency}ms" -ForegroundColor Yellow
    }
    catch {
        $sw.Stop()
        $latency = $sw.ElapsedMilliseconds
        $errMsg = $_.Exception.Message

        Write-Host "[$timestamp] [PING #$count] " -NoNewline -ForegroundColor White
        Write-Host "[WARMING UP / WAITING] " -NoNewline -ForegroundColor Magenta
        Write-Host "Đang khởi động lại container: $errMsg (${latency}ms)" -ForegroundColor Red
    }

    Start-Sleep -Seconds $IntervalSeconds
}

