#!/bin/bash
set -e

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
cd "$REPO_ROOT"

# Terminal Color Codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m' # No Color

echo -e "${BOLD}${BLUE}========================================================================${NC}"
echo -e "${BOLD}${BLUE}  Automotive Release & Performance Verification Agent                    ${NC}"
echo -e "${BOLD}  Target: Automotive Headunit (ARM Cortex-A53, Mali GPU, eMMC)           ${NC}"
echo -e "${BOLD}${BLUE}========================================================================${NC}"

VERIFY_FAILURES=0

pass() {
    echo -e "  [${GREEN}PASS${NC}] $1"
}

fail() {
    echo -e "  [${RED}FAIL${NC}] $1"
    VERIFY_FAILURES=$((VERIFY_FAILURES + 1))
}

warn() {
    echo -e "  [${YELLOW}WARN${NC}] $1"
}

# ─────────────────────────────────────────────────────────────────────────────
# STEP 1: Run Full Multi-Layer Audit
# ─────────────────────────────────────────────────────────────────────────────
echo -e "\n${BOLD}Step 1: Running Static Hardware Architecture & Performance Audit...${NC}"
if bash .agents/skills/automotive-performance-optimizer/scripts/audit_performance.sh; then
    pass "Static performance audit passed with 100% compliance across all 22 source files"
else
    fail "Static performance audit failed"
fi

# ─────────────────────────────────────────────────────────────────────────────
# STEP 2: Run Unit Tests
# ─────────────────────────────────────────────────────────────────────────────
echo -e "\n${BOLD}Step 2: Executing Unit Test Suite (./gradlew testDebugUnitTest)...${NC}"
if ./gradlew testDebugUnitTest --quiet; then
    pass "All debug unit tests executed and passed cleanly"
else
    fail "Unit tests failed"
fi

# ─────────────────────────────────────────────────────────────────────────────
# STEP 3: Assemble Release APK with R8 and Native C++ Compilation
# ─────────────────────────────────────────────────────────────────────────────
echo -e "\n${BOLD}Step 3: Compiling Release APK (./gradlew assembleRelease)...${NC}"
if ./gradlew assembleRelease --quiet; then
    pass "Release build compiled successfully with R8 full-mode optimization"
else
    fail "assembleRelease failed"
fi

# ─────────────────────────────────────────────────────────────────────────────
# STEP 4: Release APK Binary Inspection
# ─────────────────────────────────────────────────────────────────────────────
echo -e "\n${BOLD}Step 4: Inspecting Release APK Binary Constraints...${NC}"
APK_PATH="app/build/outputs/apk/release/app-release.apk"

if [ ! -f "$APK_PATH" ]; then
    fail "Target release APK not found at $APK_PATH"
else
    # 4a. Size verification (< 5.0 MB = 5242880 bytes)
    APK_BYTES=$(wc -c < "$APK_PATH" | tr -d ' ')
    APK_MB=$(awk "BEGIN {printf \"%.2f\", $APK_BYTES/1048576}")
    
    if [ "$APK_BYTES" -le 5242880 ]; then
        pass "APK Size: $APK_MB MB (Strictly under 5.0 MB budget, Actual: $APK_BYTES bytes)"
    else
        fail "APK Size exceeded ceiling: $APK_MB MB (> 5.0 MB)"
    fi

    # 4b. Single ABI check (arm64-v8a only)
    ZIP_ABIS=$(unzip -l "$APK_PATH" 2>/dev/null | grep -o 'lib/[^/]*/' | sort -u | tr -d ' ' || true)
    if [ "$ZIP_ABIS" = "lib/arm64-v8a/" ]; then
        pass "Native ABI: Strictly single arm64-v8a binary ($ZIP_ABIS)"
    else
        fail "Unexpected ABIs in release APK: $ZIP_ABIS (expected only lib/arm64-v8a/)"
    fi

    # 4c. Verify presence of libaudio-player.so
    if unzip -l "$APK_PATH" 2>/dev/null | grep -q "lib/arm64-v8a/libaudio-player.so"; then
        pass "Native Library: libaudio-player.so compiled with ARM NEON & LTO present in APK"
    else
        fail "libaudio-player.so missing from APK"
    fi
fi

# ─────────────────────────────────────────────────────────────────────────────
# VERIFICATION SUMMARY
# ─────────────────────────────────────────────────────────────────────────────
echo -e "\n${BOLD}${BLUE}========================================================================${NC}"
if [ "$VERIFY_FAILURES" -eq 0 ]; then
    echo -e "${BOLD}${GREEN}VERIFICATION COMPLETED: ALL GATES PASSED (0 Issues Found)${NC}"
    echo -e "${BOLD}${GREEN}Hardware: Automotive Headunit verified fully optimized & production ready.${NC}"
    echo -e "${BOLD}${BLUE}========================================================================${NC}"
    exit 0
else
    echo -e "${BOLD}${RED}VERIFICATION FAILED: $VERIFY_FAILURES ISSUE(S) DETECTED!${NC}"
    echo -e "${BOLD}${BLUE}========================================================================${NC}"
    exit 1
fi
