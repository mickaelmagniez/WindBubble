#!/usr/bin/env bash
# Prepares a release: bumps the version, seeds the changelogs, commits and tags.
# It never pushes: the final command is printed so you decide when it goes public.
set -euo pipefail

BUILD_FILE="app/build.gradle.kts"
FASTLANE_DIR="fastlane/metadata/android"

die() { echo "error: $*" >&2; exit 1; }

[[ $# -eq 1 ]] || die "usage: $0 <versionName>   (e.g. $0 1.1.0)"
NEW_NAME="$1"
[[ "$NEW_NAME" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "version must look like 1.2.3"

cd "$(dirname "$0")/.."
[[ -f "$BUILD_FILE" ]] || die "run this from the repository"
[[ -z "$(git status --porcelain)" ]] || die "working tree is dirty, commit or stash first"

BRANCH="$(git rev-parse --abbrev-ref HEAD)"
[[ "$BRANCH" == "main" ]] || echo "warning: releasing from '$BRANCH', not 'main'"

OLD_CODE="$(grep -oP 'versionCode = \K[0-9]+' "$BUILD_FILE")"
OLD_NAME="$(grep -oP 'versionName = "\K[^"]+' "$BUILD_FILE")"
NEW_CODE=$((OLD_CODE + 1))

git rev-parse "v$NEW_NAME" >/dev/null 2>&1 && die "tag v$NEW_NAME already exists"

echo "  $OLD_NAME ($OLD_CODE)  ->  $NEW_NAME ($NEW_CODE)"

# Every store reads the changelog of the *version code*, not the name.
for locale in en-US fr-FR; do
    changelog="$FASTLANE_DIR/$locale/changelogs/$NEW_CODE.txt"
    if [[ ! -s "$changelog" ]]; then
        mkdir -p "$(dirname "$changelog")"
        git log --pretty='- %s' "v$OLD_NAME..HEAD" 2>/dev/null > "$changelog" ||
            echo "- " > "$changelog"
        echo "  seeded $changelog — edit it before publishing"
    fi
done

sed -i "s/versionCode = $OLD_CODE/versionCode = $NEW_CODE/" "$BUILD_FILE"
sed -i "s/versionName = \"$OLD_NAME\"/versionName = \"$NEW_NAME\"/" "$BUILD_FILE"

echo "  running checks..."
./gradlew testDebugUnitTest lintDebug --quiet

read -r -p "  changelogs edited and ready to tag v$NEW_NAME? [y/N] " answer
[[ "$answer" == "y" || "$answer" == "Y" ]] || die "aborted (version bump left in the working tree)"

git add "$BUILD_FILE" "$FASTLANE_DIR"
git commit -m "Release $NEW_NAME"
git tag -a "v$NEW_NAME" -m "Wind Bubble $NEW_NAME"

cat <<EOF

  Ready. Publish with:

      git push origin $BRANCH && git push origin v$NEW_NAME

  The release workflow then builds the signed APK + AAB and attaches them to
  the GitHub Release.
EOF
