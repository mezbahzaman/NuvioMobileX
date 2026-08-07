#!/bin/sh
# Fail the build if the libmpv we are about to link is not the resize-patched engine.
#
# Live TV fullscreen depends on a locally-built libmpv carrying the resize-aware moltenvk
# context (mpv polls CAMetalLayer.drawableSize and resizes its swapchain). That engine lives
# in MPVKit/dist/release/xcframework/, which is gitignored — so a clean clone, a git worktree,
# or a /tmp release copy silently links the stock upstream MPVKit artifact instead. SPM resolves
# the path successfully and nothing warns. Build 114 shipped that way and fullscreen rendered a
# small box in the top-left corner.
#
# Usage:
#   verify-mpv-engine.sh                 # build-phase mode: check the source xcframework
#   verify-mpv-engine.sh <path>          # check a shipped .xcarchive, .app, or Mach-O binary

set -eu

MARKER='MoltenVK resize drawable'

fail() {
    # The "error:" prefix makes Xcode surface this in the Issue navigator.
    echo "error: $1" >&2
    exit 1
}

has_marker() {
    strings -a "$1" 2>/dev/null | grep -q "$MARKER"
}

# ---------------------------------------------------------------- artifact mode
if [ $# -gt 0 ]; then
    target="$1"
    [ -e "$target" ] || fail "no such path: $target"

    case "$target" in
        *.xcarchive) app=$(find "$target/Products/Applications" -maxdepth 1 -name '*.app' | head -1) ;;
        *.app)       app="$target" ;;
        *)           app="" ;;
    esac

    if [ -n "$app" ]; then
        # mpv is statically linked into the main executable; Frameworks/Libmpv.framework is a stub.
        binary="$app/$(/usr/libexec/PlistBuddy -c 'Print :CFBundleExecutable' "$app/Info.plist")"
    else
        binary="$target"
    fi

    [ -f "$binary" ] || fail "could not locate a binary to inspect under: $target"

    if has_marker "$binary"; then
        echo "OK: $binary carries the resize-patched libmpv."
        exit 0
    fi
    origin=$(strings -a "$binary" 2>/dev/null | grep -oE 'cross-file=[^ ]*' | head -1)
    fail "$binary was built against the STOCK libmpv — Live TV fullscreen will render a small
  box in the top-left corner. Built from: ${origin:-unknown}. Do not ship this archive."
fi

# ------------------------------------------------------------- build-phase mode
# SRCROOT is iosApp/ under Xcode; fall back to this script's location otherwise.
root="${SRCROOT:-$(cd "$(dirname "$0")/.." && pwd)}"
xcframework="$root/../MPVKit/dist/release/xcframework/Libmpv.xcframework"

[ -d "$xcframework" ] || fail "missing $xcframework — this checkout has no locally-built libmpv
  (MPVKit/dist is gitignored). Archive from the primary checkout, or rsync
  MPVKit/dist/release/xcframework/ into this copy before building."

case "${PLATFORM_NAME:-iphoneos}" in
    iphonesimulator) slice="ios-arm64_x86_64-simulator" ;;
    macosx)          slice="ios-arm64_x86_64-maccatalyst" ;;
    *)               slice="ios-arm64" ;;
esac

lib="$xcframework/$slice/Libmpv.framework/Libmpv"
[ -f "$lib" ] || fail "missing slice $slice in $xcframework"

if has_marker "$lib"; then
    exit 0
fi

origin=$(strings -a "$lib" 2>/dev/null | grep -oE 'cross-file=[^ ]*' | head -1)
fail "$lib is the STOCK upstream libmpv, not the resize-patched build — Live TV fullscreen
  would render a small box in the top-left corner. Built from: ${origin:-unknown}.
  Expected a locally-built engine containing '$MARKER'.
  Rebuild it in MPVKit, or rsync MPVKit/dist/release/xcframework/ from the primary checkout."
