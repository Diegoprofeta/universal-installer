---
name: upgrade-app
description: Automates the process of upgrading the app version. Bumps versionCode and versionName in build.gradle.kts, updates the changelog, verifies the build, commits, pushes, and creates a GitHub release. Use when the user says "upgrade app", "release new version", or "bump version".
---

# Upgrade App Workflow

This skill automates the end-to-end process of releasing a new version of the application.

## Workflow Steps

### 1. Preparation
- Ensure the git working tree is clean (`git status`).
- Check `gh auth status` to ensure GitHub CLI is authenticated.

### 2. Research & Versioning
- Read `app/build.gradle.kts` to find the current `versionCode` and `versionName`.
- Suggest a new `versionName` (e.g., if current is `1.7.1`, suggest `1.7.2`).
- Increment `versionCode` by 1.

### 3. Update Files
- Update `app/build.gradle.kts` with the new version info.
- Fetch commits since the last tag:
  ```bash
  LAST_TAG=$(git describe --tags --abbrev=0)
  git log $LAST_TAG..HEAD --oneline
  ```
- Generate a concise changelog and write it to `fastlane/metadata/android/en-US/changelogs/<new-versionCode>.txt`.

### 4. User Confirmation
- **MANDATORY:** Present the generated changelog to the user.
- **WAIT** for the user to confirm or edit the changelog before proceeding.

### 5. Build Verification
- Run `./gradlew assembleDebug` to verify the build.
- If it fails, stop and report errors.

### 6. Git Operations
- Stage the changes:
  ```bash
  git add app/build.gradle.kts fastlane/metadata/android/en-US/changelogs/<new-versionCode>.txt
  ```
- Commit:
  ```bash
  git commit -m "chore: bump version to <versionName> (<versionCode>)"
  ```
- Push:
  ```bash
  git push origin main
  ```

### 7. GitHub Release
- Create and push the tag:
  ```bash
  git tag v<versionName>
  git push origin v<versionName>
  ```
- Create the GitHub release:
  ```bash
  gh release create v<versionName> --title "v<versionName>" --notes-file fastlane/metadata/android/en-US/changelogs/<new-versionCode>.txt
  ```

## Guardrails
- Never push or release without a successful `./gradlew assembleDebug`.
- Always wait for user confirmation on the changelog.
- Ensure the tag matches the `vX.Y.Z` format.
