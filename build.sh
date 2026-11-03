#!/usr/bin/env bash
# Build wakeswipe APK without Gradle: aapt2 + javac + d8 + zipalign + apksigner.
# Requires JDK 17 and an Android SDK (any installed platform + a build-tools dir).
# The runner's preinstalled platforms vary (android-33 is not always present),
# so the script uses android-33 when present, otherwise the highest installed
# platform, and installs android-33 via sdkmanager only as a last resort.
# Output: out/wakeswipe-debug.apk (debug-signed, for sideloading only).
set -euo pipefail
cd "$(dirname "$0")"

SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$SDK" ]; then
  echo "ANDROID_SDK_ROOT / ANDROID_HOME is not set" >&2
  exit 1
fi
BT="$(ls -d "$SDK"/build-tools/*/ | sort -V | tail -n 1)"
AAPT2="$BT/aapt2"
D8="$BT/d8"
ZIPALIGN="$BT/zipalign"
APKSIGNER="$BT/apksigner"
ANDROID_JAR=""
if [ -f "$SDK/platforms/android-33/android.jar" ]; then
  ANDROID_JAR="$SDK/platforms/android-33/android.jar"
else
  # Highest installed platform, e.g. android-34 / android-35 on GitHub runners.
  ANDROID_JAR="$(ls "$SDK"/platforms/android-*/android.jar 2>/dev/null | sort -V | tail -n 1 || true)"
fi
if [ -z "$ANDROID_JAR" ] || [ ! -f "$ANDROID_JAR" ]; then
  echo "No platform found, installing android-33 via sdkmanager..." >&2
  SDKMGR="$(ls "$SDK"/cmdline-tools/latest/bin/sdkmanager 2>/dev/null || ls "$SDK"/cmdline-tools/*/bin/sdkmanager 2>/dev/null | head -n 1 || true)"
  if [ -n "$SDKMGR" ]; then
    yes | "$SDKMGR" --licenses >/dev/null 2>&1 || true
    "$SDKMGR" "platforms;android-33"
    ANDROID_JAR="$SDK/platforms/android-33/android.jar"
  fi
fi
[ -x "$AAPT2" ] || { echo "aapt2 not found in $BT" >&2; exit 1; }
[ -f "$ANDROID_JAR" ] || { echo "android.jar not found under $SDK/platforms (tried android-33 and highest installed)" >&2; ls "$SDK/platforms" >&2 || true; exit 1; }
echo "Using $ANDROID_JAR"

rm -rf out classes dex
mkdir -p out classes dex

"$AAPT2" compile --dir res -o out/compiled.zip
"$AAPT2" link -o out/app-unsigned.apk \
  -I "$ANDROID_JAR" \
  --manifest AndroidManifest.xml \
  --auto-add-overlay \
  out/compiled.zip

javac -source 8 -target 8 -cp "$ANDROID_JAR" -d classes \
  $(find src -name '*.java')

"$D8" --lib "$ANDROID_JAR" --output dex classes
cp out/app-unsigned.apk out/app.apk
(cd dex && zip -q ../out/app.apk classes.dex)

"$ZIPALIGN" -f 4 out/app.apk out/app-aligned.apk

if [ ! -f debug.keystore ]; then
  keytool -genkey -v -keystore debug.keystore -alias androiddebugkey \
    -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
fi
"$APKSIGNER" sign --ks debug.keystore --ks-pass pass:android \
  --key-pass pass:android --out out/wakeswipe-debug.apk out/app-aligned.apk
"$APKSIGNER" verify out/wakeswipe-debug.apk
ls -lh out/wakeswipe-debug.apk
