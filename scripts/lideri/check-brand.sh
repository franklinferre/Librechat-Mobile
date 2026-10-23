#!/usr/bin/env bash
# Static brand check for the LID.IA white-label: reads source files only, needs no build.
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
if [[ -f "$PLIST" ]] && grep -A1 '<key>CFBundleDisplayName</key>' "$PLIST" | grep -qF '<string>LID.IA</string>'; then :; else
  fail "CFBundleDisplayName is not LID.IA ($PLIST)"
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

# ---------------------------------------------------------------------------------------------
# Visual identity (plan 2): hashes of the shipped logo/fonts/icon master, palette, and the pickers.
# ---------------------------------------------------------------------------------------------
APPROVED_ICON_SHA256="d89aa4c63b802da6700706ab69b95069456f805ec48a4b54154dd66e790af8e3"
APPROVED_ANDROID_FOREGROUND_SHA256="56807b49294d33c1704d2cba03e1452d83a00f7b2afacce8421076269fb29b50"
LOGO_PRINCIPAL_SHA256="c3e7416c7147043e94fc06def8fc42ee1878777eb3efe4a6afad2dd26d5ea7e1"
LOGO_NEGATIVA_SHA256="7f8bdd370d5b0efdb99db52adaddefb985a4425dbb8100dd2c3f121893531138"
KIT_DIR="design/lideri/kit"
KIT_LOCK="$KIT_DIR/integridade-sha256.json"
RES_DIR="core/ui/src/commonMain/composeResources"
THEME_DIR="core/ui/src/commonMain/kotlin/com/garfiec/librechat/core/ui/theme"
APPEARANCE_KT="feature/settings/src/commonMain/kotlin/com/garfiec/librechat/feature/settings/screen/AppearanceSettingsScreen.kt"
ICON_BG="app/src/main/res/values/ic_launcher_background.xml"
ICON_BG_DEBUG="app/src/debug/res/values/ic_launcher_background.xml"
IOS_ICON="iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/Icon-iOS-Default-1024@1x.png"
ANDROID_FG="app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png"

# sha256 of a file, empty when it is missing.
sha() { if [[ -f "$1" ]]; then shasum -a 256 "$1" | cut -d' ' -f1; fi; }

# sha256 the kit lock records for a kit-relative path, empty when absent.
kit_sha() {
  [[ -f "$KIT_LOCK" ]] || return 0
  python3 -I -c '
import json, sys
for e in json.load(open(sys.argv[1])):
    if e.get("path") == sys.argv[2]:
        print(e.get("sha256", ""))
' "$KIT_LOCK" "$1"
}

# 1. Logos: hashes from the global constraints and from the kit lock.
check_logo() { # <resource file> <kit path> <expected sha>
  local actual lock
  actual=$(sha "$1")
  lock=$(kit_sha "$2")
  [[ -n "$actual" ]] || { fail "logo missing ($1)"; return; }
  [[ "$actual" == "$3" ]] || fail "logo changed: sha256 differs from the approved kit logo ($1)"
  [[ -n "$lock" && "$actual" == "$lock" ]] || fail "logo does not match $KIT_LOCK entry $2 ($1)"
}
check_logo "$RES_DIR/drawable/lideri_principal.png" "logos/lideri-principal.png" "$LOGO_PRINCIPAL_SHA256"
check_logo "$RES_DIR/drawable/lideri_negativa.png" "logos/lideri-negativa.png" "$LOGO_NEGATIVA_SHA256"

# 2. Fonts: each shipped .ttf is byte-identical to its kit file.
check_font() { # <resource name> <kit file>
  local actual kit lock
  actual=$(sha "$RES_DIR/font/$1.ttf")
  kit=$(sha "$KIT_DIR/fonts/$2.ttf")
  lock=$(kit_sha "fonts/$2.ttf")
  [[ -n "$actual" ]] || { fail "font missing ($RES_DIR/font/$1.ttf)"; return; }
  [[ -n "$kit" && "$actual" == "$kit" ]] || fail "font differs from the kit file $KIT_DIR/fonts/$2.ttf ($RES_DIR/font/$1.ttf)"
  [[ -n "$lock" && "$actual" == "$lock" ]] || fail "font does not match $KIT_LOCK entry fonts/$2.ttf ($RES_DIR/font/$1.ttf)"
}
check_font poppins_semibold Poppins-SemiBold
check_font poppins_bold Poppins-Bold
check_font raleway_regular Raleway-Regular
check_font raleway_medium Raleway-Medium
check_font raleway_semibold Raleway-SemiBold
check_font raleway_bold Raleway-Bold

# Every .ttf in the font folder must be a known kit file: an unmapped one fails.
known_fonts=" poppins_semibold poppins_bold raleway_regular raleway_medium raleway_semibold raleway_bold "
for f in "$RES_DIR"/font/*.ttf; do
  [[ -e "$f" ]] || continue
  name=$(basename "$f" .ttf)
  [[ "$known_fonts" == *" $name "* ]] || fail "font not in the kit map: $f"
done

# Font licences shipped next to the app match the kit's.
for l in Poppins Raleway; do
  [[ -n "$(sha "$RES_DIR/files/licenses/$l-OFL.txt")" && "$(sha "$RES_DIR/files/licenses/$l-OFL.txt")" == "$(sha "$KIT_DIR/fonts/$l-OFL.txt")" ]] \
    || fail "licence differs from the kit file $KIT_DIR/fonts/$l-OFL.txt ($RES_DIR/files/licenses/$l-OFL.txt)"
done

# 3. Icon master: the approved one, nothing else.
[[ "$(sha design/lideri/icon-master-1024.png)" == "$APPROVED_ICON_SHA256" ]] \
  || fail "icon master is not the approved one: sha256 differs (design/lideri/icon-master-1024.png)"

# Shipped icons: the iOS 1024 slot and the Android adaptive foreground are the generated, approved ones.
[[ "$(sha "$IOS_ICON")" == "$APPROVED_ICON_SHA256" ]] \
  || fail "iOS app icon is not the approved master: sha256 differs ($IOS_ICON)"
[[ "$(sha "$ANDROID_FG")" == "$APPROVED_ANDROID_FOREGROUND_SHA256" ]] \
  || fail "Android launcher foreground is not the approved one: sha256 differs ($ANDROID_FG)"

# 4. Palette wiring.
has "$THEME_DIR/Theme.kt" 'lideriColorScheme(darkTheme)' \
  || fail "theme does not use lideriColorScheme(darkTheme) ($THEME_DIR/Theme.kt)"
for c in 0xFF0424D9 0xFF0797EF; do
  has "$THEME_DIR/LideriColors.kt" "$c" || fail "brand colour $c missing ($THEME_DIR/LideriColors.kt)"
done

if has "$THEME_DIR/Theme.kt" 'rememberDynamicColorScheme'; then
  fail "theme references rememberDynamicColorScheme: wallpaper colour must stay out ($THEME_DIR/Theme.kt)"
fi
has "$THEME_DIR/Theme.kt" 'libreChatTypography()' || fail "theme does not call libreChatTypography() ($THEME_DIR/Theme.kt)"
for r in poppins_ raleway_; do
  has "$THEME_DIR/Type.kt" "$r" || fail "typography does not reference the $r fonts ($THEME_DIR/Type.kt)"
done

# Logo is on the screens that show the brand.
for scr in feature/auth/src/commonMain/kotlin/com/garfiec/librechat/feature/auth/screen/LoginScreen.kt \
           feature/auth/src/commonMain/kotlin/com/garfiec/librechat/feature/auth/screen/ServerUrlScreen.kt \
           feature/settings/src/commonMain/kotlin/com/garfiec/librechat/feature/settings/screen/GeneralSettingsScreen.kt; do
  has "$scr" 'LideriLogo(' || fail "LideriLogo( missing ($scr)"
done

# Launch screens: iOS plist and Android splash.
for k in LaunchBackground LaunchLogo; do
  has "$PLIST" "$k" || fail "UILaunchScreen has no $k ($PLIST)"
done
for t in app/src/main/res/values/themes.xml app/src/main/res/values-night/themes.xml; do
  has "$t" 'name="splash_background">#0424D9<' || fail "splash_background is not #0424D9 ($t)"
done

# 5. The accent and dynamic-colour pickers stay gone.
for k in accent_color_row dynamic_color_toggle; do
  if has "$APPEARANCE_KT" "$k"; then fail "removed picker '$k' is back ($APPEARANCE_KT)"; fi
done
[[ -f "$APPEARANCE_KT" ]] || fail "settings screen missing ($APPEARANCE_KT)"

# 6. Launcher background is the brand blue.
for bg in "$ICON_BG" "$ICON_BG_DEBUG"; do
  has "$bg" '>#0424D9<' || fail "launcher background is not exactly #0424D9 ($bg)"
done

if [[ ${#failures[@]} -gt 0 ]]; then
  echo "marca FALHOU:" >&2
  for f in "${failures[@]}"; do echo "  - $f" >&2; done
  exit 1
fi
echo "marca OK"
