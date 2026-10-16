@echo off
REM Copy built artifacts to dist_release folder

set SRC_APK=app\build\outputs\apk\release\app-release.apk
set SRC_AAB=app\build\outputs\bundle\release\app-release.aab
set DIST=..\..\dist_release

echo Copying APK...
copy /Y "%SRC_APK%" "%DIST%\Melodify-v2.0.5-release.apk"

echo Copying AAB...
copy /Y "%SRC_AAB%" "%DIST%\Melodify-v2.0.5-release.aab"

echo Done!
dir "%DIST%\Melodify-v2.0*"
