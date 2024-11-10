#!/bin/bash

clear
source "$(cd "$(dirname "$0")" && pwd -P)"/common_function.sh

checkClipboardSourcePath

template_path="${this_folder}/architecture_template/"

#rsync -av --progress --recursive --include '*/' --exclude '*' "${template_path}" "${source_pathfile}"
rsync -av --progress --recursive --include '*/' "${template_path}" "${source_pathfile}"
