#!/bin/bash
set -e

TITLE="${1:?Error: PR title is required. Usage: create-pr.sh <title> <body-file>}"
BODY_FILE="${2:?Error: Body file is required. Usage: create-pr.sh <title> <body-file>}"

if [ ! -f "$BODY_FILE" ]; then
  echo "ERROR: Body file not found: $BODY_FILE" >&2
  exit 1
fi

CURRENT=$(git branch --show-current)
case "$CURRENT" in
  feature/*)  BASE="develop" ;;
  develop)    BASE="master" ;;
  *)          BASE=$(gh pr view --json baseRefName -q .baseRefName 2>/dev/null || echo "develop") ;;
esac

if ! git ls-remote --exit-code --heads origin "$BASE" >/dev/null 2>&1; then
  DEFAULT_BASE=$(gh repo view --json defaultBranchRef -q .defaultBranchRef.name 2>/dev/null || true)
  if [ -n "$DEFAULT_BASE" ]; then
    echo "WARNING: Base branch '$BASE' does not exist on origin. Using default branch '$DEFAULT_BASE'." >&2
    BASE="$DEFAULT_BASE"
  fi
fi

ARGS=(gh pr create --title "$TITLE" --body-file "$BODY_FILE" --base "$BASE")

echo "Creating PR..."
echo "  Title : $TITLE"
echo "  Base  : $BASE"
echo ""

"${ARGS[@]}"
