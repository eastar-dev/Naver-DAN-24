#!/bin/bash
clear

#################################################################################
# JAVA SET   ####################################################################
#################################################################################
function java_setting() {
    if [ -n "$JAVA_HOME" ]; then
        # Try IBM's JDK
        # IBM's JDK on AIX uses strange locations for the executables
        IBM_JDK_JAVACMD="$JAVA_HOME/jre/sh/java"
        if [ -x "$IBM_JDK_JAVACMD" ]; then
            JAVACMD="$IBM_JDK_JAVACMD"
            return
        fi

        # Try External JDK
        EXTERNAL_JDK_JAVACMD="$JAVA_HOME/bin/java"
        if [ -x "$EXTERNAL_JDK_JAVACMD" ]; then
            JAVACMD="$EXTERNAL_JDK_JAVACMD"
            return
        fi

        # JAVA_HOME is set, but invalid java command
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME
         Please set the JAVA_HOME variable in your environment to match the
         location of your Java installation."
    else
        # Try Android Studio Embedded JDK (macOS)
        ANDROID_STUDIO_EMBEDDED_JDK_JAVACMD="$(find "/Applications/Android Studio.app" -path "*bin/java")"
        if [ -x "$ANDROID_STUDIO_EMBEDDED_JDK_JAVACMD" ]; then
            JAVACMD="$ANDROID_STUDIO_EMBEDDED_JDK_JAVACMD"
            # You need to set JAVA_HOME as well
            export JAVA_HOME="$(dirname "$(dirname "$JAVACMD")")"
            return
        fi

        # JAVA_HOME is not set, and no java command
        JAVACMD="java"
        which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
            Please set the JAVA_HOME variable in your environment to match the
            location of your Java installation."
    fi
}

java_setting
nohup "${JAVACMD}" -jar $HOME/Desktop/deploy/conf/deploy_server.jar >> $HOME/Desktop/deploy/conf/log.txt 2>&1 &
ps -ef | grep 'deploy_server.jar' | grep -v grep
sleep 5
#echo "Press any key to continue..."
#read -n1 -s
