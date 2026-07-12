param(
    [string]$ApiKey = $env:LLM_API_KEY,
    [string]$Model = "deepseek-chat"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$BackendDir = Join-Path $ProjectRoot "backend"
$JavaHome = "D:\db\jdk17.0.19_10"
$MavenCmd = Join-Path $ProjectRoot "tools\apache-maven-3.9.16\bin\mvn.cmd"

if (-not (Test-Path -LiteralPath $BackendDir)) {
    throw "Backend directory not found: $BackendDir"
}

if (-not (Test-Path -LiteralPath $JavaHome)) {
    throw "JDK directory not found: $JavaHome"
}

if (-not (Test-Path -LiteralPath $MavenCmd)) {
    throw "Maven command not found: $MavenCmd"
}

if ([string]::IsNullOrWhiteSpace($ApiKey)) {
    $secureKey = Read-Host "Enter DeepSeek API Key (input is hidden and will not be saved)" -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKey)
    try {
        $ApiKey = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

if ([string]::IsNullOrWhiteSpace($ApiKey)) {
    throw "DeepSeek API Key is required."
}

$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:Path
$previousLlmEnabled = $env:LLM_ENABLED
$previousLlmProvider = $env:LLM_PROVIDER
$previousLlmApiUrl = $env:LLM_API_URL
$previousLlmModel = $env:LLM_MODEL
$previousLlmApiKey = $env:LLM_API_KEY
$locationPushed = $false

try {
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"

    $env:LLM_ENABLED = "true"
    $env:LLM_PROVIDER = "deepseek"
    $env:LLM_API_URL = "https://api.deepseek.com/chat/completions"
    $env:LLM_MODEL = $Model
    $env:LLM_API_KEY = $ApiKey

    Write-Host "Starting backend with DeepSeek enabled..." -ForegroundColor Cyan
    Write-Host "Backend: $BackendDir"
    Write-Host "Model:   $Model"
    Write-Host "API Key: provided for this process only"
    Write-Host ""

    Push-Location $BackendDir
    $locationPushed = $true
    & $MavenCmd spring-boot:run
} finally {
    if ($locationPushed) {
        Pop-Location
    }

    $env:JAVA_HOME = $previousJavaHome
    $env:Path = $previousPath
    $env:LLM_ENABLED = $previousLlmEnabled
    $env:LLM_PROVIDER = $previousLlmProvider
    $env:LLM_API_URL = $previousLlmApiUrl
    $env:LLM_MODEL = $previousLlmModel
    $env:LLM_API_KEY = $previousLlmApiKey
}
