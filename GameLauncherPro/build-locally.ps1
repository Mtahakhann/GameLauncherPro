# Game Launcher Pro - Automated Local Build Script
# This script configures JDK 17, Android SDK cmdline-tools, and builds the APK.

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot
Set-Location $ProjectRoot

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  Building Game Launcher Pro for Pixel 6a " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# Check for existing Java
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCmd) {
    Write-Host "[!] Java 17 is required to build this project." -ForegroundColor Yellow
    Write-Host "    You can install it quickly using winget:" -ForegroundColor Yellow
    Write-Host "    winget install Microsoft.OpenJDK.17" -ForegroundColor Green
    Write-Host "    Or download it from: https://adoptium.net/temurin/releases/?version=17" -ForegroundColor Green
    Write-Host ""
    Write-Host "Alternatively, open this folder in Android Studio and press 'Build APK(s)'." -ForegroundColor Cyan
    exit 1
}

# Run Gradle build
Write-Host "[+] Running Gradle build..." -ForegroundColor Green
& ".\gradlew.bat" assembleDebug

$apkPath = "$ProjectRoot\app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    $outApk = "$ProjectRoot\GameLauncherPro-Pixel6a.apk"
    Copy-Item $apkPath $outApk -Force
    Write-Host ""
    Write-Host "========================================================" -ForegroundColor Green
    Write-Host "  SUCCESS! APK generated at:" -ForegroundColor Green
    Write-Host "  $outApk" -ForegroundColor White
    Write-Host "========================================================" -ForegroundColor Green
    Write-Host "To install on your Pixel 6a:" -ForegroundColor Cyan
    Write-Host "1. Transfer the APK file to your phone (via USB cable, Google Drive, or LocalSend)." -ForegroundColor White
    Write-Host "2. On your phone, tap the file in the Files app and select 'Install'." -ForegroundColor White
} else {
    Write-Host "[X] Build finished, but APK was not found at $apkPath" -ForegroundColor Red
}
