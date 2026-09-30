#!/usr/bin/env bash
# Run after ./build.sh: checks every platform API the app calls against a real android.jar.
# SPDX-License-Identifier: GPL-3.0-or-later
set -euo pipefail
cd "$(dirname "$0")/.."
ANDROID_JAR=${ANDROID_JAR:-/usr/lib/android-sdk/platforms/android-23/android.jar}
[ -d build/obj/classes ] || { echo "run ./build.sh first"; exit 1; }
mkdir -p build/check
javap -c -p $(find build/obj/classes -name '*.class') \
  | grep -oE '(Interface)?Method android/[A-Za-z0-9/$]+\.("<init>"|[a-zA-Z_]+):[^ ]+|Field android/[A-Za-z0-9/$]+\.[a-zA-Z_]+:[^ ]+' \
  | sed -E 's/^(Interface)?Method //; s/^Field /F:/' | sort -u > build/check/refs.txt
javac -d build/check tools/check-api/CheckApi.java
java -cp build/check CheckApi "$ANDROID_JAR" build/check/refs.txt tools/check-api/newer-members.txt
