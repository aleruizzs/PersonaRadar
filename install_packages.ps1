$sdkRoot = "C:\Users\Ale\AppData\Local\Android\Sdk"
$licDir = Join-Path $sdkRoot "licenses"
New-Item -ItemType Directory -Force -Path $licDir | Out-Null

$sdkLicense = @"
24333f8a63b6825ea9c5514f83c2829b004d1fee
8933ed6d1053fb3836ed9a324d0abfb40b3f0f12
d56f5185479d0f7930f6888799b46c5b6b70dd01
"@
Set-Content -Path (Join-Path $licDir "android-sdk-license") -Value $sdkLicense -NoNewline

$previewLicense = @"
84831b9409646a918e30573bab4c9c91346d8abd
"@
Set-Content -Path (Join-Path $licDir "android-sdk-preview-license") -Value $previewLicense -NoNewline

$armLicense = @"
859f317696f67ef3d7f30a50a5560e7834b43903
"@
Set-Content -Path (Join-Path $licDir "android-sdk-arm-dbt-license") -Value $armLicense -NoNewline

Write-Host "Licenses written. Installing SDK packages..."
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot"
$sdkmanager = Join-Path $sdkRoot "cmdline-tools\latest\bin\sdkmanager.bat"
& $sdkmanager --sdk_root="$sdkRoot" "platform-tools" "platforms;android-34" "build-tools;34.0.0"
Write-Host "SDK Packages Installed Successfully!"
