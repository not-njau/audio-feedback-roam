#!/usr/bin/env bash
set -e

echo "Installing dependencies..."
npm install

echo "Building extension..."
npx shadow-cljs release audio-feedback

echo "Build complete. Output: release/extension.js"
