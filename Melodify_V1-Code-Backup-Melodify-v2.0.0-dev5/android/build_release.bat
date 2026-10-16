@echo off
set JAVA_HOME=D:\applications\Android studio\studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%
set ANDROID_SDK_ROOT=D:\android_01
call gradlew.bat assembleRelease bundleRelease --no-daemon
