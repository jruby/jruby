@echo off
rem ---------------------------------------------------------------------------
rem jruby.bat - Start Script for the JRuby Interpreter
rem
rem ----- Execute The Requested Command ---------------------------------------

call "%~dp0_jrubyvars" %*

%_STARTJAVA% %_VM_OPTS% -Xdebug -Xrunjdwp:transport=dt_shmem,server=y,suspend=y  -Djruby.base="%JRUBY_BASE%" -Djruby.home="%JRUBY_HOME%" -Djruby.lib="%JRUBY_BASE%\lib" -Djruby.script="jruby.bat" -Djruby.shell="cmd.exe" org.jruby.Main %JRUBY_OPTS% %_RUBY_OPTS%
set E=%ERRORLEVEL%

call "%~dp0_jrubycleanup"
