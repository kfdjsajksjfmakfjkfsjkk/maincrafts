#!/bin/sh
DIRNAME=`dirname "$0"`
APP_BASE_NAME=`basename "$0"`
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
CLASSPATH=$DIRNAME/gradle/wrapper/gradle-wrapper.jar
exec java $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS "-Dorg.gradle.appname=$APP_BASE_NAME" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
