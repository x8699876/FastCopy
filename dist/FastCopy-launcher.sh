#!/bin/bash

# FastCopy Launcher Script for macOS
# This script finds the installed Java and launches the FastCopy application

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Find Java
if [ -n "$JAVA_HOME" ]; then
    JAVA="$JAVA_HOME/bin/java"
elif [ -x /usr/libexec/java_home ]; then
    # macOS specific
    JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null)
    JAVA="$JAVA_HOME/bin/java"
else
    # Try to use java from PATH
    JAVA=$(which java)
fi

# Check if Java was found
if [ -z "$JAVA" ] || [ ! -x "$JAVA" ]; then
    osascript -e 'display dialog "Java is not installed or not found. Please install Java 8 or later." buttons {"OK"} default button "OK" with icon stop'
    exit 1
fi

# Set up classpath
CLASSPATH="$SCRIPT_DIR/fastcopy-ui.jar"
for jar in "$SCRIPT_DIR"/lib/*.jar; do
    CLASSPATH="$CLASSPATH:$jar"
done

# Launch the application
"$JAVA" -Xmx512m -cp "$CLASSPATH" org.mhisoft.fc.ui.FastCopyMainForm "$@"
