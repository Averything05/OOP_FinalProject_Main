#!/bin/sh
# Compiles the project with plain javac and runs it. Run this from the project folder.
rm -rf out
mkdir out
javac -d out $(find src -name "*.java") && java -cp out app.Main
