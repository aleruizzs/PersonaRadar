$sdkRoot = "C:\Users\Alejandro\AppData\Local\Android\Sdk"
$toolsZip = Join-Path $env:TEMP "cmdline-tools.zip"
New-Item -ItemType Directory -Force -Path $sdkRoot | Out-Null

Write-Host "Downloading Android Command Line Tools with curl..."
curl.exe -L -o $toolsZip "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"

Write-Host "Extracting Command Line Tools..."
$tmpDir = Join-Path $sdkRoot "cmdline-tools-tmp"
if (Test-Path $tmpDir) { Remove-Item -Recurse -Force $tmpDir }
Expand-Archive -Path $toolsZip -DestinationPath $tmpDir -Force

$dest = Join-Path $sdkRoot "cmdline-tools\latest"
New-Item -ItemType Directory -Force -Path (Join-Path $sdkRoot "cmdline-tools") | Out-Null
if (Test-Path $dest) { Remove-Item -Recurse -Force $dest }
Move-Item -Path (Join-Path $tmpDir "cmdline-tools\*") -Destination $dest -Force
Remove-Item -Recurse -Force $tmpDir
Remove-Item -Force $toolsZip

$env:JAVA_HOME = "C:\Program Files\JetBrains\PyCharm 2025.3.4\jbr"
$sdkmanager = Join-Path $dest "bin\sdkmanager.bat"
Write-Host "Accepting licenses..."
cmd.exe /c "echo y | `"$sdkmanager`" --sdk_root=`"$sdkRoot`" --licenses"
Write-Host "Installing platform-tools, platforms;android-34, build-tools;34.0.0..."
& $sdkmanager --sdk_root="$sdkRoot" "platform-tools" "platforms;android-34" "build-tools;34.0.0"
Write-Host "Android SDK Setup Complete!"

