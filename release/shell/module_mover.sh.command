#!/bin/bash

clear
source "$(cd "$(dirname "$0")" && pwd -P)"/common_function.sh

checkClipboardSourcePath

all_module
#echo "modules= ${modules[*]}"

select_target_module

move_files
