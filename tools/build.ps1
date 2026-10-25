param(
    [switch]$Install,
    [switch]$Run,
    [switch]$Clean
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$sdk = $env:ANDROID_HOME
if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
if (-not $sdk) { throw 'ANDROID_HOME or ANDROID_SDK_ROOT is not set.' }
$bt = Join-Path $sdk 'build-tools\36.1.0'
$jar = Join-Path $sdk 'platforms\android-36\android.jar'
$javaHome = $env:JAVA_HOME
if (-not $javaHome) { throw 'JAVA_HOME is not set.' }
$javac = Join-Path $javaHome 'bin\javac.exe'
$keytool = Join-Path $javaHome 'bin\keytool.exe'
$aapt2 = Join-Path $bt 'aapt2.exe'
$d8 = Join-Path $bt 'd8.bat'
$zipalign = Join-Path $bt 'zipalign.exe'
$apksigner = Join-Path $bt 'apksigner.bat'
$adb = Join-Path $sdk 'platform-tools\adb.exe'

$build = Join-Path $root 'build'
$dist = Join-Path $root 'dist'
$apk = Join-Path $dist 'ShiJi-v1.3.apk'
$ks = Join-Path $root 'keystore.jks'
$ksPass = $env:SHIJI_KS_PASS
if (-not $ksPass) {
    throw 'SHIJI_KS_PASS is not set. Set it to the keystore password before building.'
}

if ($Clean) {
    Remove-Item $build -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force $dist | Out-Null
}
New-Item -ItemType Directory -Force $build, $dist, (Join-Path $build 'gen'), (Join-Path $build 'classes'), (Join-Path $build 'dex') | Out-Null
Remove-Item (Join-Path $build 'classes\*') -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item (Join-Path $build 'gen\*') -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item (Join-Path $build 'dex\*') -Recurse -Force -ErrorAction SilentlyContinue

if (-not (Test-Path $ks)) {
    Write-Host '>> generating signing key'
    & $keytool -genkeypair -keystore $ks -alias shiji -keyalg RSA -keysize 2048 -validity 10000 `
        -storetype PKCS12 -storepass $ksPass -keypass $ksPass -dname 'CN=ShiJi,O=ShiJi,C=CN' | Out-Null
}

Write-Host '>> aapt2 compile'
& $aapt2 compile --dir (Join-Path $root 'app\res') -o (Join-Path $build 'res.zip')
if ($LASTEXITCODE -ne 0) { throw 'aapt2 compile failed' }

Write-Host '>> aapt2 link'
& $aapt2 link -o (Join-Path $build 'unsigned.apk') `
    -I $jar `
    --manifest (Join-Path $root 'app\AndroidManifest.xml') `
    --java (Join-Path $build 'gen') `
    --min-sdk-version 28 `
    --target-sdk-version 34 `
    --version-code 4 `
    --version-name '1.3' `
    --auto-add-overlay `
    (Join-Path $build 'res.zip')
if ($LASTEXITCODE -ne 0) { throw 'aapt2 link failed' }

Write-Host '>> javac'
$sources = @(Get-ChildItem (Join-Path $root 'app\java') -Recurse -Filter *.java | ForEach-Object { $_.FullName })
$sources += @(Get-ChildItem (Join-Path $build 'gen') -Recurse -Filter *.java | ForEach-Object { $_.FullName })
& $javac -encoding UTF-8 --release 11 -nowarn -classpath $jar -d (Join-Path $build 'classes') $sources
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

Write-Host '>> d8'
$classFiles = @(Get-ChildItem (Join-Path $build 'classes') -Recurse -Filter *.class | ForEach-Object { $_.FullName })
& $d8 --release --min-api 28 --lib $jar --output (Join-Path $build 'dex') $classFiles
if ($LASTEXITCODE -ne 0) { throw 'd8 failed' }

Write-Host '>> package'
Copy-Item (Join-Path $build 'unsigned.apk') (Join-Path $build 'app.apk') -Force
Add-Type -AssemblyName System.IO.Compression | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem | Out-Null
$zip = [System.IO.Compression.ZipFile]::Open((Join-Path $build 'app.apk'), [System.IO.Compression.ZipArchiveMode]::Update)
try {
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, (Join-Path $build 'dex\classes.dex'), 'classes.dex', [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
} finally { $zip.Dispose() }

Write-Host '>> zipalign'
& $zipalign -f -p 4 (Join-Path $build 'app.apk') (Join-Path $build 'aligned.apk')
if ($LASTEXITCODE -ne 0) { throw 'zipalign failed' }

Write-Host '>> sign'
& $apksigner sign --ks $ks --ks-key-alias shiji --ks-pass "pass:$ksPass" --key-pass "pass:$ksPass" `
    --out $apk (Join-Path $build 'aligned.apk')
if ($LASTEXITCODE -ne 0) { throw 'apksigner failed' }
& $apksigner verify --print-certs $apk | Select-Object -First 3

$size = [math]::Round((Get-Item $apk).Length / 1KB, 1)
Write-Host ">> APK: $apk ($size KB)"

if ($Install) {
    Write-Host '>> adb install'
    & $adb install --no-streaming -r -d $apk
    if ($LASTEXITCODE -ne 0) { throw 'adb install failed' }
    Write-Host '>> grant permissions'
    & (Join-Path $PSScriptRoot 'setup.ps1')
}

if ($Run) {
    & $adb shell am start -n com.shiji.guard/.MainActivity | Out-Null
}
