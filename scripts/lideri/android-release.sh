#!/usr/bin/env bash
# Build the Play-channel release APK (cloud.lideri.chat) signed with the Lideri release key.
# The keystore lives outside the repo ($LIDERI_KEYSTORE, default ~/.lideri/lideri-release.jks) and
# its password only in the macOS Keychain (service lideri-android-release, account lideri); both
# reach Gradle as the SIGNING_* environment variables, so nothing secret is written to disk.
# Without them the build would silently fall back to the public debug key, so this refuses instead.
# Usage: scripts/lideri/android-release.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
KEYSTORE="${LIDERI_KEYSTORE:-$HOME/.lideri/lideri-release.jks}"
APK="$ROOT/app/build/outputs/apk/play/release/app-play-release.apk"
EXPECTED_DN="CN=LID.IA, O=Lideri, C=BR"

if [[ ! -f "$KEYSTORE" ]]; then
  echo "error: release keystore not found at $KEYSTORE (set LIDERI_KEYSTORE)." >&2
  exit 1
fi
if ! SIGNING_STORE_PASSWORD="$(security find-generic-password -s lideri-android-release -a lideri -w 2>/dev/null)"; then
  echo "error: no Keychain entry lideri-android-release/lideri holding the keystore password." >&2
  exit 1
fi

export SIGNING_STORE_FILE="$KEYSTORE"
export SIGNING_STORE_PASSWORD
export SIGNING_KEY_ALIAS="lideri"
export SIGNING_KEY_PASSWORD="$SIGNING_STORE_PASSWORD"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"

cd "$ROOT"
# playRelease only exists on request (app/build.gradle.kts, buildPlay).
./gradlew :app:assemblePlayRelease -PbuildPlay

BUILD_TOOLS="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
SIGNER="$("$BUILD_TOOLS/apksigner" verify --print-certs "$APK" | sed -n 's/^.*Signer.* certificate DN: //p' | sort -u)"
if [[ "$SIGNER" != "$EXPECTED_DN" ]]; then
  echo "error: $APK is signed by '$SIGNER', not the Lideri release key." >&2
  exit 1
fi
echo "Signed with the Lideri release key: $APK"
