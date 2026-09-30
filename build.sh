#!/usr/bin/env bash
# Build Mint PWA without Gradle, using Debian/Ubuntu-packaged Android tools.
#
#   ./build.sh --debug      throwaway debug key (build/debug.jks), for local testing
#   ./build.sh              release: needs MINTPWA_KEYSTORE, MINTPWA_KEY_ALIAS and
#                           MINTPWA_KS_PASS (or MINTPWA_KS_PASS_FILE)
#
# Requires: sudo apt install openjdk-21-jdk-headless dalvik-exchange aapt zipalign apksigner \
#                            android-sdk-platform-23
#
# Why API 23's android.jar: aapt only uses it to resolve framework resource/attribute IDs, which are
# stable across releases. Java sources compile against build-support/stubs (see its README).
# SPDX-License-Identifier: GPL-3.0-or-later
set -euo pipefail
cd "$(dirname "$0")"

ANDROID_JAR=${ANDROID_JAR:-/usr/lib/android-sdk/platforms/android-23/android.jar}
DEX=${DEX:-dalvik-exchange}
for t in javac "$DEX" aapt zipalign apksigner keytool; do
  command -v "$t" >/dev/null || { echo "missing tool: $t (see header of $0)"; exit 1; }
done
[ -f "$ANDROID_JAR" ] || { echo "missing $ANDROID_JAR (apt install android-sdk-platform-23, or set ANDROID_JAR)"; exit 1; }

MODE=release; [ "${1:-}" = "--debug" ] && MODE=debug
VERSION=$(sed -n 's/.*android:versionName="\([^"]*\)".*/\1/p' AndroidManifest.xml)
B=build; rm -rf "$B/obj" && mkdir -p "$B/obj/stubs" "$B/obj/classes"

echo "==> compiling stubs + sources"
javac --release 8 -nowarn -Xlint:none -d "$B/obj/stubs" $(find build-support/stubs -name '*.java')
javac --release 8 -Xlint:none -cp "$B/obj/stubs" -d "$B/obj/classes" $(find src -name '*.java')

echo "==> dexing (app classes only; stubs are never packaged)"
"$DEX" --dex --min-sdk-version=26 --output="$B/obj/classes.dex" "$B/obj/classes"

echo "==> packaging"
aapt package -f -M AndroidManifest.xml -S res -I "$ANDROID_JAR" -F "$B/obj/unaligned.apk"
(cd "$B/obj" && aapt add unaligned.apk classes.dex >/dev/null)
zipalign -f -p 4 "$B/obj/unaligned.apk" "$B/obj/aligned.apk"

echo "==> signing ($MODE)"
OUT="$B/mintpwa-$VERSION.apk"; [ "$MODE" = debug ] && OUT="$B/mintpwa-$VERSION-debug.apk"
if [ "$MODE" = debug ]; then
  KS="$B/debug.jks"
  [ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass android -keypass android -alias debug \
      -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Mint PWA debug" >/dev/null 2>&1
  apksigner sign --v4-signing-enabled false --ks "$KS" --ks-pass pass:android --ks-key-alias debug \
      --out "$OUT" "$B/obj/aligned.apk"
else
  : "${MINTPWA_KEYSTORE:?set MINTPWA_KEYSTORE (or use --debug)}" "${MINTPWA_KEY_ALIAS:?set MINTPWA_KEY_ALIAS}"
  if [ -n "${MINTPWA_KS_PASS_FILE:-}" ]; then PASS="file:$MINTPWA_KS_PASS_FILE"
  else PASS="pass:${MINTPWA_KS_PASS:?set MINTPWA_KS_PASS or MINTPWA_KS_PASS_FILE}"; fi
  apksigner sign --v4-signing-enabled false --ks "$MINTPWA_KEYSTORE" --ks-key-alias "$MINTPWA_KEY_ALIAS" \
      --ks-pass "$PASS" --out "$OUT" "$B/obj/aligned.apk"
fi
apksigner verify "$OUT"
echo "==> $OUT"
apksigner verify --print-certs "$OUT" | grep -E 'SHA-256' || true
