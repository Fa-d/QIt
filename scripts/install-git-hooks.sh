#!/bin/bash
# Points git at the shared hooks in scripts/git-hooks (bash 3.2 compatible).
set -euo pipefail

git config core.hooksPath scripts/git-hooks
echo "quality: git hooks installed (core.hooksPath=scripts/git-hooks)"
