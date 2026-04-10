#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
JNI_LIBS_DIR="${REPO_ROOT}/android/app/src/main/jniLibs"

cd "${REPO_ROOT}"

cargo ndk \
  -t arm64-v8a \
  -t armeabi-v7a \
  -t x86_64 \
  -p 24 \
  -o "${JNI_LIBS_DIR}" \
  build --release -p sprint-sync-protocol-jni
