@echo off

set _MEM=-Xmx500m
if not defined JAVA_MEM goto memOptDone
set _MEM=%JAVA_MEM%
:memOptDone

set _STK=-Xss1024k
if not defined JAVA_STACK goto stackOptDone
set _STK=%JAVA_STACK%
:stackOptDone

set _VM_OPTS=
set _RUBY_OPTS=
set _DFLT_VM_OPTS=%JAVA_OPTS%
set _JAVA_VM=-client

rem
rem Can you believe I'm rewriting batch arg processing in batch files because batch
rem file arg processing sucks so bad? Can you believe this is even possible?
rem http://support.microsoft.com/kb/71247

rem escape any quotes. use -q == ", -d == '.
set _ARGS=%*
if not defined _ARGS goto vmoptsDone
set _ARGS=%_ARGS:'=-d%
set _ARGS=%_ARGS:"=-q%
rem prequote all args for 'for' statement
set _ARGS="%_ARGS%"

:vmoptsLoop
rem split args by spaces into first and rest
for /f "tokens=1,*" %%i in (%_ARGS%) do call :getarg "%%i" "%%j"
goto procarg

:getarg
rem remove quotes around first arg
for %%i in (%1) do set _CMP=%%~i
rem set the rest args (note, they're all quoted and ready to go)
set _ARGS=%2
rem return to line 18
goto :EOF

:procarg
if [%_CMP%] == [] goto vmoptsDone

if [%_CMP%] == [--server] (
  set _JAVA_VM=-server
  goto :vmoptsNext
)

if [%_CMP%] == [--client] (
  set _JAVA_VM=-client
  goto :vmoptsNext
)

if [%_CMP%] == [--jdb] (
  set _STARTJAVA=%JAVA_HOME%\bin\jdb
  goto :vmoptsNext
)

if [%_CMP%] == [--sample] (
  set _CMP=-J-Xprof
  goto :jvmarg
)

if [%_CMP%] == [--manage] (
  set _CMP=-J-Dcom.sun.management.jmxremote
  goto :jvmarg
)

if [%_CMP%] == [--1.9] (
  set _CMP=-J-Djruby.compat.version=RUBY1_9
  goto :jvmarg
)

if [%_CMP%] == [--1.8] (
  set _CMP=-J-Djruby.compat.version=RUBY1_8
  goto :jvmarg
)

rem now unescape -q and -d
set _CMP=%_CMP:-q="%
set _CMP=%_CMP:-d='%
set _CMP1=%_CMP:~0,1%
set _CMP2=%_CMP:~0,2%

rem detect first character is a quote; skip directly to rubyarg
rem this avoids a batch syntax error
if "%_CMP1:"=\\%" == "\\" goto rubyarg

rem removing quote avoids a batch syntax error
if "%_CMP2:"=\\%" == "-J" goto jvmarg

:rubyarg
set _RUBY_OPTS=%_RUBY_OPTS% %_CMP%
goto vmoptsNext

:jvmarg
set _VAL=%_CMP:~2%

if "%_VAL:~0,4%" == "-Xmx" (
  set _MEM=%_VAL%
  goto vmoptsNext
)

if "%_VAL:~0,4%" == "-Xss" (
  set _STK=%_VAL%
  goto vmoptsNext
)

rem Make sure the older way to specify server VM
rem is still supported.
if [%_VAL%] == [-server] (
  set _JAVA_VM=-server
  goto vmoptsNext
)

set _VM_OPTS=%_VM_OPTS% %_VAL%

:vmoptsNext
set _CMP=
goto vmoptsLoop

:vmoptsDone
set _VM_OPTS=%_VM_OPTS% %_JAVA_VM% %_MEM% %_STK% %_DFLT_VM_OPTS%
set _DFLT_VM_OPTS=
set _MEM=
set _STK=
set _ARGS=
set _VAL=
set _CMP=
set _CMP1=
set _JAVA_VM=
