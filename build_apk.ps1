$env:JAVA_HOME = "C:\Program Files\JetBrains\PyCharm 2025.3.4\jbr"
$env:ANDROID_HOME = "C:\Users\Alejandro\AppData\Local\Android\Sdk"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH

Write-Host "Building Persona Radar Debug APK..."
.\gradlew.bat assembleDebug --stacktrace

