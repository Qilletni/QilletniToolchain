#!/bin/bash
# download_latest_toolchain.sh
#
# This script downloads the latest release .jar from the GitHub repository
# Qilletni/QilletniToolchain and saves it to the path specified as the sole argument.
#
# It first tries to use the /releases/latest endpoint. If that fails (404),
# it falls back to listing all releases and picking the first one.
#
# Usage: ./download_latest_toolchain.sh /path/to/destination.jar
#
# Requirements:
#   - curl
#   - jq
#
# Exit immediately if any command fails.
set -euo pipefail

# Check for a destination path argument.
if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <download_path>"
  exit 1
fi

DOWNLOAD_PATH="$1"

# Repository details.
REPO_OWNER="Qilletni"
REPO_NAME="QilletniToolchain"

# Optionally, if you have a GitHub token set in the environment, use it:
if [ -n "${GITHUB_TOKEN:-}" ]; then
  AUTH_HEADER="Authorization: token ${GITHUB_TOKEN}"
fi

# Try to get the latest release.
API_URL_LATEST="https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/releases/latest"
echo "Fetching latest release information from ${REPO_OWNER}/${REPO_NAME}..."
LATEST_RELEASE_JSON=$(curl -s -H "$AUTH_HEADER" "$API_URL_LATEST")

# Check if we got a valid release (the presence of an "assets" field is our cue).
if echo "$LATEST_RELEASE_JSON" | jq -e 'has("assets")' > /dev/null 2>&1; then
  RELEASE_JSON="$LATEST_RELEASE_JSON"
else
  echo "Stable release not found. Falling back to list releases..."
  API_URL_ALL="https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/releases"
  ALL_RELEASES_JSON=$(curl -s -H "$AUTH_HEADER" "$API_URL_ALL")
  # Get the first release in the list (most recent release, even if a pre-release)
  RELEASE_JSON=$(echo "$ALL_RELEASES_JSON" | jq '.[0]')
fi

# Extract the asset URL for the .jar file (assuming its name ends with .jar).
ASSET_URL=$(echo "$RELEASE_JSON" | jq -r '.assets[] | select(.name | endswith(".jar")) | .browser_download_url')

if [ -z "$ASSET_URL" ] || [ "$ASSET_URL" == "null" ]; then
  echo "Error: Could not find a .jar asset in the latest release of ${REPO_OWNER}/${REPO_NAME}."
  exit 1
fi

# Extract Asset ID (instead of URL)
ASSET_ID=$(echo "$RELEASE_JSON" | jq -r '.assets[] | select(.name | endswith(".jar")) | .id')

if [ -z "$ASSET_ID" ] || [ "$ASSET_ID" == "null" ]; then
  echo "Error: Could not find a valid .jar asset in the latest release."
  exit 1
fi

# Download using GitHub API
curl --fail -H "$AUTH_HEADER" -H "Accept: application/octet-stream" \
    --location "https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/releases/assets/${ASSET_ID}" \
    -o "$DOWNLOAD_PATH"


echo "Download complete: ${DOWNLOAD_PATH}"
