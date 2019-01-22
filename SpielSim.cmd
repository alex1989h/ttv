@echo off
echo Starte Spielsimulation
SET CLASS_NAME=bin/ttv/SchiffeVersenken.class
SET JAR_NAME=Schiffeversenken.jar

if exist %CLASS_NAME% (
    cd bin
	start java -cp ../lib/californium-core-1.1.0.jar;../lib/element-connector-1.1.0.jar;../lib/log4j-1.2.17.jar; ttv.SchiffeVersenken localhost:8080 localhost:8080 localhost/led yes
	start java -cp ../lib/californium-core-1.1.0.jar;../lib/element-connector-1.1.0.jar;../lib/log4j-1.2.17.jar; ttv.SchiffeVersenken localhost:8181 localhost:8080 localhost/led no
	java -cp ../lib/californium-core-1.1.0.jar;../lib/element-connector-1.1.0.jar;../lib/log4j-1.2.17.jar; ttv.SchiffeVersenken localhost:8282 localhost:8080 localhost/led no
) else if exist %JAR_NAME% (
	echo %CLASS_NAME% nicht gefunden, versuche %JAR_NAME%
	start java -jar Schiffeversenken.jar localhost:8080 localhost:8080 localhost/led yes
	start java -jar Schiffeversenken.jar localhost:8181 localhost:8080 localhost/led no
	java -jar Schiffeversenken.jar localhost:8282 localhost:8080 localhost/led no
) else (
	echo %JAR_NAME% und %CLASS_NAME% nicht gefunden
)
pause