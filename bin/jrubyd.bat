@echo off
rem ---------------------------------------------------------------------------
rem jruby.bat - Start Script for the JRuby Interpreter
rem
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
rem ---------------------------------------------------------------------------


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
set JRUBY_HOME=.
if exist "%JRUBY_HOME%\lib\jruby.jar" goto okHome
set JRUBY_HOME=..
:gotHome
if exist "%JRUBY_HOME%\lib\jruby.jar" goto okHome
echo Cannot find jruby.bat in %JRUBY_HOME% 
echo Please check your JRUBY_HOME setting
goto cleanup
:okHome

if not "%JRUBY_BASE%" == "" goto gotBase
set JRUBY_BASE=%JRUBY_HOME%
:gotBase


rem ----- Prepare Appropriate Java Execution Commands -------------------------

if not "%OS%" == "Windows_NT" goto noTitle
rem set _STARTJAVA=start "JRuby" "%JAVA_HOME%\bin\java"
set _STARTJAVA="%JAVA_HOME%\bin\java"
goto gotTitle
:noTitle
rem set _STARTJAVA=start "%JAVA_HOME%\bin\java"
set _STARTJAVA="%JAVA_HOME%\bin\java"
:gotTitle

set _RUNJAVA="%JAVA_HOME%\bin\java"
rem ----- Set Up The Runtime Classpath ----------------------------------------

set CP=%JRUBY_HOME%\jruby.jar;%JAVA_HOME%\lib\tools.jar
set CLASSPATH=%CP%
echo Using JRUBY_BASE: %JRUBY_BASE%
echo Using JRUBY_HOME: %JRUBY_HOME%
echo Using CLASSPATH:  %CLASSPATH%
echo Using JAVA_HOME:  %JAVA_HOME%


rem ----- Execute The Requested Command ---------------------------------------

%_STARTJAVA%  -Xdebug -Xrunjdwp:transport=dt_shmem,server=y,suspend=y  -Djruby.base="%JRUBY_BASE%" -Djruby.home="%JRUBY_HOME%" -Djruby.lib="%JRUBY_BASE%\lib" -Djruby.script="jruby.bat" -Djruby.shell="cmd.exe" org.jruby.Main %JRUBY_OPTS% %1 %2 %3 %4 %5 %6 %7 %8 %9

rem ----- Restore Environment Variables ---------------------------------------

:cleanup
set JRUBY_BASE=%_JRUBY_BASE%
set _JRUBY_BASE=
set JRUBY_HOME=%_JRUBY_HOME%
set _JRUBY_HOME=
set CLASSPATH=%_CLASSPATH%
set _CLASSPATH=
set CP=%_CP%
set _LIBJARS=
set _RUNJAVA=
set _STARTJAVA=
:finish
