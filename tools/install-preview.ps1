param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [string]$ExpectedModel = 'AQUOS_TVE19A',
    [string]$Apk = (Join-Path (Split-Path $PSScriptRoot -Parent) 'out\preview\tvhome-header-preview.apk'),
    [string]$Sdk = "$env:LOCALAPPDATA\Android\sdk",
    [switch]$MakeDefaultHome
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$package = 'com.thewhitewolf1985.tvhome.preview'
$component = "$package/com.ansub.tvhome.MainActivity"
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
$manifest = & "$Sdk\build-tools\36.0.0\aapt2.exe" dump xmltree --file AndroidManifest.xml $Apk
if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect the APK manifest' }
$hasHome = ($manifest -join "`n") -match 'android\.intent\.category\.HOME'
$homeBefore = Invoke-PreviewAdb @('shell','cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
if ($MakeDefaultHome -and -not $hasHome) { throw 'Build the Home variant with build-preview.ps1 -AsHome first' }
if (-not $hasHome -and $homeBefore.Contains($package + '/')) {
    throw 'This app is already the default Home. Refusing to replace it with a preview without HOME support; build with -AsHome.'
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
if ($MakeDefaultHome) {
    Write-Output (Invoke-PreviewAdb @('shell','cmd','package','set-home-activity','--user','0',$component))
    $resolved = Invoke-PreviewAdb @('shell','cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
    if (-not $resolved.Contains($component)) { throw 'The requested default Home did not resolve after selection' }
    Write-Output "Default Home verified: $component"
}
Write-Output 'App updated; persistent brightness grants verified. No volume/backlight levels or input selections changed.'
