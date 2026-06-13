#!/usr/bin/env bash
set -e

echo "Installing dependencies..."
npm install

echo "Building extension..."
npx shadow-cljs release :extension

echo "Build complete. Output: release/extension.js"
