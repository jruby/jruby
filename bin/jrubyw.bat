@echo off

rem This script just makes jruby.bat run with javaw

set JAVA_COMMAND=javaw
call "%~dp0_jrubyvars" %*

%_STARTJAVA% %_VM_OPTS% -cp "%CLASSPATH%" -Djruby.base="%JRUBY_BASE%" -Djruby.home="%JRUBY_HOME%" -Djruby.lib="%JRUBY_HOME%\lib" -Djruby.shell="cmd.exe" -Djruby.script=jruby.bat org.jruby.Main %JRUBY_OPTS% %_RUBY_OPTS%
set E=%ERRORLEVEL%

call "%~dp0_jrubycleanup"
