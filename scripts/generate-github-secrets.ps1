param(
    [Parameter(Mandatory = $false)]
    [string]$KeystorePath,

    [Parameter(Mandatory = $false)]
    [string]$KeyAlias,

    [Parameter(Mandatory = $false)]
    [string]$OutputFile = "github-secrets.txt"
)

$ErrorActionPreference = "Stop"

function Read-SecretPlainText {
    param([string]$Prompt)

    $secure = Read-Host -AsSecureString -Prompt $Prompt
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

if ([string]::IsNullOrWhiteSpace($KeystorePath)) {
    $KeystorePath = Read-Host "Enter full path to your keystore (.jks)"
}

if (-not (Test-Path -LiteralPath $KeystorePath)) {
    throw "Keystore not found: $KeystorePath"
}

if ([string]::IsNullOrWhiteSpace($KeyAlias)) {
    $KeyAlias = Read-Host "Enter key alias (ANDROID_KEY_ALIAS)"
}

$keystorePassword = Read-SecretPlainText "Enter keystore password (ANDROID_KEYSTORE_PASSWORD)"
$keyPassword = Read-SecretPlainText "Enter key password (ANDROID_KEY_PASSWORD)"

$keystoreBytes = [IO.File]::ReadAllBytes((Resolve-Path -LiteralPath $KeystorePath))
$keystoreBase64 = [Convert]::ToBase64String($keystoreBytes)

$lines = @(
    "ANDROID_KEYSTORE_BASE64=$keystoreBase64"
    "ANDROID_KEYSTORE_PASSWORD=$keystorePassword"
    "ANDROID_KEY_ALIAS=$KeyAlias"
    "ANDROID_KEY_PASSWORD=$keyPassword"
)

$lines | Set-Content -LiteralPath $OutputFile -Encoding UTF8

Write-Host ""
Write-Host "Generated secret values:" -ForegroundColor Green
Write-Host "------------------------------------------"
$lines | ForEach-Object { Write-Host $_ }
Write-Host "------------------------------------------"
Write-Host "Saved to: $(Resolve-Path -LiteralPath $OutputFile)"
Write-Host ""
Write-Host "Next: copy each value into GitHub repo Settings > Secrets and variables > Actions." -ForegroundColor Yellow
