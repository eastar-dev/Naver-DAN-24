#!/bin/bash
clear
source "$(cd "$(dirname "$0")" && pwd -P)"/common_function.sh

java_setting
cd $project_root

./gradlew :tool:deploy_server:shadowJar
mkdir -p $HOME/Desktop/deploy/conf
cp $project_root/tool/deploy_server/build/libs/deploy_server.jar $HOME/Desktop/deploy/conf/deploy_server.jar
cp $project_root/tool/deploy_server/restart.sh.command $HOME/Desktop/deploy/conf/restart.sh.command
cp $project_root/tool/deploy_server/start.sh.command $HOME/Desktop/deploy/conf/start.sh.command
cp $project_root/tool/deploy_server/stop.sh.command $HOME/Desktop/deploy/conf/stop.sh.command
#pause
