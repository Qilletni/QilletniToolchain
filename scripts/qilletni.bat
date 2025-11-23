@echo off
setlocal enabledelayedexpansion

:: If the .jar file is in the same directory as this script:
set "JAR_PATH=%~dp0QilletniToolchain.jar"

:: Variables to hold Java system properties (-D*) and other args
set "JAVA_OPTS="
set "OTHER_ARGS="

:parse
if "%~1"=="" goto done

:: Grab the current argument
set "ARG=%~1"
:: Extract the first two characters to check for -D
set "ARG_HEAD=!ARG:~0,2!"

if "!ARG_HEAD!"=="-D" (
    :: Add to the list of Java options
    set "JAVA_OPTS=%JAVA_OPTS% %ARG%"
) else (
    :: Otherwise, it's a normal argument for the Qilletni Toolchain
    set "OTHER_ARGS=%OTHER_ARGS% %ARG%"
)

shift
goto parse

:done
:: Run java with all -D options first, then -jar, then pass other args to the JAR
java %JAVA_OPTS% -jar "%JAR_PATH%" %OTHER_ARGS%

endlocal
