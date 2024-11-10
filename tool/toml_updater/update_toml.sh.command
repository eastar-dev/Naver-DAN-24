#!/bin/bash
clear
source "$(cd "$(dirname "$0")" && pwd -P)"/common_function.sh

NEW_BRANCH=feature/gradle_update

cd $project_root
./gradlew :tool:toml_updater:updateToml
git branch -D $NEW_BRANCH
git push origin --delete $NEW_BRANCH
git checkout -b $NEW_BRANCH
git config --global user.email "[이메일주소입력]"
git config --global user.name "♍️[이름입력]"
git add -A
git commit -m "♍️Updated TOML files - $(date -u +%Y%m%d%H%M%S)" || exit 0  # Exit gracefully if no changes
git push --set-upstream origin $NEW_BRANCH
exit 0
