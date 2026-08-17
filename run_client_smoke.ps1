$ErrorActionPreference = "Continue"
$log = Join-Path $PWD.Path "run\client_smoke.log"
if (Test-Path $log) { Remove-Item $log }
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$proc = Start-Process -FilePath ".\gradlew.bat" -ArgumentList "runClient","--console=plain","--stacktrace" -WorkingDirectory $PWD.Path -RedirectStandardOutput $log -RedirectStandardError (Join-Path $PWD.Path "run\client_smoke_err.log") -PassThru -WindowStyle Hidden
$deadline = (Get-Date).AddSeconds(420)
$booted = $false
$crashed = $false
while ((Get-Date) -lt $deadline) {
  Start-Sleep -Seconds 4
  if (Test-Path $log) {
    $content = Get-Content $log -Raw -ErrorAction SilentlyContinue
    if ($content -match "Backend library: LWJGL|Sound engine started|Reloading ResourceManager") {
      $booted = $true
    }
    if ($content -match 'Mixin apply failed|Failed to apply mixin|got a serious error|Mods were unable to load|ClassCastException|NullPointerException|Exception in thread "main"') {
      $crashed = $true
      break
    }
  }
  if ($proc.HasExited) { break }
}
Write-Output ("BOOTED=" + $booted)
Write-Output ("CRASHED=" + $crashed)
Write-Output ("PROC_EXITED=" + $proc.HasExited)
if (-not $proc.HasExited) {
  Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
  Get-Process java -ErrorAction SilentlyContinue | Where-Object { $_.Path -like "*jdk-17*" } | Stop-Process -Force -ErrorAction SilentlyContinue
}
Start-Sleep -Seconds 2
$tail = if (Test-Path $log) { (Get-Content $log -Tail 40 | Out-String) } else { "(no log)" }
$tail