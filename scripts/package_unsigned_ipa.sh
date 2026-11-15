#!/usr/bin/env bash
# Package an unsigned iOS device build as a sideloadable IPA with a SHA-256 sidecar.
#
# Usage: bash scripts/package_unsigned_ipa.sh PRODUCTS_DIRECTORY OUTPUT_IPA VERSION BUILD
#
#   PRODUCTS_DIRECTORY  Xcode device products directory (e.g. .../Build/Products/Release-iphoneos)
#                       containing exactly one .app bundle.
#   OUTPUT_IPA          Path of the IPA to create; it and OUTPUT_IPA.sha256 must not exist yet.
#   VERSION             CFBundleShortVersionString, MAJOR.MINOR.PATCH.
#   BUILD               CFBundleVersion, a numeric triple such as 1.0.1.
#
# The app is staged in a temporary copy, so the build output is never modified. The IPA and
# its checksum appear at their final paths only after the archive has been validated.
#
# Adapted from bpawnzZ/CivitDeck commits 9530081 and 1d24997 by bpawnzZ.

set -euo pipefail

die() {
  echo "package_unsigned_ipa: $*" >&2
  exit 1
}

[ "$#" -eq 4 ] || die "usage: $0 PRODUCTS_DIRECTORY OUTPUT_IPA VERSION BUILD"

PRODUCTS_DIR=$1
OUTPUT_IPA=$2
VERSION=$3
BUILD=$4

# Nine digits keeps every component within a signed 32-bit integer.
COMPONENT='(0|[1-9][0-9]{0,8})'
TRIPLE="^${COMPONENT}\.${COMPONENT}\.${COMPONENT}$"
[[ "$VERSION" =~ $TRIPLE ]] || die "VERSION must be MAJOR.MINOR.PATCH, got '$VERSION'"
[[ "$BUILD" =~ $TRIPLE ]] || die "BUILD must be a numeric triple such as 1.0.1, got '$BUILD'"

[ -d "$PRODUCTS_DIR" ] || die "products directory not found: $PRODUCTS_DIR"
case "$OUTPUT_IPA" in
  *.ipa) ;;
  *) die "OUTPUT_IPA must end with .ipa: $OUTPUT_IPA" ;;
esac

OUTPUT_DIR=$(dirname -- "$OUTPUT_IPA")
OUTPUT_NAME=$(basename -- "$OUTPUT_IPA")
[ -d "$OUTPUT_DIR" ] || die "output directory not found: $OUTPUT_DIR"
OUTPUT_DIR=$(cd -- "$OUTPUT_DIR" && pwd -P)
FINAL_IPA="$OUTPUT_DIR/$OUTPUT_NAME"
FINAL_SHA="$FINAL_IPA.sha256"
for path in "$FINAL_IPA" "$FINAL_SHA"; do
  if [ -e "$path" ] || [ -L "$path" ]; then
    die "refusing to overwrite existing output: $path"
  fi
done

apps=()
for candidate in "$PRODUCTS_DIR"/*.app; do
  [ -d "$candidate" ] && apps+=("$candidate")
done
[ "${#apps[@]}" -eq 1 ] || die "expected exactly one .app in $PRODUCTS_DIR, found ${#apps[@]}"
APP=$(cd -- "${apps[0]}" && pwd -P)
APP_NAME=$(basename -- "$APP")

case "$OUTPUT_DIR/" in
  "$APP"/*) die "output must not be inside the app bundle: $FINAL_IPA" ;;
esac

# Embedded Python reads the plist because built apps carry a binary Info.plist and plutil
# is macOS-only; the same logic then also runs in the Linux-hosted fixture tests.
python3 - "$APP" <<'PY' || die "invalid app bundle: $APP"
import os, plistlib, stat, sys

app = sys.argv[1]
def fail(message):
    print(f"package_unsigned_ipa: {message}", file=sys.stderr)
    sys.exit(1)

info_path = os.path.join(app, "Info.plist")
if not os.path.isfile(info_path) or os.path.islink(info_path):
    fail("Info.plist is missing")
try:
    with open(info_path, "rb") as handle:
        info = plistlib.load(handle)
except Exception as error:
    fail(f"Info.plist is unreadable: {error}")
if not isinstance(info, dict):
    fail("Info.plist is not a dictionary")

platforms = info.get("CFBundleSupportedPlatforms")
if not isinstance(platforms, list) or platforms != ["iPhoneOS"]:
    fail(f"CFBundleSupportedPlatforms must be ['iPhoneOS'], got {platforms!r}")
if info.get("DTPlatformName", "iphoneos") != "iphoneos":
    fail(f"DTPlatformName must be iphoneos, got {info.get('DTPlatformName')!r}")

executable = info.get("CFBundleExecutable")
if not isinstance(executable, str) or not executable:
    fail("CFBundleExecutable is missing or empty")
if "/" in executable or "\\" in executable or executable in (".", ".."):
    fail(f"CFBundleExecutable must be a file name inside the bundle, got {executable!r}")
executable_path = os.path.join(app, executable)
if os.path.islink(executable_path):
    fail(f"CFBundleExecutable must not be a symlink: {executable}")
if os.path.dirname(os.path.realpath(executable_path)) != os.path.realpath(app):
    fail(f"CFBundleExecutable escapes the bundle: {executable}")
try:
    status = os.stat(executable_path)
except FileNotFoundError:
    fail(f"executable not found: {executable}")
if not stat.S_ISREG(status.st_mode):
    fail(f"executable is not a regular file: {executable}")
if status.st_size == 0:
    fail(f"executable is empty: {executable}")
# 64-bit thin Mach-O or a universal binary; anything else cannot launch on a device.
with open(executable_path, "rb") as handle:
    magic = handle.read(4)
if magic not in (b"\xcf\xfa\xed\xfe", b"\xca\xfe\xba\xbe"):
    fail(f"executable is not a Mach-O binary: {executable}")
PY

STAGING=$(mktemp -d "$OUTPUT_DIR/.package_unsigned_ipa.XXXXXX")
PUBLISHED=()
cleanup() {
  local status=$?
  rm -rf -- "$STAGING"
  if [ "$status" -ne 0 ] && [ "${#PUBLISHED[@]}" -gt 0 ]; then
    rm -f -- "${PUBLISHED[@]}"
  fi
  exit "$status"
}
trap cleanup EXIT
trap 'exit 1' HUP INT TERM

mkdir "$STAGING/Payload"
# -P keeps symlinks as links and -p keeps modes on both BSD and GNU cp.
cp -pPR -- "$APP" "$STAGING/Payload/"
STAGED_APP="$STAGING/Payload/$APP_NAME"

# A sideloading tool re-signs the whole bundle, so stale signatures and profiles are dropped.
find "$STAGED_APP" -name _CodeSignature -type d -prune -exec rm -rf -- {} +
find "$STAGED_APP" -name embedded.mobileprovision -type f -exec rm -f -- {} +

python3 - "$STAGED_APP/Info.plist" "$VERSION" "$BUILD" <<'PY'
import plistlib, sys

path, version, build = sys.argv[1:]
with open(path, "rb") as handle:
    raw = handle.read()
info = plistlib.loads(raw)
info["CFBundleShortVersionString"] = version
info["CFBundleVersion"] = build
fmt = plistlib.FMT_BINARY if raw.startswith(b"bplist") else plistlib.FMT_XML
with open(path, "wb") as handle:
    plistlib.dump(info, handle, fmt=fmt)
PY

STAGED_IPA="$STAGING/$OUTPUT_NAME"
(cd "$STAGING" && zip -qry "$STAGED_IPA" Payload) || die "zip failed"

python3 - "$STAGED_IPA" "$APP_NAME" "$VERSION" "$BUILD" <<'PY' || die "archive validation failed"
import plistlib, stat, sys, zipfile

ipa, app_name, version, build = sys.argv[1:]
def fail(message):
    print(f"package_unsigned_ipa: {message}", file=sys.stderr)
    sys.exit(1)

prefix = f"Payload/{app_name}/"
with zipfile.ZipFile(ipa) as archive:
    bad = archive.testzip()
    if bad is not None:
        fail(f"corrupt entry: {bad}")
    entries = {item.filename: item for item in archive.infolist()}
    for name in entries:
        if not (name == "Payload/" or name.startswith(prefix) or name == prefix.rstrip("/")):
            fail(f"unexpected archive entry: {name}")
        if "/_CodeSignature/" in f"/{name}" or name.endswith("embedded.mobileprovision"):
            fail(f"signing material left in archive: {name}")
    info = plistlib.loads(archive.read(prefix + "Info.plist"))
    if info.get("CFBundleShortVersionString") != version or info.get("CFBundleVersion") != build:
        fail("stamped version does not match")
    executable = prefix + info["CFBundleExecutable"]
    item = entries.get(executable)
    if item is None or item.file_size == 0:
        fail(f"executable missing or empty in archive: {executable}")
    if not (item.external_attr >> 16) & stat.S_IXUSR:
        fail(f"executable lost its execute permission: {executable}")
PY

(cd "$STAGING" && shasum -a 256 "$OUTPUT_NAME" > "$OUTPUT_NAME.sha256") || die "checksum failed"
(cd "$STAGING" && shasum -a 256 -c "$OUTPUT_NAME.sha256" > /dev/null) || die "checksum verification failed"

# ln fails instead of replacing a file created after the collision check above. Signals are
# ignored while publishing so an interruption cannot leave a linked file that cleanup misses.
trap '' HUP INT TERM
ln -- "$STAGED_IPA" "$FINAL_IPA" || die "could not publish $FINAL_IPA"
PUBLISHED+=("$FINAL_IPA")
ln -- "$STAGED_IPA.sha256" "$FINAL_SHA" || die "could not publish $FINAL_SHA"
PUBLISHED+=("$FINAL_SHA")
trap 'exit 1' HUP INT TERM

echo "Packaged $FINAL_IPA ($(wc -c < "$FINAL_IPA" | tr -d ' ') bytes, version $VERSION, build $BUILD)"
