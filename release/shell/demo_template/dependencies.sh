#!/bin/bash
clear

# 함수 정의
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

# 변수 설정
module_root_file="build.gradle.kts"
project_root_file="gradlew"
this_folder="$(cd "$(dirname "$0")" && pwd -P)"
project_root=$(find_file_in_parent_dir "$this_folder" "$project_root_file")

# project_root로부터 this_folder까지의 상대 경로를 구하여 Gradle 프로젝트 경로 형식으로 변환
relative_path=${this_folder#$project_root/} # 프로젝트 루트를 제거하여 상대 경로를 얻음
module_path=":${relative_path//\//:}" # '/'를 ':'로 변경하여 Gradle 프로젝트 경로 형식으로 변환

# 결과 출력
#echo "this_folder       = $this_folder"
#echo "project_root      = $project_root"
#echo "Gradle project path = $module_path"

# Gradle 명령 실행
sh $project_root/gradlew $module_path:dependencies --configuration stageDebugRuntimeClasspath | grep "project :"
