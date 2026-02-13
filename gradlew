#!/usr/bin/env sh
#
# Minimal Gradle wrapper launcher for Unix-like systems.
#
set -eu

DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

# Prefer JAVA_HOME if set; otherwise fall back to java on PATH.
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
else
  JAVA_CMD="java"
fi

WRAPPER_JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "ERROR: Missing $WRAPPER_JAR" >&2
  exit 1
fi

exec "$JAVA_CMD" -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
