@echo off
set "JAVA_HOME=D:\applications\jdk-17"
set "ANDROID_HOME=D:\android_01"
set "ANDROID_SDK_ROOT=D:\android_01"
set "PATH=D:\android_01\platform-tools;D:\android_01\emulator;D:\android_01\cmdline-tools\latest\bin;%PATH%"

echo [1/3] Creating Fire Tablet AVD (1280x800, No GMS)...
(echo no) | "D:\android_01\cmdline-tools\latest\bin\avdmanager.bat" create avd -n "Fire_Tablet_Test" -k "system-images;android-30;default;x86_64" --device "10.1in WXGA (Tablet)" --force

echo [2/3] Configuring Fire Tablet specs...
powershell -Command "(Get-Content -Path \"$env:USERPROFILE\.android\avd\Fire_Tablet_Test.avd\config.ini\") | ForEach-Object { if ($_ -match '^hw.ramSize=') { 'hw.ramSize=2048' } else { $_ } } | Set-Content -Path \"$env:USERPROFILE\.android\avd\Fire_Tablet_Test.avd\config.ini\""

echo [3/3] Ready to launch!
