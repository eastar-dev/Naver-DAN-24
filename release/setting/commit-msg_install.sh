#!/bin/bash

#check used git
GIT_FOLDER=../../.git
if [ -d "${GIT_FOLDER}" ]; then
    echo "git location : ${GIT_FOLDER}"
else
    echo "has not git folder"
    exit 1
fi

#hook folder create
HOOKS_FOLDER="$GIT_FOLDER/hooks"
if [ -d "${HOOKS_FOLDER}" ]; then
    echo "git hooks location : ${HOOKS_FOLDER}"
else
    echo "create git hooks folder : ${HOOKS_FOLDER}"
    mkdir "${HOOKS_FOLDER}"
fi

#backup
HOOK_FILE="${HOOKS_FOLDER}/commit-msg"
if [ -f "${HOOK_FILE}" ]; then
    TIME_SEED=$(date +%s)
    mv $HOOK_FILE "$HOOK_FILE.old.$TIME_SEED"
    echo "$HOOK_FILE $HOOK_FILE.old.$TIME_SEED"
fi

cp commit-msg $HOOK_FILE

chmod +x $HOOK_FILE
