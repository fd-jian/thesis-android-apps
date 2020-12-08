#!/bin/sh

DOC_DIR=/opt/googleplay/doc
cp "$1" /tmp/classes.jar &&
mkdir -p "$DOC_DIR" &&
mkdir -p /tmp/googleplay &&
unzip -od /tmp/googleplay /tmp/classes.jar > /dev/null &&
(
  cd /tmp/googleplay &&
  find . -type d -exec sh -c \
    '[ -n "$(find "$1"/. ! -name . -prune -type f -name "*.class")" ]' _ {} \; \
    -print |
  sed 's|^./||g;s|/|.|g'
) > "$DOC_DIR"/package-list
