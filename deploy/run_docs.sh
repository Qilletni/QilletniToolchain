#!/bin/bash
# /opt/qilletni/run_docs.sh
#
# This script accepts one argument: a library name (without spaces).
# It looks up the given library in /opt/qilletni/allowed_releases.json to retrieve:
#   - The GitHub repository (e.g. "RubbaBoy/MyLibrary1")
#   - The relative path to the "qilletni-src" directory in that repo.
#
# It then clones the repository into /tmp/<library>-repo and, finally, runs a Docker container
# (using Eclipse Temurin 27) that executes:
#
#    java -jar /tmp/app.jar <clone_path>/<qilletni-src>
#
# Requirements: git, curl, jq, docker, and /opt/qilletni/allowed_releases.json

# Define Docker container parameters.
CONTAINER_NAME="qilletni_app_${LIBRARY_NAME}"
SERVE_PATH="/srv/docker/nginx/php/docs.qilletni.dev"
CACHE_PATH="/opt/qilletni/cache"

set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <library_name>"
  exit 1
fi

LIBRARY_NAME="$1"

# Reject library names that contain spaces.
if [[ "$LIBRARY_NAME" =~ \  ]]; then
  echo "Error: Library name must not contain spaces."
  exit 1
fi

ALLOWED_JSON="/opt/qilletni/allowed_releases.json"
if [ ! -f "$ALLOWED_JSON" ]; then
  echo "Error: Allowed releases file not found at $ALLOWED_JSON"asd
  exit 1
fi

# Look up the repository and qilletni-src for the given library.
REPO_ENTRY=$(jq -r --arg lib "$LIBRARY_NAME" '.releases[] | select(.name == $lib) | .repo' "$ALLOWED_JSON")
QILLETNI_SRC=$(jq -r --arg lib "$LIBRARY_NAME" '.releases[] | select(.name == $lib) | .["qilletni-src"]' "$ALLOWED_JSON")

if [ -z "$REPO_ENTRY" ] || [ "$REPO_ENTRY" == "null" ]; then
  echo "Error: Library '$LIBRARY_NAME' is not allowed or not found in $ALLOWED_JSON"
  exit 1
fi

if [ -z "$QILLETNI_SRC" ] || [ "$QILLETNI_SRC" == "null" ]; then
  echo "Error: 'qilletni-src' path not found for library '$LIBRARY_NAME' in $ALLOWED_JSON"
  exit 1
fi

# Split the repository string into OWNER and REPO parts.
OWNER=$(echo "$REPO_ENTRY" | cut -d'/' -f1)
REPO=$(echo "$REPO_ENTRY" | cut -d'/' -f2)
echo "Using GitHub repository: ${OWNER}/${REPO}"
echo "Using qilletni-src path: ${QILLETNI_SRC}"

# Build the clone URL (using HTTPS).
CLONE_URL="https://github.com/${OWNER}/${REPO}.git"
CLONE_PATH="/tmp/${LIBRARY_NAME}-repo"

echo "Cloning repository from $CLONE_URL into $CLONE_PATH..."
# Remove any existing clone directory.
if [ -d "$CLONE_PATH" ]; then
  rm -rf "$CLONE_PATH"
fi

# Clone the repository.
git clone "$CLONE_URL" "$CLONE_PATH"
echo "Repository cloned to $CLONE_PATH."

# Compute the final parameter by appending the qilletni-src relative path.
FINAL_PARAM="${CLONE_PATH}/${QILLETNI_SRC}"
echo "Final parameter to be passed to jar: ${FINAL_PARAM}"

# Define the jar asset location.
TOOLCHAIN_JAR="/tmp/Qilletni.jar"

/opt/qilletni/download_toolchain.sh "${TOOLCHAIN_JAR}"

echo "download_toolchain.sh exited with code $?"

mkdir -p "${CACHE_PATH}"

echo "Stopping any existing container named ${CONTAINER_NAME}..."
if docker ps -q --filter "name=${CONTAINER_NAME}" | grep -q .; then
  docker stop "${CONTAINER_NAME}"
  docker rm "${CONTAINER_NAME}"
fi

echo "Pulling Eclipse Temurin 27 image..."
docker pull eclipse-temurin:27-jre

echo "Starting Docker container ${CONTAINER_NAME}..."
docker run --rm --name "${CONTAINER_NAME}" \
  -v "${SERVE_PATH}":"${SERVE_PATH}":rw \
  -v "${CACHE_PATH}":"${CACHE_PATH}":rw \
  -v "/tmp":"/tmp":rw \
  eclipse-temurin:27-jre \
  bash -c "\
    set -euo pipefail; \
    echo 'Running application with Java 27...'; \
    java -jar ${TOOLCHAIN_JAR} doc -o ${SERVE_PATH} -c ${CACHE_PATH} ${FINAL_PARAM} \
  "

echo "Deployment complete."
