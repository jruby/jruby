@echo off
rem ---------------------------------------------------------------------------
rem jruby.bat - Start Script for the JRuby Interpreter
rem
rem ----- Execute The Requested Command ---------------------------------------

setlocal
call "%~dp0_jrubyvars" %*

if %ERRORLEVEL%==0 "%_STARTJAVA%" %_VM_OPTS% -Xdebug -Xrunjdwp:transport=dt_shmem,server=y,suspend=y  -Djruby.home="%JRUBY_HOME%" -Djruby.lib="%JRUBY_HOME%\lib" -Djruby.script="jruby.bat" -Djruby.shell="cmd.exe" org.jruby.Main %JRUBY_OPTS% %_RUBY_OPTS%
set E=%ERRORLEVEL%

call "%~dp0_jrubycleanup"
endlocal