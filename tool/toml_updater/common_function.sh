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
    OUTPUT_METADATA="$project_root/$APP/build/intermediates/merged_manifests/$VARIANT_CAMEL/process${VARIANT_PASCAL}Manifest/output-metadata.json"
    OUTPUT_APKS="$OUTPUT_AAB.apks"
    UNIVERSAL_APK="$OUTPUT_AAB.apk"
    yyyymmddhhmmss=$(date '+%Y-%m-%d %H:%M:%S')

    echo "project_root   = ${project_root}   "
    echo "DEPLOY_DIR     = ${DEPLOY_DIR}     "
    echo "OUTPUT_DIR     = ${OUTPUT_DIR}     "
    echo "OUTPUT_AAB     = ${OUTPUT_AAB}     "
    echo "OUTPUT_APKS    = ${OUTPUT_APKS}    "
    echo "UNIVERSAL_APK  = ${UNIVERSAL_APK}  "
    echo "OUTPUT_MAPPING = ${OUTPUT_MAPPING} "
    echo "OUTPUT_METADATA= ${OUTPUT_METADATA}"
    echo "VERSION_GRADLE = ${VERSION_GRADLE} "
    echo "yyyymmddhhmmss = ${yyyymmddhhmmss} "

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

function find_folders_with_file() {
    local _root_dir="$1"
    local _module_root_file="$2"
    local _max_depth="${3:-3}"

    local folder_list
    folder_list=($(find "$_root_dir" -maxdepth "$_max_depth" -type f -name "$_module_root_file" -exec dirname {} \; | sort -u))
    echo "${folder_list[@]}"
}

function check_path_type() {
    local _path="$1"
    echo "$_path"

    if [ -d "$_path" ]; then
        return 1 # Folder
    elif [ -f "$_path" ]; then
        return 2 # File
    else
        return 0 # Invalid path
    fi
}

# 호출방법
#_param1=$(IFS=","; echo "${array1[*]}")
#_param2=$(IFS=","; echo "${array2[*]}")
#filter_list "$_param1" "$_param2"

function filter_list() {
    local _list
    local _filter_list
    local _result=()
    IFS="," read -ra _list <<<"$1"
    IFS="," read -ra _filter_list <<<"$2"

    for _element in "${_list[@]}"; do
        for _exclude_string in "${_filter_list[@]}"; do
            if [[ "$_element" == *"${_exclude_string}"* ]]; then
                _result+=("$_element")
                break
            fi
        done
    done

    echo "${_result[@]}"
}

function exitProcess() {
    echo "$1"
    echo "$2"
    read -n 1
    echo
    exit 1
}

#pause
function pause() {
    echo "Press any key to continue..."
    read -n1 -s
}

# 클립보드에서 읽은 파일 경로가 유효한지 확인
# 하나라도 유효하지 않으면 종료
function checkClipboardSourcePath() {
    # 클립보드에서 파일 경로 읽어오기 - 시작 ------------------------------------------------------------------
    source_pathfile=$(pbpaste)

    #다중파일분리
    source_pathfile_array=()
    while IFS= read -r line; do
        source_pathfile_array+=("$line")
    done <<<"$source_pathfile"

    #echo "$source_pathfile_array"
    #source_file_count=${#source_pathfile_array[@]}
    #echo "배열의 요소 개수: $source_file_count"

    for file in "${source_pathfile_array[@]}"; do
        source_pathfile="$file"
        #echo "$source_pathfile"

        #소스파일유효성체크
        if [ -d "$source_pathfile" ]; then
            echo "폴더:${source_pathfile#*${project_root}}"
        elif [ -f "$source_pathfile" ]; then
            echo "파일:${source_pathfile#*${project_root}}"
        else
#            echo "$source_pathfile" "유효한 경로가 아닙니다."
            exitProcess "$source_pathfile" "유효한 경로가 아닙니다."
        fi
    done
    # 클립보드에서 파일 경로 읽어오기 - 끝 ------------------------------------------------------------------
}

function select_target_module() {
    #target모듈 select start -----------------------------------------------------------------------------------------------
    modules_count=${#modules[@]}
    indexer=("1" "2" "3" "4" "5" "6" "7" "8" "9" "0" "a" "b" "c" "d" "e" "f" "g" "h" "i" "j" "k" "l" "m" "n" "o" "p" "q" "r" "s" "t" "u" "v" "w" "x" "y" "z" "A" "B" "C" "D" "E" "F" "G" "H" "I" "J" "K" "L" "M" "N" "O" "P" "Q" "R" "S" "T" "U" "V" "W" "X" "Y" "Z")
    indexer_count=${#indexer[@]}

    #echo "$modules_count"
    if [ $modules_count -lt 1 ]; then
        exitProcess "오류: 모듈 개수가 1보다 작습니다."
    elif [ $modules_count -eq 1 ]; then
        selected_folder="${modules[0]}"
    else
        if [ $modules_count -gt $indexer_count ]; then
            indexer=()
            for ((i=1; i<=$modules_count; i++)); do
                indexer+=($i)
            done
        fi

        for i in "${!modules[@]}"; do
            echo "${indexer[$i]}. ${modules[$i]#*${project_root}}"
        done

        echo "Enter the number of the target module: "
        if [ $modules_count -gt $indexer_count ]; then
            read input_letter
        else
            read -n 1 input_letter
        fi
        echo

        index=-1
        for i in "${!indexer[@]}"; do
            if [ "${indexer[$i]}" = "$input_letter" ]; then
                index=$i
                break
            fi
        done

        if [ $index -eq -1 ]; then
            exitProcess "오류: 유효하지 않은 선택입니다."
        fi

        if [ $index -ge ${#modules[@]} ]; then
            exitProcess "해당 위치에 폴더가 없습니다."
        fi
        selected_folder="${modules[$index]}"
    fi
#    exitProcess "select :: $selected_folder"
    target_module_root=$selected_folder

    echo "${target_module_root}" >"${this_folder}/module_mover_last_target.txt"
    #target모듈 select end -----------------------------------------------------------------------------------------------
}

function move_files() {
    # Print the array elements
    for file in "${source_pathfile_array[@]}"; do
        source_pathfile="$file"

        source_module_root=$(find_file_in_parent_dir "$source_pathfile" "$module_root_file")
        project_root=$(find_file_in_parent_dir "$source_pathfile" "$project_root_file")
        source_file="${source_pathfile#*${source_module_root}}"

        target_pathfile="$target_module_root$source_file"
        target_path=$(dirname "$target_pathfile")

        #echo "dump ============================="
        #echo $project_root
        #echo $source_pathfile
        #echo $source_module_root
        #echo $source_file
        #echo $target_module_root
        #echo $target_pathfile
        #echo "${source_module_root#*${project_root}} -> ${target_module_root#*${project_root}}"
        #echo "dump ============================="

        #소스파일유효성체크
        prefix=""
        if [ -d "$source_pathfile" ]; then
            prefix="폴더:"
        elif [ -f "$source_pathfile" ]; then
            if [ -f "$target_pathfile" ]; then
                prefix="*파일:"
            else
                prefix="파일:"
            fi
        else
            echo prefix="에러:"
        fi

        # 폴더가 있는 경우는 mv 명령으로 이동을 하지 못해 복사후 지우는 것으로 변경
        echo "$prefix ${source_module_root#*${project_root}} -> ${target_module_root#*${project_root}} : $source_file"
        mkdir -p "$target_path" && cp -R "$source_pathfile" "$target_path/" && rm -r "$source_pathfile"
    done

#    exitProcess "작업이 완료되었습니다."
}

function all_module() {
    modules=($(find_folders_with_file "$project_root" "$module_root_file"))
#    echo ${modules[*]}
    filter_strings=("$project_root/") #전체
    param1=$(IFS=","; echo "${modules[*]}")
    param2=$(IFS=","; echo "${filter_strings[*]}")
    filtered_array=$(filter_list "$param1" "$param2")
    modules=($filtered_array)
#    echo ${modules[*]}
}

# 폴더확인 없으면 종료
# checkSourcePath "/path/to/your/directory"
checkSourcePath() {
    local dir_path=$1

    if [ ! -d "$dir_path" ]; then
        exitProcess "error: '$dir_path'는 유효한 디렉토리가 아닙니다."
    fi
}

# 폴더명 변경
# rename_folders "/path/to/your/directory" "old_string" "new_string"
rename_folders() {
    local target_path=$1
    local old_string=$2
    local new_string=$3

    if [[ -z $target_path || -z $old_string || -z $new_string ]]; then
        exitProcess "모든 매개변수를 제공해야 합니다: 대상 경로, 이전 문자열, 새 문자열(2)"
    fi

    find "$target_path" -type d -name "*$old_string*" | while read -r old_dir; do
        local new_dir=${old_dir//$old_string/$new_string}
#        echo  "$old_dir" "->" "$new_dir"
        mkdir -p "$(dirname "$new_dir")" && mv "$old_dir" "$new_dir"
    done
}

# 파일명 변경
# rename_files "/path/to/your/directory" "old_string" "new_string"
rename_files() {
    local target_path=$1
    local old_string=$2
    local new_string=$3

    if [[ -z $target_path || -z $old_string || -z $new_string ]]; then
        exitProcess "모든 매개변수를 제공해야 합니다: 대상 경로, 이전 문자열, 새 문자열(2)"
    fi

    find "$target_path" -type f -name "*$old_string*" | while read -r old_file; do
        local new_file=${old_file//$old_string/$new_string}
#        echo  "$old_file" "->" "$new_file"
        mkdir -p "$(dirname "$new_file")" && mv "$old_file" "$new_file"
    done
}

# 파일내부 문자열변경
# replace_in_files "/path/to/your/directory" "com.example.template" "com.example.newmodule"
replace_in_files() {
    local target_path=$1
    local old_string=$2
    local new_string=$3

    if [[ -z $target_path || -z $old_string || -z $new_string ]]; then
        echo "모든 매개변수를 제공해야 합니다: 대상 경로, 변경할 문자열, 새 문자열(1)"
    fi

    find "$target_path" -type f -exec sed -i '' "s/$old_string/$new_string/g" {} +
}

# target 모듈이 존제하는지 확인 있으면 종료
# checkModuleExistence "/path/to/target" "module_root_file"
checkModule() {
    local target_path=$1
    local module_root_file=$2

    local modules=($(find_folders_with_file "$target_path" "$module_root_file"))

    if (( ${#modules[@]} > 0 )); then
        exitProcess "error: $target_path 내에 이미 존재하는 모듈이 있습니다."
    fi
}

# target 폴더가 있을 경우 Overwrite할지 확인 N 하면 종료
# confirmOverwrite "/path/to/target"
confirmOverwrite() {
    local target_path=$1

    if [ -d "$target_path" ]; then
        echo "해당 경로에 폴더가 존재합니다. 덮어쓸까요? (Y/n)"
        read -n 1 -p "?: " input
        echo
        if [[ "$input" == "Y" || "$input" == "y" || "$input" == "" ]]; then
            echo "$target_path 폴더에 덮어씁니다."
        else
            exitProcess "작업을 종료합니다."
        fi
    fi
}

module_root_file="build.gradle.kts"
project_root_file="gradlew"
this_folder="$(cd "$(dirname "$0")" && pwd -P)"
project_root=$(find_file_in_parent_dir "$this_folder" "$project_root_file")

echo "-----------------------------------------------------------------------------"
echo "this_folder       = $this_folder"
echo "project_root      = $project_root"
echo "-----------------------------------------------------------------------------"
