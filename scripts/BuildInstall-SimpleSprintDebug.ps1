param(
    [string]$TabletHostSerial = "4c637b9e"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

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

function Get-ConnectedDevices {
    param(
        [Parameter(Mandatory = $true)]
        [string]$AdbPath
    )

    $lines = & $AdbPath devices
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to run `"$AdbPath devices`"."
    }

    $ids = $lines |
        Select-Object -Skip 1 |
        Where-Object { $_ -match "^\S+\s+device$" } |
        ForEach-Object { ($_ -split "\s+")[0] }

    $devices = @()
    foreach ($id in $ids) {
        $model = (& $AdbPath -s $id shell getprop ro.product.model).Trim()
        $manufacturer = (& $AdbPath -s $id shell getprop ro.product.manufacturer).Trim()
        $devices += [pscustomobject]@{
            Serial       = $id
            Model        = $model
            Manufacturer = $manufacturer
        }
    }

    return $devices
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

$adb = Resolve-AdbPath
[array]$connectedDevices = @(Get-ConnectedDevices -AdbPath $adb)
if (-not $connectedDevices -or $connectedDevices.Count -eq 0) {
    throw "No ready Android devices found. Connect devices and run adb devices."
}

$connectedSerials = @($connectedDevices | ForEach-Object { $_.Serial })
Write-Host "Connected devices:"
foreach ($device in $connectedDevices) {
    Write-Host " - serial=$($device.Serial) manufacturer=$($device.Manufacturer) model=$($device.Model)"
}

if (-not $TabletHostSerial -or $TabletHostSerial.Trim().Length -eq 0) {
    throw "TabletHostSerial must be provided."
}

if ($connectedSerials -notcontains $TabletHostSerial) {
    throw "Tablet host serial '$TabletHostSerial' is not connected. Connected serials: $($connectedSerials -join ', ')"
}

Invoke-Step -Name "Assemble debug APK (tablet host profile)" -Action {
    .\gradlew.bat :app:assembleDebug -PtabletAlwaysHost=true | Out-Host
}

Invoke-Step -Name "Install tablet-host APK on $TabletHostSerial" -Action {
    powershell -ExecutionPolicy Bypass -File .\scripts\Install-SimpleSprintDebug.ps1 -DeviceId $TabletHostSerial | Out-Host
}

$clientSerials = @($connectedSerials | Where-Object { $_ -ne $TabletHostSerial })

if ($clientSerials.Count -gt 0) {
    Invoke-Step -Name "Assemble debug APK (client profile)" -Action {
        .\gradlew.bat :app:assembleDebug | Out-Host
    }

    foreach ($deviceId in $clientSerials) {
        Invoke-Step -Name "Install client APK on $deviceId" -Action {
            powershell -ExecutionPolicy Bypass -File .\scripts\Install-SimpleSprintDebug.ps1 -DeviceId $deviceId | Out-Host
        }
    }
}

Write-Host "`nDone. Debug build installed and launched. tablet-host=$TabletHostSerial"
