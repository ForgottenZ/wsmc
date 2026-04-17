#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$ROOT_DIR/build"
MAIN_SRC_DIR="$ROOT_DIR/src/main/java"
TEST_SRC_DIR="$ROOT_DIR/src/test/java"
CLASSES_DIR="$BUILD_DIR/classes"
TEST_CLASSES_DIR="$BUILD_DIR/test-classes"
LIBS_DIR="$BUILD_DIR/libs"
JAR_PATH="$LIBS_DIR/voice-tunnel-mod.jar"

rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR" "$TEST_CLASSES_DIR" "$LIBS_DIR"

# Compile main sources
find "$MAIN_SRC_DIR" -name '*.java' -print0 | xargs -0 javac -encoding UTF-8 -d "$CLASSES_DIR"

# Create runnable/main jar (library style)
cat > "$BUILD_DIR/MANIFEST.MF" <<'MANIFEST'
Manifest-Version: 1.0
Implementation-Title: Voice Tunnel Mod
Implementation-Version: 0.2.0
MANIFEST

jar cfm "$JAR_PATH" "$BUILD_DIR/MANIFEST.MF" -C "$CLASSES_DIR" .

# Compile and run lightweight self-tests if present
if find "$TEST_SRC_DIR" -name '*.java' | grep -q .; then
  find "$TEST_SRC_DIR" -name '*.java' -print0 | xargs -0 javac -encoding UTF-8 -cp "$CLASSES_DIR" -d "$TEST_CLASSES_DIR"

  # Run codec self-test
  java -cp "$CLASSES_DIR:$TEST_CLASSES_DIR" voicetunnelmod.protocol.TunnelCodecSelfTest
fi

# Run loopback demo smoke test
java -cp "$CLASSES_DIR" voicetunnelmod.demo.LoopbackDemo

echo "Build success: $JAR_PATH"
