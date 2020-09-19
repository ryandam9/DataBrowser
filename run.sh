#!/bin/sh

# Set Paths
export JAVA_HOME="/c/Users/ravis/Desktop/software/jdk-14"
export PATH="/c/Users/ravis/Desktop/software/jdk-14/bin":$PATH
export PATH_TO_FX="/c/Users/ravis/Desktop/software/javafx-sdk-15/lib"

cd ~/Desktop/db_browser_run/
export LOG4J_CONFIGURATION_FILE="C:\Users\ravis\Desktop\test_run\resources\log4j2-browser.xml"
java --module-path $PATH_TO_FX --add-modules javafx.controls --add-modules javafx.fxml -jar ./DataBrowser-1.0.jar
