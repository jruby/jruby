@echo off
set _MEM=-Xmx256m
set _STK=-Xss1024k
set _VM_OPTS=
set _RUBY_OPTS=
set _DFLT_VM_OPTS=-Xverify:none -da

:vmoptsLoop
set _ARG=%1
set _CMP=
rem remove surrounding quotes
for %%i in (%_ARG%) do set _CMP=%%~i
if "%_CMP%" == "" goto vmoptsDone

if not "%_CMP:~0,2%" == "-J" (
  set _RUBY_OPTS=%_RUBY_OPTS% %_ARG%
  goto vmoptsNext
)
set _VAL=%_ARG:~2%
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
shift
goto vmoptsLoop

:vmoptsDone
set _VM_OPTS=%_VM_OPTS% %_MEM% %_STK% %_DFLT_VM_OPTS%
set _DFLT_VM_OPTS=
set _MEM=
set _STK=
set _ARG=
set _VAL=
set _CMP=
