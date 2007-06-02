@echo off

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
set _VM_OPTS=
set _RUBY_OPTS=
:finish
exit /b %E%
