@echo off
set _MEM=-Xmx378m
set _STK=-Xss1024k
set _VM_OPTS=
set _RUBY_OPTS=
set _DFLT_VM_OPTS=-Xverify:none

rem
rem Can you believe I'm rewriting batch arg processing in batch files because batch
rem file arg processing sucks so bad? Can you believe this is even possible?
rem http://support.microsoft.com/kb/71247

rem escape any quotes. use -q == ", -d == -.
set _ARGS=%*
if not defined _ARGS goto vmoptsDone
set _ARGS=%_ARGS:-=-d%
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
if "%_CMP%" == "" goto vmoptsDone

rem now unescape -q and -d
set _CMP=%_CMP:-q="%
set _CMP=%_CMP:-d=-%
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

set _VM_OPTS=%_VM_OPTS% %_VAL%

:vmoptsNext
set _CMP=
goto vmoptsLoop

:vmoptsDone
set _VM_OPTS=%_VM_OPTS% %_MEM% %_STK% %_DFLT_VM_OPTS%
set _DFLT_VM_OPTS=
set _MEM=
set _STK=
set _ARGS=
set _VAL=
set _CMP=
set _CMP1=