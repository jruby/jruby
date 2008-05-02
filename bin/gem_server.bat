@echo off
setlocal
rem ---------------------------------------------------------------------------

call "%~dp0_jrubyvars" %*

if %JRUBY_BAT_ERROR%==0 "%_STARTJAVA%" %_VM_OPTS% -Xbootclasspath/a:"%CP%" -cp "%CLASSPATH%" -Djruby.home="%JRUBY_HOME%" -Djruby.lib="%JRUBY_HOME%\lib" -Djruby.shell="cmd.exe" -Djruby.script=jruby.bat org.jruby.Main %JRUBY_OPTS% "%JRUBY_HOME%\bin\gem_server" %_RUBY_OPTS%
set E=%ERRORLEVEL%

call "%~dp0_jrubycleanup"
endlocal