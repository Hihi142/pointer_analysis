@echo off
call gradlew fatJar
del "..\judge\user\submission_jar\*" /Q
copy build\tai-e-all-0.5.1-SNAPSHOT.jar  ..\judge\user\submission_jar\
ren ..\judge\user\submission_jar\tai-e-all-0.5.1-SNAPSHOT.jar submission.jar
cd "..\judge" && wsl ./run_jar.sh
cd "..\Tai-e"
