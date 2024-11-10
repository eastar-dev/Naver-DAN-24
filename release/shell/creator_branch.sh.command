#!/bin/bash

clear
source "$(cd "$(dirname "$0")" && pwd -P)"/common_function.sh
cd $project_root

# Python 스크립트 실행
branchName=$(python3 "$project_root/release/shell/common_function.py" "creator_branch_name")
echo "$branchName"
#pause

# 새로운 브랜치 생성
git checkout -b "$branchName"
sleep 5
