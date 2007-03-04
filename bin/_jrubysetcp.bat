@echo off
if not "%CP%" == "" goto add

set CP=%*
goto done

:add
set CP=%CP%;%*

:done