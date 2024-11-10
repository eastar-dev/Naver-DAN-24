#!/bin/bash
clear
echo '== start init ==================================================================='
BUILD_VARIANT=DevDebug
source "$(cd "$(dirname "$0")" && pwd -P)"/common_function.sh
java_setting
project_setting
echo "${yyyymmdd_hhmmss}" > "${project_root}/app/src/debug/assets/build_time.txt"

echo '== start gradlew bundle build ==================================================='
#./gradlew clean
bundle_build
make_universal_apk
build_result_setting

echo "== start upload ================================================================="
upload_setting
upload_apk

dump

