#!/bin/bash
clear
source "$(cd "$(dirname "$0")" && pwd -P)"/common_function.sh

cd $project_root

while true; do
    # gradlew 명령어 실행 및 성공/실패에 따라 다른 소리 재생
    if sh ./gradlew clean :app:compileRealReleaseKotlin :app:compileStageDebugKotlin ; then
        # 성공 사운드 재생
        afplay /System/Library/Sounds/Blow.aiff

        echo "success..."
        sleep 2  # 잠시 대기
        break  # 성공하면 루프 종료
    else
        # 실패 사운드 재생
        afplay /System/Library/Sounds/Funk.aiff

        #pause
        echo "Press any key to Retrying..."
        read -n1 -s
    fi
done
