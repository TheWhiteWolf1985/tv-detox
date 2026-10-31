param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [string]$ExpectedModel = 'AQUOS_TVE19A',
    [string]$Apk = (Join-Path (Split-Path $PSScriptRoot -Parent) 'out\preview\tvhome-header-preview.apk'),
    [string]$Sdk = "$env:LOCALAPPDATA\Android\sdk"
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$package = 'com.thewhitewolf1985.tvhome.preview'
function Invoke-PreviewAdb([string[]]$Arguments) {
    $result = & adb.exe -s $Serial @Arguments
    if ($LASTEXITCODE -ne 0) { throw "adb failed: $($Arguments -join ' ')" }
    return ($result -join "`n")
}
$model = (Invoke-PreviewAdb @('shell','getprop','ro.product.model')).Trim()
if ($model -ne $ExpectedModel) { throw "Unexpected TV model: $model" }
$badging = & "$Sdk\build-tools\36.0.0\aapt2.exe" dump badging $Apk
if ($LASTEXITCODE -ne 0 -or -not ($badging -match "package: name='$([regex]::Escape($package))'")) {
    throw 'The APK is not the separate preview package'
}
# Updating in place keeps the UID, preferences, signing identity and persistent grants.
# adb rejects a signing mismatch; there is deliberately no uninstall/reinstall fallback.
Write-Output (Invoke-PreviewAdb @('install','-r',$Apk))
$permissions = Invoke-PreviewAdb @('shell','dumpsys','package',$package)
$retained = $permissions -match 'android\.permission\.WRITE_SECURE_SETTINGS: granted=true'
Write-Output "WRITE_SECURE_SETTINGS retained across update: $retained"
if (-not $retained) {
    $null = Invoke-PreviewAdb @('shell','pm','grant','--user','0',$package,'android.permission.WRITE_SECURE_SETTINGS')
}
$mode = Invoke-PreviewAdb @('shell','appops','get',$package,'WRITE_SETTINGS')
if ($mode -notmatch 'WRITE_SETTINGS: allow') {
    $null = Invoke-PreviewAdb @('shell','appops','set',$package,'WRITE_SETTINGS','allow')
}
# Some Android 9 builds print usage successfully but return exit code 1 for "help".
$help = (& adb.exe -s $Serial shell appops help 2>&1) -join "`n"
if ($help -match 'write-settings') {
    Write-Output (Invoke-PreviewAdb @('shell','appops','write-settings'))
}
$permissions = Invoke-PreviewAdb @('shell','dumpsys','package',$package)
$mode = Invoke-PreviewAdb @('shell','appops','get',$package,'WRITE_SETTINGS')
if ($permissions -notmatch 'android\.permission\.WRITE_SECURE_SETTINGS: granted=true' -or $mode -notmatch 'WRITE_SETTINGS: allow') {
    throw 'The preview brightness permissions were not retained/granted'
}
Write-Output 'Preview updated; persistent brightness grants verified. No TV settings or input selections changed.'
