@echo off

rem This script just makes jruby.bat run with javaw

rem ----- Save Environment Variables That May Change --------------------------

set _JRUBY_BASE=%JRUBY_BASE%
set _JRUBY_HOME=%JRUBY_HOME%
set _CLASSPATH=%CLASSPATH%
set _CP=%CP%
set _JAVA_COMMAND=%JAVA_COMMAND%

rem ----- Verify and Set Required Environment Variables -----------------------
set JAVA_COMMAND=javaw

if not "%JAVA_HOME%" == "" goto gotJava
echo You must set JAVA_HOME to point at your Java Development Kit installation
goto cleanup
:gotJava

if not "%JRUBY_HOME%" == "" goto gotHome
set JRUBY_HOME=.
if exist "%JRUBY_HOME%\lib\jruby.jar" goto okHome
set JRUBY_HOME=..
:gotHome
if exist "%JRUBY_HOME%\lib\jruby.jar" goto okHome
echo Cannot find jruby.jar in %JRUBY_HOME%\lib
echo Please check your JRUBY_HOME setting
goto cleanup
:okHome

if not "%JRUBY_BASE%" == "" goto gotBase
set JRUBY_BASE=%JRUBY_HOME%
:gotBase

%JRUBY_HOME%\bin\jruby.bat %*

rem ----- Restore Environment Variables ---------------------------------------

:cleanup
set JRUBY_BASE=%_JRUBY_BASE%
set _JRUBY_BASE=
set JRUBY_HOME=%_JRUBY_HOME%
set _JRUBY_HOME=
set CLASSPATH=%_CLASSPATH%
set _CLASSPATH=
set CP=%_CP%
set JAVA_COMMAND=%_JAVA_COMMAND%
set _LIBJARS=
set _RUNJAVA=
set _STARTJAVA=
set _JAVA_COMMAND=
:finish
exit /b %E%
