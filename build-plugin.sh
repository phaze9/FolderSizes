#!/bin/sh

set -u

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
LOCAL_GRADLE_HOME="$PROJECT_DIR/.gradle-user"

cleanup() {
    exit_status=$?
    trap - EXIT HUP INT TERM
    rm -rf -- "$LOCAL_GRADLE_HOME"
    exit "$exit_status"
}

trap cleanup EXIT HUP INT TERM

GRADLE_USER_HOME="$LOCAL_GRADLE_HOME" \
    "$PROJECT_DIR/gradlew" --no-daemon buildPlugin "$@"
