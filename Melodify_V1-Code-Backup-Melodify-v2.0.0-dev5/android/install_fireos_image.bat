@echo off
set "JAVA_HOME=D:\applications\jdk-17"
set "ANDROID_HOME=D:\android_01"
set "ANDROID_SDK_ROOT=D:\android_01"

echo Accepting licenses...
(echo y & echo y & echo y & echo y & echo y & echo y & echo y & echo y) | "D:\android_01\cmdline-tools\latest\bin\sdkmanager.bat" --licenses

echo Downloading system-images;android-30;default;x86_64...
(echo y) | "D:\android_01\cmdline-tools\latest\bin\sdkmanager.bat" "system-images;android-30;default;x86_64"

echo Done!
