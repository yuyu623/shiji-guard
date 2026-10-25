$ErrorActionPreference = 'Continue'
$adb = $env:ADB
if (-not $adb) {
    if ($env:ANDROID_HOME) { $adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe' }
    elseif ($env:ANDROID_SDK_ROOT) { $adb = Join-Path $env:ANDROID_SDK_ROOT 'platform-tools\adb.exe' }
    else { $adb = 'adb' }
}
$pkg = 'com.shiji.guard'
$svc = "$pkg/$pkg.GuardService"

Write-Host '>> usage access'
& $adb shell appops set $pkg GET_USAGE_STATS allow

Write-Host '>> overlay / background start'
& $adb shell appops set $pkg SYSTEM_ALERT_WINDOW allow
& $adb shell appops set $pkg AUTO_START allow 2>$null
& $adb shell appops set $pkg RUN_IN_BACKGROUND allow 2>$null
& $adb shell appops set $pkg RUN_ANY_IN_BACKGROUND allow 2>$null
& $adb shell appops set $pkg BACKGROUND_START_ACTIVITY allow 2>$null

Write-Host '>> notifications'
& $adb shell pm grant $pkg android.permission.POST_NOTIFICATIONS 2>$null

Write-Host '>> accessibility service'
$cur = (& $adb shell settings get secure enabled_accessibility_services).Trim()
if ($cur -eq 'null' -or $cur -eq '') { $cur = '' }
$parts = @($cur -split ':' | Where-Object { $_ -and ($_ -notlike "*$pkg*") })
$clean = ($parts -join ':')
if ($clean) { & $adb shell settings put secure enabled_accessibility_services $clean }
else { & $adb shell settings put secure enabled_accessibility_services null }
& $adb shell settings put secure accessibility_enabled 0
Start-Sleep -Milliseconds 900
$new = if ($clean) { "$clean`:$svc" } else { $svc }
& $adb shell settings put secure enabled_accessibility_services $new
& $adb shell settings put secure accessibility_enabled 1
Start-Sleep -Milliseconds 900

Write-Host '>> notification access (background audio)'
& $adb shell cmd notification allow_listener "$pkg/$pkg.MediaListenerService" 2>$null

Write-Host '>> battery whitelist / standby bucket'
& $adb shell dumpsys deviceidle whitelist "+$pkg" | Out-Null
& $adb shell am set-standby-bucket $pkg active

Write-Host '>> launch'
& $adb shell am start -n "$pkg/.MainActivity" | Out-Null
Start-Sleep -Seconds 1

Write-Host ''
Write-Host 'Status:'
Write-Host ("  usage : " + (& $adb shell appops get $pkg GET_USAGE_STATS).Trim())
Write-Host ("  a11y  : " + (& $adb shell settings get secure enabled_accessibility_services).Trim())
Write-Host ("  notify: " + (& $adb shell settings get secure enabled_notification_listeners).Trim())
$dump = (& $adb shell dumpsys activity services $pkg).Trim()
if ($dump -match 'TrackerService') { Write-Host '  tracker: running' } else { Write-Host '  tracker: not running (open the app once)' }
