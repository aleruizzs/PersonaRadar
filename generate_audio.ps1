$rawDir = "c:\Users\Ale\Desktop\Persona\app\src\main\res\raw"
if (!(Test-Path $rawDir)) {
    New-Item -ItemType Directory -Force -Path $rawDir | Out-Null
}
$filePath = Join-Path $rawDir "last_surprise.mp3"

$sampleRate = 44100
$duration = 5.0
$numSamples = [int]($sampleRate * $duration)
$dataLength = $numSamples * 2
$fileLength = $dataLength + 36

$stream = [System.IO.File]::Create($filePath)
$writer = New-Object System.IO.BinaryWriter($stream)

# RIFF header
$writer.Write([System.Text.Encoding]::ASCII.GetBytes("RIFF"))
$writer.Write([int32]$fileLength)
$writer.Write([System.Text.Encoding]::ASCII.GetBytes("WAVE"))

# fmt chunk
$writer.Write([System.Text.Encoding]::ASCII.GetBytes("fmt "))
$writer.Write([int32]16)
$writer.Write([int16]1) # PCM
$writer.Write([int16]1) # Mono
$writer.Write([int32]$sampleRate)
$writer.Write([int32]($sampleRate * 2))
$writer.Write([int16]2)
$writer.Write([int16]16)

# data chunk
$writer.Write([System.Text.Encoding]::ASCII.GetBytes("data"))
$writer.Write([int32]$dataLength)

# Funky synth bass/lead sequence
$notes = @(329.63, 392.00, 440.00, 493.88, 587.33, 659.25, 587.33, 440.00)
$noteLen = $sampleRate * 0.45

for ($i = 0; $i -lt $numSamples; $i++) {
    $noteIdx = [int]($i / $noteLen) % $notes.Length
    $freq = $notes[$noteIdx]
    $t = $i / $sampleRate
    $posInNote = ($i % $noteLen) / $noteLen
    $env = [Math]::Sin($posInNote * [Math]::PI)
    $val = (0.6 * [Math]::Sin(2 * [Math]::PI * $freq * $t) + 0.3 * [Math]::Sin(4 * [Math]::PI * $freq * $t) + 0.1 * [Math]::Sin(6 * [Math]::PI * $freq * $t)) * $env
    $sample = [int16]($val * 30000)
    $writer.Write($sample)
}

$writer.Close()
$stream.Close()
Write-Host "Created valid audio file at $filePath"
