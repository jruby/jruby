@echo off
if not "%CP%" == "" goto add

set CP=%1
goto done

:add
set CP=%CP%;%1

:done