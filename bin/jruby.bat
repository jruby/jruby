@echo off
rem ---------------------------------------------------------------------------
rem jruby.bat - Start Script for the JRuby Interpreter
rem
rem for info on environment variables, see internal batch script _jrubyvars.bat

setlocal
call "%~dp0_jrubyvars" %*

if %JRUBY_BAT_ERROR%==0 "%_STARTJAVA%" %_VM_OPTS% -cp "%CLASSPATH%" -Djruby.home="%JRUBY_HOME%" -Djruby.lib="%JRUBY_HOME%\lib" -Djruby.shell="cmd.exe" -Djruby.script=jruby.bat org.jruby.Main %JRUBY_OPTS% %_RUBY_OPTS%
set E=%ERRORLEVEL%

call "%~dp0_jrubycleanup"

rem 1. exit must be on the same line in order to see local %E% variable!
rem 2. we must use cmd /c in order for the exit code properly returned!
rem    See JRUBY-2094 for more details.
endlocal & cmd /c exit /b %E%
