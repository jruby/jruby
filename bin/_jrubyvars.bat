@echo off
rem Environment Variable Prequisites:
rem
rem   JRUBY_BASE    (Optional) Base directory for resolving dynamic portions
rem                 of a JRuby installation.  If not present, resolves to
rem                 the same directory that JRUBY_HOME points to.
rem
rem   JRUBY_HOME    (Optional) May point at your JRuby "build" directory.
rem                 If not present, the current working directory is assumed.
rem
rem   JRUBY_OPTS    (Optional) Default JRuby command line args.
rem
rem   JAVA_HOME     Must point at your Java Development Kit installation.
rem

rem ----- Save Environment Variables That May Change --------------------------

set _JRUBY_BASE=%JRUBY_BASE%
set _JRUBY_HOME=%JRUBY_HOME%
set _CLASSPATH=%CLASSPATH%
set _CP=%CP%


rem ----- Verify and Set Required Environment Variables -----------------------

if not "%JAVA_HOME%" == "" goto gotJava
echo You must set JAVA_HOME to point at your Java Development Kit installation
goto cleanup
:gotJava

if not "%JRUBY_HOME%" == "" goto gotHome
set JRUBY_HOME=%~dp0..
:gotHome

if not "%JRUBY_BASE%" == "" goto gotBase
set JRUBY_BASE=%JRUBY_HOME%
:gotBase


rem ----- Prepare Appropriate Java Execution Commands -------------------------

if not "%JAVA_COMMAND%" == "" goto gotCommand
set _JAVA_COMMAND=%JAVA_COMMAND%
set JAVA_COMMAND=java
:gotCommand

if not "%OS%" == "Windows_NT" goto noTitle
rem set _STARTJAVA=start "JRuby" "%JAVA_HOME%\bin\java"
set _STARTJAVA=%JAVA_HOME%\bin\%JAVA_COMMAND%
goto gotTitle
:noTitle
rem set _STARTJAVA=start "%JAVA_HOME%\bin\java"
set _STARTJAVA=%JAVA_HOME%\bin\%JAVA_COMMAND%
:gotTitle

rem ----- Set up the VM options
call "%~dp0_jrubyvmopts" %*
set _RUNJAVA="%JAVA_HOME%\bin\java"

rem ----- Set Up The Runtime Classpath ----------------------------------------

for %%i in ("%JRUBY_HOME%\lib\*.jar") do @call :setcp %%i

if not "%CLASSPATH%" == "" goto gotCP
set CLASSPATH=%CP%
goto doneCP
:gotCP
set CLASSPATH=%CP%;%CLASSPATH%
:doneCP

goto :EOF

rem Setcp subroutine
:setcp
if not "%CP%" == "" goto add

set CP=%*
goto :EOF

:add
set CP=%CP%;%*

goto :EOF

rem echo Using JRUBY_BASE: %JRUBY_BASE%
rem echo Using JRUBY_HOME: %JRUBY_HOME%
rem echo Using CLASSPATH:  %CLASSPATH%
rem echo Using JAVA_HOME:  %JAVA_HOME%
rem echo Using Args:       %*
