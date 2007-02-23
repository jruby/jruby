@echo off
rem ---------------------------------------------------------------------------
rem jruby.bat - Start Script for the JRuby Interpreter
rem
rem ----- Execute The Requested Command ---------------------------------------

call %~dp0_jrubyvars

%_STARTJAVA%  -Xdebug -Xrunjdwp:transport=dt_shmem,server=y,suspend=y  -Djruby.base="%JRUBY_BASE%" -Djruby.home="%JRUBY_HOME%" -Djruby.lib="%JRUBY_BASE%\lib" -Djruby.script="jruby.bat" -Djruby.shell="cmd.exe" org.jruby.Main %JRUBY_OPTS% %1 %2 %3 %4 %5 %6 %7 %8 %9
set E=%ERRORLEVEL%

call %~dp0_jrubycleanup
