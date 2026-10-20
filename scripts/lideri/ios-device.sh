#!/usr/bin/env bash
# Build the Debug app for a physical iPhone (free Personal Team signing) and install it.
# Team ID comes from scripts/lideri/.ios-team (gitignored) or $LIDERI_IOS_TEAM.
# Usage: scripts/lideri/ios-device.sh [--udid <id>]
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TEAM_FILE="$ROOT/scripts/lideri/.ios-team"

UDID=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --udid) UDID="${2:-}"; shift 2 ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
done

TEAM="${LIDERI_IOS_TEAM:-}"
if [[ -z "$TEAM" && -f "$TEAM_FILE" ]]; then
  TEAM="$(tr -d '[:space:]' < "$TEAM_FILE")"
fi
if [[ -z "$TEAM" ]]; then
  echo "error: no Apple Team ID. Put it in scripts/lideri/.ios-team (gitignored) or export LIDERI_IOS_TEAM." >&2
  exit 1
fi

if [[ -z "$UDID" ]]; then
  JSON="$(mktemp)"
  trap 'rm -f "$JSON"' EXIT
  xcrun devicectl list devices --json-output "$JSON" >/dev/null
  DEVICES="$(python3 -I - "$JSON" <<'PY'
import json, sys
d = json.load(open(sys.argv[1]))
for dev in d.get("result", {}).get("devices", []):
    if dev.get("connectionProperties", {}).get("tunnelState") == "connected" \
       and dev.get("hardwareProperties", {}).get("platform") == "iOS":
        print(dev["hardwareProperties"]["udid"])
PY
)"
  COUNT="$(printf '%s\n' "$DEVICES" | grep -c . || true)"
  if [[ "$COUNT" -eq 0 ]]; then
    echo "error: no connected iPhone found (unlock it, trust this Mac, enable Developer Mode)." >&2
    exit 1
  elif [[ "$COUNT" -gt 1 ]]; then
    echo "error: several devices connected; pass --udid <id>:" >&2
    printf '%s\n' "$DEVICES" >&2
    exit 1
  fi
  UDID="$DEVICES"
fi

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}"
export PATH="$JAVA_HOME/bin:$PATH"

DERIVED="$ROOT/iosApp/build"
xcodebuild -project "$ROOT/iosApp/iosApp.xcodeproj" -scheme iosApp -configuration Debug \
  -destination "id=$UDID" -derivedDataPath "$DERIVED" \
  DEVELOPMENT_TEAM="$TEAM" -allowProvisioningUpdates build

APP="$DERIVED/Build/Products/Debug-iphoneos/iosApp.app"
xcrun devicectl device install app --device "$UDID" "$APP"
echo "Installed on $UDID. Trust the developer profile on the iPhone if prompted."
