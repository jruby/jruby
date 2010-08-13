@IF NOT ["%JAVA_HOME%"]==[""] @GOTO :set_jdk_includes

@FOR %%i IN (javac.exe) DO @SET JAVA_HOME=%%~$PATH:i
@SET JAVA_HOME=%JAVA_HOME%\..\..

:set_jdk_includes
@SET JDK_INCLUDES=-I"%JAVA_HOME%\include\" -I"%JAVA_HOME%\include\win32" -I"%JAVA_HOME%\include\win64"
@SET JDK_INCLUDES=%JDK_INCLUDES:\=\\%

@ECHO JAVA_HOME=%JAVA_HOME%
@ECHO JDK_INCLUDES=%JDK_INCLUDES%

@IF [%1]==[] GOTO :plain_make
make.exe %1
@GOTO :end

:plain_make
make.exe

:end


