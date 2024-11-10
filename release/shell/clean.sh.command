#!/bin/bash
clear
source "$(cd "$(dirname "$0")" && pwd -P)"/common_function.sh
find "$project_root" -type d -name "build" -prune -print -exec rm -rf "{}" \;
find "$project_root" -type d -name ".gradle" -prune -print -exec rm -rf "{}" \;
