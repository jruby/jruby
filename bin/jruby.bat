@echo off
rem ---------------------------------------------------------------------------
rem jruby.bat - Start Script for the JRuby Interpreter
rem
rem for info on environment variables, see internal batch script _jrubyvars.bat

setlocal

rem Sometimes, when jruby.bat is being invoked from another BAT file,
rem %~dp0 is incorrect and points to the current dir, not to JRuby's bin dir,
rem so we look on the PATH in such cases.
IF EXIST "%~dp0_jrubyvars.bat" (set FULL_PATH=%~dp0) ELSE (set FULL_PATH=%~dp$PATH:0)

call "%FULL_PATH%_jrubyvars.bat" %*

if not "%CP%" == "" (
  set CLASSPATH = ""
)

if %JRUBY_BAT_ERROR%==0 (
  if "%_NAILGUN_CLIENT%" == "" (
     "%_STARTJAVA%" %_VM_OPTS% %_JRUBY_BOOTCP_OPTS% -classpath "%CP%;%CLASSPATH%" -Djruby.home="%JRUBY_HOME%" -Djruby.lib="%JRUBY_HOME%\lib" -Djruby.shell="cmd.exe" -Djruby.script=jruby.bat %_JAVA_CLASS% %JRUBY_OPTS% %_RUBY_OPTS%
  ) else (
     "%JRUBY_HOME%\tool\nailgun\ng.exe" org.jruby.util.NailMain %JRUBY_OPTS% %_RUBY_OPTS%
  )
)
set E=%ERRORLEVEL%

call "%FULL_PATH%_jrubycleanup"

rem 1. exit must be on the same line in order to see local %E% variable!
rem 2. we must use cmd /c in order for the exit code properly returned!
rem    See JRUBY-2094 for more details.
endlocal & cmd /d /c exit /b %E%
