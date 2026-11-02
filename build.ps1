Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.IO.Compression

$ErrorActionPreference = "Stop"
$modDir = $PSScriptRoot
if (-not $modDir) { $modDir = (Get-Location).Path }
$templateDir = "C:\Users\JamesAdams\AppData\Roaming\ModrinthApp\profiles\Template 1.21.1"

$version = "1.4.2"
Write-Output "Building KeyBindBundles v$version via Gradle..."

& cmd.exe /c "gradlew.bat jar --no-daemon"
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed with code $LASTEXITCODE"
}

$buildLibDir = Join-Path $modDir "build\libs"
$jarBuilt = Get-ChildItem -Path $buildLibDir -Filter "keybindbundles-$version*.jar" | Select-Object -First 1

if (-not $jarBuilt) {
    # Fallback to any built jar in build/libs
    $jarBuilt = Get-ChildItem -Path $buildLibDir -Filter "keybindbundles-*.jar" | Select-Object -First 1
}

if (-not $jarBuilt) {
    throw "Build artifact not found in $buildLibDir"
}

Write-Output "Jar built successfully: $($jarBuilt.FullName) ($($jarBuilt.Length) bytes)"

# Deploy to template profile mods folder
$deployedJar = Join-Path $templateDir "mods\keybindbundles-$version.jar"
$oldJars = Get-ChildItem -Path (Join-Path $templateDir "mods") -Filter "*keybindbundles*.jar"
foreach ($old in $oldJars) {
    Remove-Item $old.FullName -Force
    Write-Output "Removed old jar: $($old.Name)"
}

Copy-Item -Path $jarBuilt.FullName -Destination $deployedJar -Force
Write-Output "Deployed to: $deployedJar"
