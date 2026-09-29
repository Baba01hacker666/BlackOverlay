#!/bin/sh
# Gradle wrapper script (simplified, works with gradle-wrapper.jar next to properties)
APP_BASE_NAME=${0##*/}
APP_HOME=$(cd "${0%/*}" >/dev/null && pwd)

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
