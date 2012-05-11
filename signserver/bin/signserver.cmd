@echo off

rem library classpath
if "%SIGNSERVER_HOME%" == "" (
  set SIGNSRV_HOME=..
  rem It must work to call both as bin\signserver.cmd or from within bin
  if not exist signserver.cmd set SIGNSRV_HOME=.
 ) else (
    set SIGNSRV_HOME=%SIGNSERVER_HOME%
) 
  
set SIGNSERVER_CP=%SIGNSRV_HOME%\lib\log4j.jar;%SIGNSRV_HOME%\lib\1.6\bcprov-jdk.jar;%SIGNSRV_HOME%\lib\1.6\bcmail-jdk.jar;%SIGNSRV_HOME%\lib\ejbca-util.jar;%SIGNSRV_HOME%\lib\cert-cvc.jar;%SIGNSRV_HOME%\lib\commons-lang-2.0.jar;%SIGNSRV_HOME%\lib\ext\ejb\jboss-ejb3x.jar;%SIGNSRV_HOME%\lib\asm\asm-3.1.jar;%SIGNSRV_HOME%\lib\asm\asm-commons-3.1.jar;%SIGNSRV_HOME%\lib\commons-lang-2.4.jar;%SIGNSRV_HOME%\lib\ext\commons-cli-1.2.jar;%SIGNSRV_HOME%\dist-client\lib\SignServer-Common.jar;%SIGNSRV_HOME%\dist-client\lib\SignServer-ejb.jar
set J2EE_CP=%SIGNSRV_HOME%\dist-client\lib\jbossall-client.jar

set SIGNSERVER_PKG_CP=%SIGNSRV_HOME%\lib\asm-3.1.jar;%SIGNSRV_HOME%\lib\asm-commons-3.1.jar;%SIGNSRV_HOME%\lib\bcmail-jdk.jar;%SIGNSRV_HOME%\lib\bcprov-jdk.jar;%SIGNSRV_HOME%\lib\commons-lang-2.0.jar;%SIGNSRV_HOME%\lib\ejbca-util.jar;%SIGNSRV_HOME%\lib\cert-cvc.jar;%SIGNSRV_HOME%\lib\jbossall-client.jar;%SIGNSRV_HOME%\lib\jboss-ejb3x.jar;%SIGNSRV_HOME%\lib\log4j.jar;%SIGNSRV_HOME%\lib\signserver-cli.jar

rem check that we have built the classes

if not exist %SIGNSRV_HOME%\dist-client\SignServer-AdminCLI.jar  (
    echo You must build SignServer before using the cli, use 'ant'.
    goto end
)


set CLASSPATH=%J2EE_CP%;%SIGNSERVER_CP%;%SIGNSRV_HOME%\bin;%SIGNSRV_HOME%\dist-client\SignServer-AdminCLI.jar;%SIGNSERVER_PKG_CP%
rem echo %CLASSPATH%

if "%JAVA_HOME%" == "" (
  java -cp %CLASSPATH%  org.signserver.cli.signserver %*
) else (
  "%JAVA_HOME%\bin\java" -cp %CLASSPATH% org.signserver.cli.signserver %*
)
:end
