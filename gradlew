#!/bin/sh
# Gradle start script (simplified for CI / local)

##############################################################################
# Gradle start up script for POSIX generated for SoThuChi
##############################################################################

APP_HOME=$( cd "${0%/*}" && pwd -P ) || exit
APP_NAME="Gradle"
APP_BASE_NAME=${0##*/}
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Find java
if [ -n "$JAVA_HOME" ] ; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Download wrapper jar if missing
if [ ! -f "$CLASSPATH" ]; then
  echo "Downloading gradle-wrapper.jar..."
  mkdir -p "$APP_HOME/gradle/wrapper"
  curl -fsSL -o "$CLASSPATH" \
    "https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradle/wrapper/gradle-wrapper.jar" \
    || curl -fsSL -o "$CLASSPATH" \
    "https://github.com/gradle/gradle/raw/v8.4.0/gradle/wrapper/gradle-wrapper.jar"
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
