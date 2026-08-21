$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot"
$env:ANDROID_HOME = "C:\Users\Ale\AppData\Local\Android\Sdk"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH

Write-Host "Building Persona Radar Debug APK..."
.\gradlew.bat assembleDebug --stacktrace
