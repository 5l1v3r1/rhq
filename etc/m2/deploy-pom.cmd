@echo off

if "%1" == "" goto USAGE

setlocal
set POM_FILE=%1
if "%RHQ_M2_REPO%" == "" set RHQ_M2_REPO=%USERPROFILE%\.m2\repository

echo mvn deploy:deploy-file -Dfile="%POM_FILE%" -Dpackaging=pom -DgeneratePom=false -DpomFile="%POM_FILE%" -Durl="file://%RHQ_M2_REPO%" -DrepositoryId=local
mvn deploy:deploy-file -Dfile="%POM_FILE%" -Dpackaging=pom -DgeneratePom=false -DpomFile="%POM_FILE%" -Durl="file://%RHQ_M2_REPO%" -DrepositoryId=local

endlocal
exit /b 0

:USAGE
echo Usage: deploy-pom path/to/pomfile >&2
echo. >&2
echo For example: deploy-pom pom.xml >&2
exit /b 1
