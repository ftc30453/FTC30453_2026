@echo off
rem Compiles your TeamCode against the simulator's stand-in FTC SDK and connects it to the
rem Godot simulator. Re-run this after every code change (close the window or Ctrl+C to stop).
rem
rem   run-bridge.bat
rem   set TEAMCODE=C:\path\to\TeamCode\src\main\java  then  run-bridge.bat

setlocal EnableDelayedExpansion
cd /d "%~dp0"

if "%TEAMCODE%"=="" set "TEAMCODE=%~dp0..\..\TeamCode\src\main\java"

set "JAVAC="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" (
  set "JAVAC=%JAVA_HOME%\bin\javac.exe"
  set "JAVA=%JAVA_HOME%\bin\java.exe"
)
if not defined JAVAC (
  where javac >nul 2>nul && (
    set "JAVAC=javac"
    set "JAVA=java"
  )
)
if not defined JAVAC if exist "C:\Program Files\Android\Android Studio\jbr\bin\javac.exe" (
  set "JAVAC=C:\Program Files\Android\Android Studio\jbr\bin\javac.exe"
  set "JAVA=C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
)
if not defined JAVAC (
  echo No Java compiler found. Install a JDK 17+ or set JAVA_HOME to Android Studio's "jbr" folder.
  pause
  exit /b 1
)

rem Not next to the repo? Use the path saved last time, or ask for it.
if not exist "%TEAMCODE%" if exist "%~dp0teamcode-path.txt" set /p TEAMCODE=<"%~dp0teamcode-path.txt"

:check_teamcode
set "TEAMCODE=%TEAMCODE:"=%"
if exist "%TEAMCODE%\TeamCode\src\main\java" set "TEAMCODE=%TEAMCODE%\TeamCode\src\main\java"
if exist "%TEAMCODE%\src\main\java\org" set "TEAMCODE=%TEAMCODE%\src\main\java"
if exist "%TEAMCODE%\org" goto teamcode_ok
echo.
echo Could not find your TeamCode at:
echo   %TEAMCODE%
echo.
echo Paste the path to your robot project folder (the one that contains TeamCode)
echo and press Enter. Tip: open that folder in File Explorer, click the address bar, copy.
set "TEAMCODE="
set /p "TEAMCODE=Path: "
if "%TEAMCODE%"=="" exit /b 1
goto check_teamcode

:teamcode_ok
> "%~dp0teamcode-path.txt" echo %TEAMCODE%

if exist build rmdir /s /q build
mkdir build\classes

(for /r "src\main\java" %%f in (*.java) do (
  set "p=%%f"
  echo "!p:\=/!"
)) > build\sources.txt
(for /r "%TEAMCODE%" %%f in (*.java) do (
  set "p=%%f"
  echo "!p:\=/!"
)) >> build\sources.txt

echo [bridge] Compiling TeamCode from %TEAMCODE% ...
"%JAVAC%" --release 17 -nowarn -encoding UTF-8 -d build\classes @build\sources.txt
if errorlevel 1 (
  echo.
  echo Compile failed - see the errors above.
  pause
  exit /b 1
)
"%JAVA%" -cp build\classes sim.SimMain %*
