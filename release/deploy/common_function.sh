#!/bin/bash
#################################################################################
# const #########################################################################
#################################################################################
APP=app

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

#################################################################################
# PROJECT ROOT SET   ############################################################
#################################################################################
function project_setting() {
    cd $project_root

    if [ ! -f "${project_root}/gradlew" ]; then
        die "!!current DIR must have gradlew"
    fi

    DEPLOY_DIR="$project_root/release/deploy"
    OUTPUT_DIR="$project_root/$APP/build/outputs"
    OUTPUT_AAB="$OUTPUT_DIR/bundle/$VARIANT_CAMEL/app-$VARIANT_KEBAB.aab"
    OUTPUT_MAPPING="$OUTPUT_DIR/mapping/$VARIANT_CAMEL/mapping.txt"
    OUTPUT_METADATA="$project_root/$APP/build/intermediates/packaged_manifests/$VARIANT_CAMEL/process${VARIANT_PASCAL}ManifestForPackage/output-metadata.json"
    OUTPUT_APKS="$OUTPUT_AAB.apks"
    UNIVERSAL_APK="$OUTPUT_AAB.apk"
    yyyymmdd_hhmmss=$(date '+%Y%m%d_%H%M%S')

    echo "project_root   = ${project_root}   "
    echo "DEPLOY_DIR     = ${DEPLOY_DIR}     "
    echo "OUTPUT_DIR     = ${OUTPUT_DIR}     "
    echo "OUTPUT_AAB     = ${OUTPUT_AAB}     "
    echo "OUTPUT_APKS    = ${OUTPUT_APKS}    "
    echo "UNIVERSAL_APK  = ${UNIVERSAL_APK}  "
    echo "OUTPUT_MAPPING = ${OUTPUT_MAPPING} "
    echo "OUTPUT_METADATA= ${OUTPUT_METADATA}"
    echo "VERSION_GRADLE = ${VERSION_GRADLE} "
    echo "yyyymmdd_hhmmss = ${yyyymmdd_hhmmss} "

#    VERSION_GRADLE="$project_root/android.gradle"
#    version_name=$(cat "${VERSION_GRADLE}" | grep versionName | awk '{ print $3 }' | cut -d\" -f2)
#    version_code=$(cat "${VERSION_GRADLE}" | grep versionCode | awk '{ print $3 }' | cut -d, -f1)
#    echo "version_name   = ${version_name}  "
#    echo "version_code   = ${version_code}  "
}

#################################################################################
# util ##########################################################################
#################################################################################
function divider() {
    echo "== $1 =="
}
function warn() {
    echo "$*"
}

function die() {
    echo
    echo "$*"
    echo
    exit 1
}
function toArray() {
    echo "$1" | sed -r 's/([a-z0-9])([A-Z])/\1 \2/g' | tr '[:upper:]' '[:lower:]' | tr '_' ' ' | tr '-' ' '
}
function camelCase() {
    result=$(PascalCase "$1")
    echo "$(echo "${result:0:1}" | tr '[:upper:]' '[:lower:]')${result:1}"
}

function PascalCase() {
    local arr=($(toArray "$1"))

    result=""
    for i in "${arr[@]}"; do
        i="$(echo "${i:0:1}" | tr '[:lower:]' '[:upper:]')${i:1}"
        result="$result${i}"
    done

    echo "$result"
}
function snake_case() {
    local arr=($(toArray "$1"))

    result=""
    for i in "${arr[@]}"; do
        result="${result}_${i}"
    done

    echo "${result:1}"
}
function kebab_case() {
    result=$(snake_case "$1")
    echo $result | tr '_' '-'
}

function find_file_in_parent_dir() {
    local _current="$1"
    local _target="$2"

    while true; do
        if [ -f "$_current/$_target" ]; then
            echo "$_current"
            return 0
        fi
        if [ "$_current" = "/" ]; then
            echo "파일을 찾을 수 없습니다."
            return 1
        fi
        _current=$(dirname "$_current")
    done
}


#################################################################################
# Build #########################################################################
#################################################################################
function bundle_build() {
    ./gradlew ":$APP:bundle$VARIANT_PASCAL"
    if [ $? -ne 0 ]; then
        die "!!may be buile fail?"
    fi
    if [ ! -f "${OUTPUT_AAB}" ]; then
        die "!!may be buile fail?
                file not found any aab file in ${OUTPUT_AAB}"
    fi
}

function build_result_setting() {
    version_name=$(cat "${OUTPUT_METADATA}" | grep versionName | awk '{ print $2 }' | cut -d\" -f2)
    version_name_s=$(echo "$version_name" | cut -d- -f1) # 1.0.0-rc1 -> 1.0.0
    version_code=$(cat "${OUTPUT_METADATA}" | grep versionCode | awk '{ print $2 }' | cut -d, -f1)
    variant_name=$(cat "${OUTPUT_METADATA}" | grep variantName | awk '{ print $2 }' | cut -d\" -f2)

    echo "version_name   = ${version_name}  "
    echo "version_name_s = ${version_name_s}"
    echo "version_code   = ${version_code}  "
    echo "variant_name   = ${variant_name}  "
}

#################################################################################
# Universal Apk #################################################################
#################################################################################
function make_universal_apk() {
    echo --make_universal_apk ----------------------------------------------------
    BUNDLETOOL="$DEPLOY_DIR/bundletool.jar"

    KEYSTORE_FILE="$project_root/release.jks"
    KEYSTORE_PASS="qqqqqqqq"
    KEY_ALIAS="key0"

    if [ -f "${OUTPUT_APKS}" ]; then
        rm -f "${OUTPUT_APKS}"
        echo -- deleted apks "${OUTPUT_APKS}"
    fi
    if [ -f "${UNIVERSAL_APK}" ]; then
        rm -f "${UNIVERSAL_APK}"
        echo -- deleted apks "${UNIVERSAL_APK}"
    fi

    "$JAVACMD" -jar "${BUNDLETOOL}" build-apks --bundle="${OUTPUT_AAB}" --output="${OUTPUT_APKS}" --ks="${KEYSTORE_FILE}" --ks-pass=pass:${KEYSTORE_PASS} --ks-key-alias=${KEY_ALIAS} --mode=universal

    unzip -o "${OUTPUT_APKS}"

    rm -f "${OUTPUT_APKS}"
    rm -f toc.pb
    mv universal.apk ${UNIVERSAL_APK}
}

function check_server() {
    local url=$1
    local timeout=5
    local http_code

    # curl을 사용하여 서버 상태 확인
    http_code=$(curl -o /dev/null -s -w "%{http_code}" -m "$timeout" "$url")

    # HTTP 상태 코드가 200이면 성공, 그 외에는 실패
    if [ "$http_code" -eq 200 ]; then
        echo "true"
    else
        echo "false"
    fi
}

function check_ip() {
    local ip_to_check=$1

    if ifconfig | grep -q "$ip_to_check"; then
        echo "true"
    else
        echo "false"
    fi
}

#pause
function pause() {
    echo "Press any key to continue..."
    read -n1 -s
}

function upload_setting() {
    server_url="http://xxx.xxx.xxx.xxx" #서버주소
    upload_server_url="${server_url}" #업로드주소
    upload_rootpath="$HOME/Desktop/deploy"
    local_server=$(check_server "http://127.0.0.1/live")


    # 업로드 파일
    upload_apk="${UNIVERSAL_APK}"
    upload_aab="${OUTPUT_AAB}"
    upload_mapping="${OUTPUT_MAPPING}"
    # 업로드 이름
#    upload_apk_filename="${version_name}/${PROJECT_POM_ARTIFACT_ID}-${version_name}-${yyyymmdd_hhmmss}.apk"
#    upload_aabapk_filename="${version_name}/${PROJECT_POM_ARTIFACT_ID}-${version_name}.apk"
#    upload_aab_filename="${version_name}/${PROJECT_POM_ARTIFACT_ID}-${version_name}.aab"
#    upload_mapping_filename="${version_name}/${PROJECT_POM_ARTIFACT_ID}-${version_name}.mapping"
    upload_apk_filename="${PROJECT_POM_ARTIFACT_ID}-${version_name}-${yyyymmdd_hhmmss}.apk"
    upload_aabapk_filename="${PROJECT_POM_ARTIFACT_ID}-${version_name}.apk"
    upload_aab_filename="${PROJECT_POM_ARTIFACT_ID}-${version_name}.aab"
    upload_mapping_filename="${PROJECT_POM_ARTIFACT_ID}-${version_name}.mapping"

    upload_apk_path="${upload_rootpath}/${version_name}/${upload_apk_filename}"
    upload_aabapk_path="${upload_rootpath}/${version_name}/${upload_aabapk_filename}"
    upload_aab_path="${upload_rootpath}/${version_name}/${upload_aab_filename}"
    upload_mapping_path="${upload_rootpath}/${version_name}/${upload_mapping_filename}"

    download_apk_url="${server_url}/${version_name}/${upload_apk_filename}"
    download_aabapk_url="${server_url}/${version_name}/${upload_aabapk_filename}"
    download_aab_url="${server_url}/${version_name}/${upload_aab_filename}"
    download_mapping_url="${server_url}/${version_name}/${upload_mapping_filename}"

    echo "local_server         = ${local_server}         "
    if [ "$local_server" = "true" ]; then
        echo "upload_apk           = ${upload_apk}           "
        echo "upload_apk_path      = ${upload_apk_path}      "
        echo "upload_apk           = ${upload_apk}           "
        echo "upload_aabapk_path   = ${upload_aabapk_path}   "
        echo "upload_aab           = ${upload_aab}           "
        echo "upload_aab_path      = ${upload_aab_path}      "
        echo "upload_mapping       = ${upload_mapping}       "
        echo "upload_mapping_path  = ${upload_mapping_path}  "
    else
        echo "upload_apk           = ${upload_apk}           "
        echo "upload_apk_url       = ${download_apk_url}       "
        echo "upload_apk           = ${upload_apk}           "
        echo "upload_aabapk_url    = ${download_aabapk_url}    "
        echo "upload_aab           = ${upload_aab}           "
        echo "upload_aab_url       = ${download_aab_url}       "
        echo "upload_mapping       = ${upload_mapping}       "
        echo "upload_mapping_url   = ${download_mapping_url}   "
    fi
}

function upload_apk() {
    if [ "$local_server" = "true" ]; then
        mkdir -p "$(dirname ${upload_apk_path})"
        cp -f ${upload_apk} ${upload_apk_path}
    else
        curl -F "file=@${upload_apk};filename=${upload_apk_filename}" "${upload_server_url}"
    fi
}

function upload_aabapk() {
    if [ "$local_server" = "true" ]; then
        mkdir -p "$(dirname ${upload_aabapk_path})"
        cp -f ${upload_apk} ${upload_aabapk_path}
    else
        curl -F "file=@${upload_apk};filename=${upload_aabapk_filename}" "${upload_server_url}"
    fi
}

function upload_aab() {
    if [ "$local_server" = "true" ]; then
        mkdir -p "$(dirname ${upload_aab_path})"
        cp -f ${upload_aab} ${upload_aab_path}
    else
        curl -F "file=@${upload_aab};filename=${upload_aab_filename}" "${upload_server_url}"
    fi
}

function upload_mapping() {
    if [ "$local_server" = "true" ]; then
        mkdir -p "$(dirname ${upload_mapping_path})"
        cp -f ${upload_mapping} ${upload_mapping_path}
    else
        curl -F "file=@${upload_mapping};filename=${upload_mapping_filename}" "${upload_server_url}"
    fi
}

#################################################################################
# dump ##########################################################################
#################################################################################
function dump() {
    echo
    echo "======================================================================================="
    echo "== end ================================================================================"
    echo "JAVACMD              = ${JAVACMD}                 "
    echo "project_root         = ${project_root}            "
    echo "DEPLOY_DIR           = ${DEPLOY_DIR}              "
    echo "OUTPUT_DIR           = ${OUTPUT_DIR}              "
    echo "OUTPUT_AAB           = ${OUTPUT_AAB}              "
    echo "OUTPUT_APKS          = ${OUTPUT_APKS}             "
    echo "UNIVERSAL_APK        = ${UNIVERSAL_APK}           "
    echo "OUTPUT_MAPPING       = ${OUTPUT_MAPPING}          "
    echo "OUTPUT_METADATA      = ${OUTPUT_METADATA}         "
    echo "version_name         = ${version_name}            "
    echo "version_name_s       = ${version_name_s}          "
    echo "version_code         = ${version_code}            "
    echo "variant_name         = ${variant_name}            "
    echo "ARTIFACT_ID          = ${PROJECT_POM_ARTIFACT_ID} "
    echo "---------------------------------------------------------------------------------------"
    echo "aab_url              = ${aab_url}                 "
    echo "apk_url              = ${apk_url}                 "
    echo "mapping_url          = ${mapping_url}             "
    echo "---------------------------------------------------------------------------------------"
    echo "local_server         = ${local_server}            "
    echo "upload_apk           = ${upload_apk}              "
    echo "upload_apk           = ${upload_apk}              "
    echo "upload_aab           = ${upload_aab}              "
    echo "upload_mapping       = ${upload_mapping}          "
    echo
    echo "upload_apk_path      = ${upload_apk_path}         "
    echo "upload_aabapk_path   = ${upload_aabapk_path}      "
    echo "upload_aab_path      = ${upload_aab_path}         "
    echo "upload_mapping_path  = ${upload_mapping_path}     "
    echo
    echo "download_apk_url     = ${download_apk_url}        "
    echo "download_aabapk_url  = ${download_aabapk_url}     "
    echo "download_aab_url     = ${download_aab_url}        "
    echo "download_mapping_url = ${download_mapping_url}    "
    echo "band_content   = "
    echo "---------------------------------------------------------------------------------------"
    echo "${band_content}"
    echo "---------------------------------------------------------------------------------------"
    echo "======================================================================================="
}

VARIANT_PASCAL=$(PascalCase "${BUILD_VARIANT}")
VARIANT_CAMEL=$(camelCase "${BUILD_VARIANT}")
VARIANT_SNAKE=$(snake_case "${BUILD_VARIANT}")
VARIANT_KEBAB=$(kebab_case "${BUILD_VARIANT}")
#echo "VARIANT_PASCAL: ${VARIANT_PASCAL}"
#echo "VARIANT_CAMEL: ${VARIANT_CAMEL}"
#echo "VARIANT_SNAKE: ${VARIANT_SNAKE}"
#echo "VARIANT_KEBAB: ${VARIANT_KEBAB}"

module_root_file="build.gradle.kts"
project_root_file="gradlew"
this_folder="$(cd "$(dirname "$0")" && pwd -P)"
project_root=$(find_file_in_parent_dir "$this_folder" "$project_root_file")

echo "-----------------------------------------------------------------------------"
echo "this_folder       = $this_folder"
echo "project_root      = $project_root"
echo "-----------------------------------------------------------------------------"
