$wrapperDir = "c:\Users\Ale\Desktop\Persona\gradle\wrapper"
if (!(Test-Path $wrapperDir)) {
    New-Item -ItemType Directory -Force -Path $wrapperDir | Out-Null
}
$wrapperJar = Join-Path $wrapperDir "gradle-wrapper.jar"
Write-Host "Downloading gradle-wrapper.jar..."
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar" -OutFile $wrapperJar
Write-Host "gradle-wrapper.jar downloaded: $(Test-Path $wrapperJar)"
