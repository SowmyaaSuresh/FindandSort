@echo off
setlocal
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out src\findsort\FindAndSortApp.java
if errorlevel 1 (
  echo.
  echo Compilation failed. Please install JDK 17 or later and try again.
  pause
  exit /b 1
)
java -cp out findsort.FindAndSortApp
