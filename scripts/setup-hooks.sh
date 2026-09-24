#!/usr/bin/env bash
# =============================================================================
# FixLink - Setup Git Pre-Commit Hooks (macOS / Linux / Git Bash)
# =============================================================================

set -e

echo "Configuring Git core.hooksPath to .githooks..."
git config core.hooksPath .githooks

# Ensure the hook is executable
chmod +x .githooks/pre-commit 2>/dev/null || true

echo "✅ [SUCCESS] Git pre-commit hooks configured successfully!"
echo "Pre-commit hooks will automatically validate code formatting before each commit."
