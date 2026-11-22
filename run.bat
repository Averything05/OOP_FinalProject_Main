@echo off
rem Compiles the project with plain javac and runs it. Run this from the project folder.
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
if errorlevel 1 exit /b 1
del sources.txt
java -cp out app.Main
