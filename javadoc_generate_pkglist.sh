#!/bin/sh

SDK_VERSION=$1
ANDROID_HOME=${2:-${ANDROID_HOME:-/opt/android-sdk}}

DOC_DIR=$ANDROID_HOME/doc/android-$SDK_VERSION

mkdir -p "$DOC_DIR" &&
(
  cd "$ANDROID_HOME"/sources/android-"$SDK_VERSION" &&
  find . -type d -exec sh -c \
    '[ -n "$(find "$1"/. ! -name . -prune -type f -name "*.java")" ]' \_ {} \; \
    -print |
  sed 's|^./||g;s|/|.|g'
) > "$DOC_DIR"/package-list
