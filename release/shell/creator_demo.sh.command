#!/bin/bash

clear

source "$(cd "$(dirname "$0")" && pwd -P)"/common_function.sh

#모듈입력받기
# _debug 변수 설정 (개발용: 1, 운영용: 0)
_debug=0  # 1 for debug mode, 0 for production mode
if [ "$_debug" -eq 1 ]; then
    # 디버그 모드일 때
    # 모듈자동입력
    module_pathname="demo/test_demo"
    # 기존모듈제거
    rm -r "$project_root/$module_pathname"
else
    # 운영 모드일 때
    echo "Enter the name for demo module ex : test"
    read -p "?:" module_pathname
    module_pathname="demo/${module_pathname}_demo"
    echo
fi


source_path="$this_folder/demo_template"
target_path="$project_root/$module_pathname"
#echo "s:$source_path"
#echo "t:$target_path"

#source 유효성 체크 -------------------------------------------------------------
checkSourcePath "$source_path"
#target module 유효성 체크 -------------------------------------------------------------
checkModule "$target_path" "$module_root_file"
#target Overwrite 체크 -------------------------------------------------------------
confirmOverwrite "$target_path"

#template > temp > target --------------------------------------------
source_temp="${source_path}_temp"
source_text="__package__"
target_text=$(basename "$target_path" /)
SourceText="__Package__"
TargetText=$(PascalCase "${target_text}")
#echo "s:$source_path"
#echo "t:$target_path"
#echo "s:$source_text"
#echo "t:$target_text"
#echo "s:$SourceText"
#echo "t:$TargetText"

# 혹시 모르는 temp 제거
rm -r "$source_temp" > /dev/null 2>&1
# temp에 복사
cp -r "$source_path/." "$source_temp"
# 폴더이름변경
rename_folders "$source_temp" "$source_text" "$target_text"
# 파일이름변경
rename_files "$source_temp" "$SourceText" "$TargetText"
# 파일내용변경
replace_in_files "$source_temp" "$source_text" "$target_text"
# 파일내용변경
replace_in_files "$source_temp" "$SourceText" "$TargetText"
# target에 복사
cp -r "$source_temp/." "$target_path"
# temp 제거
rm -r "$source_temp"
#exitProcess "모듈생성이 완료되었습니다." "프로젝트를 열어서 모듈을 추가해주세요."
exit 0
