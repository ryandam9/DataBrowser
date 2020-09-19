#!/bin/sh

# Set Paths
export JAVA_HOME="/c/Users/ravis/Desktop/software/jdk-14"
export PATH="/c/Users/ravis/Desktop/software/jdk-14/bin":$PATH
export PATH_TO_FX="/c/Users/ravis/Desktop/software/javafx-sdk-15/lib"

# Clean up & Compile, build, install the jar file in the Local maven repo
mvn clean install

# Delete existing entries in the target directory
rm -rf ~/Desktop/db_browser_run/*

# Copy the jar file
cp ./target/DataBrowser-1.0.jar ~/Desktop/db_browser_run/

# Copy resources
cp -r src/main/resources ~/Desktop/db_browser_run/

# Using the "Maven maven-dependency-plugin", all the required dependencies are copied to
# "target/lib" directory. All these are required during run time. As the run time class path is mentioned
# as "./resources/lib/" in the pom.xml file, the "lib" folder is expected to be present in "Run time folder/resources".
# This copy step copies the Project runtime dependencies to resources folder.
cp -r ./target/lib ~/Desktop/db_browser_run/resources/

# This project also needs the resources required to execute "DBUtils" module.
cp -r /c/Users/ravis/Desktop/projects/dbutils/src/main/resources/* ~/Desktop/db_browser_run/resources/