#!/bin/sh

# Clean up & Compile, build, install the jar file in the Local maven repo
mvn clean install

# Delete existing entries in the target directory
rm -rf ~/Desktop/test_run/*

# Copy the jar file
cp ./target/DataBrowser-1.0.jar ~/Desktop/test_run/

# Copy resources
cp -r src/main/resources ~/Desktop/test_run/

# Using the "Maven maven-dependency-plugin", all the required dependencies are copied to
# "target/lib" directory. All these are required during run time. As the run time class path is mentioned
# as "./resources/lib/" in the pom.xml file, the "lib" folder is execpted to be present in "Run time folder/resources".
# This copy step copies the Project runtime dependencies to resources folder.
cp -r ./target/lib ~/Desktop/test_run/resources/

# This project also needs the resources required to execute "DBUtils" module.
cp -r /c/Users/ryand/Desktop/dbutils/src/main/resources/* ~/Desktop/test_run/resources/

cd ~/Desktop/test_run/
export LOG4J_CONFIGURATION_FILE="C:\Users\ryand\Desktop\test_run\resources\log4j2-browser.xml"
java --module-path $PATH_TO_FX --add-modules javafx.controls --add-modules javafx.fxml -jar ./DataBrowser-1.0.jar