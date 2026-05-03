# --- Admin Check ---
$currentPrincipal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
if (-not $currentPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "Not Admin, relaunch as admin..." -ForegroundColor Yellow
    Start-Process powershell.exe -ArgumentList "-NoExit -ExecutionPolicy Bypass -File `"$PSCommandPath`"" -Verb RunAs
    exit
}

# --- Config ---
$TotalLoops = 50

$WifiOnMinWait = 5
$WifiOnMaxWait = 60*2

$WifiOffMinWait = 10
$WifiOffMaxWait = 60*6


# --- Run ---

for ($i = 1; $i -le $TotalLoops; $i++) {
    $WaitTime = Get-Random -Minimum $WifiOffMinWait -Maximum ($WifiOffMaxWait + 1)
    $Timestamp = Get-Date -Format "HH:mm:ss"
    Write-Host "[$Timestamp] Loop $i/${TotalLoops}: Waiting ${WaitTime}s to turn wifi off..." -ForegroundColor Red
    Start-Sleep -Seconds $WaitTime
	Set-NetAdapterAdvancedProperty -Name "Wi-Fi" -AllProperties -RegistryKeyword "SoftwareRadioOff" -RegistryValue "1"
	
	$WaitTime = Get-Random -Minimum $WifiOnMinWait -Maximum ($WifiOnMaxWait + 1)
    $Timestamp = Get-Date -Format "HH:mm:ss"
    Write-Host "[$Timestamp] Loop $i/${TotalLoops}: Waiting ${WaitTime}s to turn wifi back on..." -ForegroundColor Green
    Start-Sleep -Seconds $WaitTime
	Set-NetAdapterAdvancedProperty -Name "Wi-Fi" -AllProperties -RegistryKeyword "SoftwareRadioOff" -RegistryValue "0"
}

Write-Host "All done." -ForegroundColor Cyan
