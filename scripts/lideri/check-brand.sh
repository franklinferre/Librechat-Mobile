#!/usr/bin/env bash
# Static brand check for the Lideri Chat white-label: reads source files only, needs no build.
# Exits 0 and prints "marca OK" when every item holds; otherwise exits 1 listing each failure.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

BRAND_KT="core/common/src/commonMain/kotlin/com/garfiec/librechat/core/common/brand/Brand.kt"
COMMON_MODULE_KT="core/common/src/commonMain/kotlin/com/garfiec/librechat/core/common/di/CommonModule.kt"
APP_GRADLE="app/build.gradle.kts"
MANIFEST="app/src/main/AndroidManifest.xml"
PBXPROJ="iosApp/iosApp.xcodeproj/project.pbxproj"
PLIST="iosApp/iosApp/Info.plist"

failures=()
fail() { failures+=("$1"); }

# Succeeds when the file exists and contains the fixed string.
has() { [[ -f "$1" ]] && grep -qF -- "$2" "$1"; }

# Number of lines in $1 matching the extended regex $2 (0 when the file is missing).
count() { if [[ -f "$1" ]]; then grep -cE -- "$2" "$1" || true; else echo 0; fi; }

# Android: the play flavor owns the bare applicationId.
has "$APP_GRADLE" 'applicationId = "cloud.lideri.chat"' \
  || fail "Android applicationId is not cloud.lideri.chat ($APP_GRADLE)"

# iOS: bundle id and channel in all three build configurations.
n=$(count "$PBXPROJ" '^[[:space:]]*PRODUCT_BUNDLE_IDENTIFIER = cloud\.lideri\.chat\.ios;$')
[[ "$n" -eq 3 ]] || fail "iOS bundle id is cloud.lideri.chat.ios in $n of 3 configurations ($PBXPROJ)"
total=$(count "$PBXPROJ" '^[[:space:]]*PRODUCT_BUNDLE_IDENTIFIER = ')
[[ "$total" -eq 3 ]] || fail "iOS has $total PRODUCT_BUNDLE_IDENTIFIER entries, expected 3 ($PBXPROJ)"
n=$(count "$PBXPROJ" '^[[:space:]]*SWITCHBOARD_CHANNEL = appstore;$')
[[ "$n" -eq 3 ]] || fail "iOS channel is appstore in $n of 3 configurations ($PBXPROJ)"
total=$(count "$PBXPROJ" '^[[:space:]]*SWITCHBOARD_CHANNEL = ')
[[ "$total" -eq 3 ]] || fail "iOS has $total SWITCHBOARD_CHANNEL entries, expected 3 ($PBXPROJ)"

# iOS display name: the <string> right after the CFBundleDisplayName key.
if [[ -f "$PLIST" ]] && grep -A1 '<key>CFBundleDisplayName</key>' "$PLIST" | grep -qF '<string>Lideri Chat</string>'; then :; else
  fail "CFBundleDisplayName is not Lideri Chat ($PLIST)"
fi

# Locked server and deep-link scheme in Brand.
has "$BRAND_KT" 'const val LOCKED_SERVER_URL = "https://chat.lideri.cloud"' \
  || fail "Brand.LOCKED_SERVER_URL is not https://chat.lideri.cloud ($BRAND_KT)"
has "$BRAND_KT" 'const val DEEP_LINK_SCHEME = "liderichat"' \
  || fail "Brand.DEEP_LINK_SCHEME is not liderichat ($BRAND_KT)"

# The shipped Koin graph builds the lock from Brand (a ServerLock(null) here would ship the upstream picker).
has "$COMMON_MODULE_KT" 'ServerLock(Brand.LOCKED_SERVER_URL)' \
  || fail "CommonModule no longer registers ServerLock(Brand.LOCKED_SERVER_URL) ($COMMON_MODULE_KT)"

# Deep-link scheme in the Android manifest and the iOS Info.plist.
has "$MANIFEST" 'android:scheme="liderichat"' \
  || fail "AndroidManifest has no liderichat scheme ($MANIFEST)"
if [[ -f "$PLIST" ]] && grep -A1 '<key>CFBundleURLSchemes</key>' "$PLIST" | grep -v 'CFBundleURLSchemes' | grep -qF '<array>' \
   && grep -qF '<string>liderichat</string>' "$PLIST"; then :; else
  fail "Info.plist has no liderichat URL scheme ($PLIST)"
fi

# No "Switchboard" in user-facing strings (localized string tables and Android resources).
leaks=$(find . \( -path ./upstream -o -path ./.git -o -path ./.superpowers -o -name build -o -name .gradle \) -prune -o \
  -type f \( -name 'strings.xml' -o -name '*.strings' -o -name '*.xcstrings' \) -print0 \
  | xargs -0 grep -il 'switchboard' 2>/dev/null || true)
if [[ -n "$leaks" ]]; then
  while IFS= read -r f; do fail "\"Switchboard\" appears in user-facing strings ($f)"; done <<< "$leaks"
fi

if [[ ${#failures[@]} -gt 0 ]]; then
  echo "marca FALHOU:" >&2
  for f in "${failures[@]}"; do echo "  - $f" >&2; done
  exit 1
fi
echo "marca OK"
