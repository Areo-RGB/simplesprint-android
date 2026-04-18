Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

param(
    [switch]$TabletAlwaysHost = $true
)

function Invoke-Step {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [scriptblock]$Action
    )

    Write-Host "`n==> $Name"
    & $Action
    if ($LASTEXITCODE -ne 0) {
        throw "Step failed: $Name"
    }
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

Invoke-Step -Name "Build JNI libraries" -Action {
    cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -P 24 -o app/src/main/jniLibs build --release -p sprint-sync-protocol-jni | Out-Host
}

$gradleArgs = @(":app:assembleDebug")
if ($TabletAlwaysHost) {
    $gradleArgs += "-PtabletAlwaysHost=true"
}

Invoke-Step -Name "Assemble debug APK" -Action {
    .\gradlew.bat @gradleArgs | Out-Host
}

Invoke-Step -Name "Install and launch on connected devices" -Action {
    powershell -ExecutionPolicy Bypass -File .\scripts\Install-SimpleSprintDebug.ps1 | Out-Host
}

Write-Host "`nDone. Debug build installed and launched."
