param(
    [string]$Sdk = "$env:LOCALAPPDATA\Android\sdk",
    [string]$BuildTools = '36.0.0',
    [string]$Package = 'com.thewhitewolf1985.tvhome.preview'
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Set-Location (Split-Path $PSScriptRoot -Parent)
function Run([string]$Command, [string[]]$Arguments) {
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Command failed: $LASTEXITCODE" }
}
$bt = Join-Path $Sdk "build-tools\$BuildTools"
$jar = Join-Path $Sdk 'platforms\android-34\android.jar'
$out = Join-Path (Get-Location) 'out\preview'
foreach ($dir in @('classes','dex','gen')) {
    $generated = [IO.Path]::GetFullPath((Join-Path $out $dir))
    if (-not $generated.StartsWith($out + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Generated-directory path escaped the preview workspace'
    }
    if (Test-Path -LiteralPath $generated) { Remove-Item -LiteralPath $generated -Recurse -Force }
}
foreach ($dir in @('classes','dex','gen\com\ansub\tvhome')) {
    New-Item -ItemType Directory -Force (Join-Path $out $dir) | Out-Null
}
# Only this generated manifest changes identity. The upstream-compatible manifest is preserved.
[xml]$manifest = Get-Content -LiteralPath 'AndroidManifest.xml' -Raw
$ns = 'http://schemas.android.com/apk/res/android'
$manifest.manifest.SetAttribute('package', $Package)
$manifest.manifest.SetAttribute('versionName', $ns, '1.5.7-header-preview.3')
$manifest.manifest.application.SetAttribute('label', $ns, 'TV Detox Anteprima')
$manifest.manifest.application.SetAttribute('debuggable', $ns, 'true')
foreach ($node in $manifest.SelectNodes('//activity|//service')) {
    $name = $node.GetAttribute('name', $ns)
    if ($name.StartsWith('.')) { $node.SetAttribute('name', $ns, "com.ansub.tvhome$name") }
}
foreach ($filter in @($manifest.SelectNodes('//intent-filter'))) {
    $homeCategories = @($filter.SelectNodes('category') | Where-Object { $_.GetAttribute('name', $ns) -eq 'android.intent.category.HOME' })
    if ($homeCategories.Count -gt 0) { $null = $filter.ParentNode.RemoveChild($filter) }
}
$manifest.Save((Join-Path $out 'AndroidManifest.xml'))
$apps = Get-Content 'apps.txt' | ForEach-Object { $_.Trim() } | Where-Object { $_ -and -not $_.StartsWith('#') }
$code = 'package com.ansub.tvhome; final class Apps { static final String[] LIST = {' + (($apps | ForEach-Object { '"' + $_ + '"' }) -join ',') + '}; }'
[IO.File]::WriteAllText((Join-Path $out 'gen\com\ansub\tvhome\Apps.java'), $code)
Run "$bt\aapt2.exe" @('compile','--dir','res','-o',"$out\res.zip")
Run "$bt\aapt2.exe" @('link','-o',"$out\base.apk",'-I',$jar,'--manifest',"$out\AndroidManifest.xml",'--min-sdk-version','21','--target-sdk-version','34',"$out\res.zip")
$java = @((Get-ChildItem 'src',"$out\gen" -Recurse -Filter '*.java').FullName)
Run 'javac' (@('-nowarn','-Xlint:-options','-encoding','UTF-8','-source','1.8','-target','1.8','-bootclasspath',"$jar;$bt\core-lambda-stubs.jar",'-d',"$out\classes") + $java)
$classes = @((Get-ChildItem "$out\classes" -Recurse -Filter '*.class').FullName)
Run "$bt\d8.bat" (@('--min-api','21','--release','--lib',$jar,'--output',"$out\dex") + $classes)
Copy-Item "$out\base.apk" "$out\unsigned.apk" -Force
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::Open("$out\unsigned.apk", [IO.Compression.ZipArchiveMode]::Update)
try { $null = [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,"$out\dex\classes.dex",'classes.dex') }
finally { $archive.Dispose() }
Run "$bt\zipalign.exe" @('-f','4',"$out\unsigned.apk","$out\aligned.apk")
# Persistent local preview key; ignored by Git. Never share it or replace the official app with it.
$key = Join-Path (Get-Location) 'key.jks'
if (-not (Test-Path $key)) {
    Run 'keytool' @('-genkeypair','-keystore',$key,'-storepass','tvhome123','-keypass','tvhome123','-alias','tvhome','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=TV Detox Local Preview')
}
Run "$bt\apksigner.bat" @('sign','--ks',$key,'--ks-pass','pass:tvhome123','--out',"$out\tvhome-header-preview.apk","$out\aligned.apk")
Run "$bt\apksigner.bat" @('verify','--print-certs',"$out\tvhome-header-preview.apk")
Write-Output "Preview APK: $out\tvhome-header-preview.apk"
