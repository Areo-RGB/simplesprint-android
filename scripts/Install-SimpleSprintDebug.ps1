param(
    [string]$DeviceId
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-AdbPath {
    $candidates = @(
        $env:ADB_BIN,
        $env:ADB_PATH,
        "adb.exe",
        "adb"
    ) | Where-Object { $_ -and $_.Trim().Length -gt 0 }

    $sdkRoots = @($env:ANDROID_SDK_ROOT, $env:ANDROID_HOME) | Where-Object { $_ -and $_.Trim().Length -gt 0 }
    foreach ($sdkRoot in $sdkRoots) {
        $candidates += (Join-Path $sdkRoot "platform-tools\adb.exe")
        $candidates += (Join-Path $sdkRoot "platform-tools\adb")
    }

    foreach ($candidate in $candidates) {
        if ($candidate -match "[\\/]" -or $candidate -like "*.exe") {
            if (Test-Path -LiteralPath $candidate) {
                return $candidate
            }
            continue
        }

        $cmd = Get-Command $candidate -ErrorAction SilentlyContinue
        if ($cmd) {
            return $candidate
        }
    }

    throw "Unable to locate adb. Set ADB_BIN or ADB_PATH to your adb executable."
}

function Get-ReadyDeviceIds {
    param(
        [Parameter(Mandatory = $true)]
        [string]$AdbPath
    )

    $lines = & $AdbPath devices
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to run `"$AdbPath devices`"."
    }

    return $lines |
        Select-Object -Skip 1 |
        Where-Object { $_ -match "^\S+\s+device$" } |
        ForEach-Object { ($_ -split "\s+")[0] }
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$apkPath = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"
$packageId = "com.paul.simplesprint.debug"

if (-not (Test-Path -LiteralPath $apkPath)) {
    throw "Debug APK not found at: $apkPath`nBuild first with: .\gradlew.bat :app:assembleDebug"
}

$adb = Resolve-AdbPath
$deviceIds = Get-ReadyDeviceIds -AdbPath $adb
if (-not $deviceIds -or $deviceIds.Count -eq 0) {
    throw "No ready Android devices found. Connect devices and run adb devices."
}

if ($DeviceId -and $DeviceId.Trim().Length -gt 0) {
    if ($deviceIds -notcontains $DeviceId) {
        throw "Requested device '$DeviceId' is not ready. Connected ready devices: $($deviceIds -join ', ')"
    }
    $deviceIds = @($DeviceId)
}

foreach ($deviceId in $deviceIds) {
    Write-Host "Installing debug APK on $deviceId..."
    & $adb -s $deviceId install -r $apkPath | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "Install failed on $deviceId."
    }

    Write-Host "Launching $packageId on $deviceId..."
    & $adb -s $deviceId shell monkey -p $packageId -c android.intent.category.LAUNCHER 1 | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "Launch failed on $deviceId."
    }
}

Write-Host "Installed and launched debug APK on $($deviceIds.Count) device(s)."
