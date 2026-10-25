param(
  [switch]$Deploy
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$envFile = Join-Path $projectRoot '.env.production'

if (-not (Test-Path -LiteralPath $envFile)) {
  throw "Missing environment file: $envFile"
}

if (-not (Get-Command npx.cmd -ErrorAction SilentlyContinue)) {
  throw 'Node.js/npm are required. Install Node.js, reopen PowerShell, then run this script again.'
}
$npxCommand = (Get-Command npx.cmd).Source

function Invoke-VercelCli {
  param(
    [Parameter(Mandatory = $true)][string[]]$Arguments,
    [string]$InputValue,
    [switch]$AllowFailure,
    [switch]$Interactive,
    [switch]$ShowOutput
  )

  $previousPreference = $ErrorActionPreference
  try {
    # Windows PowerShell 5.1 can turn npm's stderr notices into terminating errors.
    $ErrorActionPreference = 'Continue'
    if ($Interactive) {
      & $script:npxCommand --yes vercel@latest @Arguments
      $exitCode = $LASTEXITCODE
    } elseif ($PSBoundParameters.ContainsKey('InputValue')) {
      $output = $InputValue | & $script:npxCommand --yes vercel@latest @Arguments 2>&1
      $exitCode = $LASTEXITCODE
    } else {
      $output = & $script:npxCommand --yes vercel@latest @Arguments 2>&1
      $exitCode = $LASTEXITCODE
    }
  } finally {
    $ErrorActionPreference = $previousPreference
  }

  if (($ShowOutput -or ($exitCode -ne 0 -and -not $AllowFailure)) -and $output) {
    $output | ForEach-Object { Write-Host $_ }
  }
  if ($exitCode -ne 0 -and -not $AllowFailure) {
    throw "Vercel command failed (exit code $exitCode): $($Arguments -join ' ')"
  }
  return $exitCode
}

$requiredNames = @(
  'VITE_CLOUDINARY_CLOUD_NAME',
  'VITE_CLOUDINARY_UPLOAD_PRESET'
)

$values = @{}
foreach ($line in Get-Content -LiteralPath $envFile) {
  if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)\s*$') {
    $values[$matches[1]] = $matches[2]
  }
}

foreach ($name in $requiredNames) {
  if (-not $values.ContainsKey($name) -or [string]::IsNullOrWhiteSpace($values[$name])) {
    throw "Missing or empty $name in $envFile"
  }
}

Push-Location $projectRoot
try {
  Write-Host 'Checking Vercel login...'
  $whoamiExit = Invoke-VercelCli -Arguments @('whoami')
  if ($whoamiExit -ne 0) {
    Write-Host 'Vercel login is required. Complete the browser login, then this script will continue.'
    Invoke-VercelCli -Arguments @('login') -Interactive | Out-Null
  }

  Write-Host 'Linking to team fig-link-vc / project fixlink...'
  Invoke-VercelCli -Arguments @('link', '--yes', '--scope', 'fig-link-vc', '--project', 'fixlink') -ShowOutput | Out-Null

  $environments = @('production', 'preview', 'development')
  foreach ($name in $requiredNames) {
    foreach ($environment in $environments) {
      Write-Host "Replacing $name ($environment) with a non-sensitive value..."
      # Remove first so an old Sensitive setting cannot block Development or survive an overwrite.
      Invoke-VercelCli -Arguments @('env', 'rm', $name, $environment, '--yes') -AllowFailure | Out-Null
      $inputValue = $values[$name]
      if ($environment -eq 'preview') {
        # Vercel asks for an optional Git branch first; an empty first line means all Preview branches.
        $inputValue = "`n$inputValue"
      }
      Invoke-VercelCli -Arguments @('env', 'add', $name, $environment, '--force', '--type', 'config') -InputValue $inputValue -ShowOutput | Out-Null
    }
  }

  if ($Deploy) {
    Write-Host 'Deploying to Production...'
    Invoke-VercelCli -Arguments @('deploy', '--prod', '--yes') -Interactive | Out-Null
  } else {
    Write-Host 'All Cloudinary variables are set. Run this script with -Deploy to deploy now, or redeploy from the Vercel dashboard.'
  }
} finally {
  Pop-Location
}
