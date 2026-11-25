#!/bin/bash

set -e

CURSEFORGE_API_KEY=${CURSEFORGE_API_KEY}
CHANGELOG_FILE="./CHANGELOG.md"
API_URL="https://minecraft.curseforge.com/api"
PROJECT_ID=${CURSEFORGE_PROJECT_ID:-}
USER_AGENT=${CURSEFORGE_USER_AGENT:-"MobChat-CreatureChat-Fork:local"}
SLEEP_DURATION=5

if [ -z "$PROJECT_ID" ]; then
  echo "ERROR: Set CURSEFORGE_PROJECT_ID for this fork before deploying."
  exit 1
fi

fetch_base_version_id() {
  local base_version=$(echo "$1" | grep -oE '^[0-9]+\.[0-9]+')
  local version_types_cache="/tmp/version_types.json"

  if [ ! -f "$version_types_cache" ]; then
    curl --retry 3 --retry-delay 5 -s -H "X-Api-Token: $CURSEFORGE_API_KEY" "$API_URL/game/version-types" > "$version_types_cache"
  fi

  local version_types_response=$(cat "$version_types_cache")
  local base_version_id=$(echo "$version_types_response" | jq -r --arg base_version "Minecraft $base_version" '.[] | select(.name == $base_version) | .id')

  if [ -z "$base_version_id" ]; then
    echo "ERROR: Base version ID not found."
    exit 1
  fi

  echo "$base_version_id"
}

fetch_game_version_ids() {
  local minecraft_version="$1"

  local base_version_id=$(fetch_base_version_id "$minecraft_version")

  local game_versions_cache="/tmp/game_versions.json"
  if [ ! -f "$game_versions_cache" ]; then
    curl --retry 3 --retry-delay 5 -s -H "X-Api-Token: $CURSEFORGE_API_KEY" "$API_URL/game/versions" > "$game_versions_cache"
  fi

  local response=$(cat "$game_versions_cache")

  local minecraft_id=$(echo "$response" | jq -r --arg base_version_id "$base_version_id" --arg full_version "$minecraft_version" '.[] | select(.gameVersionTypeID == ($base_version_id | tonumber) and .name == $full_version) | .id' | head -n 1)

  if [ -z "$minecraft_id" ]; then
    echo "ERROR: Minecraft version ID not found."
    exit 1
  fi

  local client_id=$(echo "$response" | jq -r '.[] | select(.name == "Client") | .id')
  local server_id=$(echo "$response" | jq -r '.[] | select(.name == "Server") | .id')
  local fabric_id=$(echo "$response" | jq -r '.[] | select(.name == "Fabric") | .id')
  local forge_id=$(echo "$response" | jq -r '.[] | select(.name == "Forge") | .id')
  local neoforge_id=$(echo "$response" | jq -r '.[] | select(.name == "NeoForge") | .id')

  if [ -z "$client_id" ] || [ -z "$server_id" ] || ([ -z "$fabric_id" ] && [ -z "$forge_id" ]); then
    echo "ERROR: One or more game version IDs not found."
    exit 1
  fi

  echo "$client_id $server_id $fabric_id $forge_id $minecraft_id $neoforge_id"
}

CHANGELOG=$(awk '/^## \[/{ if (p) exit; p=1 } p' "$CHANGELOG_FILE")
echo "CHANGELOG:"
echo "$CHANGELOG"
echo ""

if echo "$CHANGELOG" | grep -qi "UNRELEASED"; then
  echo "ERROR: Changelog contains UNRELEASED. Please finalize the changelog before deploying."
  exit 1
fi

VERSION=$(echo "$CHANGELOG" | head -n 1 | sed -n 's/^## \[\(.*\)\] - .*/\1/p')
echo "VERSION:"
echo "$VERSION"
echo ""

for FILE in creaturechat*.jar; do
  if [ -f "$FILE" ]; then
    echo "--------------$FILE----------------"
    FILE_BASENAME=$(basename "$FILE")
    OUR_VERSION=$(echo "$FILE_BASENAME" | sed -n 's/creaturechat-\(.*\)+.*\.jar/\1/p')
    MINECRAFT_VERSION=$(echo "$FILE_BASENAME" | sed -n 's/.*+\([0-9.]*\)\(-forge\|-neoforge\)*\.jar/\1/p')
    VERSION_NUMBER="$OUR_VERSION-$MINECRAFT_VERSION"

    if [ -z "$OUR_VERSION" ] || [ -z "$MINECRAFT_VERSION" ] || [ "$OUR_VERSION" != "$VERSION" ]; then
      echo "ERROR: Version mismatch or missing version information in $FILE_BASENAME. OUR_VERSION: $OUR_VERSION, MINECRAFT_VERSION: $MINECRAFT_VERSION, EXPECTED VERSION: $VERSION"
      exit 1
    fi

    echo "Preparing to upload $FILE_BASENAME as version $VERSION_NUMBER..."

    GAME_TYPE_ID=$(fetch_base_version_id "$MINECRAFT_VERSION")
    GAME_VERSION_IDS=($(fetch_game_version_ids "$MINECRAFT_VERSION"))

    echo "Minecraft Type ID: $GAME_TYPE_ID"
    echo "Minecraft Versions IDs (client_id: ${GAME_VERSION_IDS[0]}, server_id: ${GAME_VERSION_IDS[1]}, fabric_id: ${GAME_VERSION_IDS[2]}, forge_id: ${GAME_VERSION_IDS[3]}, minecraft_id: ${GAME_VERSION_IDS[4]}, neoforge_id: ${GAME_VERSION_IDS[5]})"

    if [[ "$FILE_BASENAME" == *"-forge.jar" ]]; then
      DEPENDENCY_SLUGS=("sinytra-connector" "forgified-fabric-api")
      GAME_VERSIONS="[${GAME_VERSION_IDS[0]}, ${GAME_VERSION_IDS[1]}, ${GAME_VERSION_IDS[3]}, ${GAME_VERSION_IDS[4]}]"
    elif [[ "$FILE_BASENAME" == *"-neoforge.jar" ]]; then
      DEPENDENCY_SLUGS=("sinytra-connector" "forgified-fabric-api")
      GAME_VERSIONS="[${GAME_VERSION_IDS[0]}, ${GAME_VERSION_IDS[1]}, ${GAME_VERSION_IDS[5]}, ${GAME_VERSION_IDS[4]}]"
    else
      DEPENDENCY_SLUGS=("fabric-api")
      GAME_VERSIONS="[${GAME_VERSION_IDS[0]}, ${GAME_VERSION_IDS[1]}, ${GAME_VERSION_IDS[2]}, ${GAME_VERSION_IDS[4]}]"
    fi

    RELATIONS=$(for slug in "${DEPENDENCY_SLUGS[@]}"; do jq -n --arg slug "$slug" '{"slug": $slug, "type": "requiredDependency"}'; done | jq -s .)

    PAYLOAD=$(jq -n --arg changelog "$CHANGELOG" \
      --arg changelogType "markdown" \
      --arg displayName "$FILE_BASENAME" \
      --argjson gameVersions "$GAME_VERSIONS" \
      --argjson gameVersionTypeIds "[$GAME_TYPE_ID]" \
      --arg releaseType "release" \
      --argjson relations "$RELATIONS" \
      '{
        "changelog": $changelog,
        "changelogType": $changelogType,
        "displayName": $displayName,
        "gameVersions": $gameVersions | map(tonumber),
        "gameVersionTypeIds": $gameVersionTypeIds,
        "releaseType": $releaseType,
        "relations": {
          "projects": $relations
        }
      }')

    echo "$PAYLOAD" > metadata.json

    sleep $SLEEP_DURATION

    echo "Uploading $FILE_BASENAME as version $VERSION_NUMBER..."
    HTTP_RESPONSE=$(curl --retry 3 --retry-delay 5 --fail -o response.txt -w "\nHTTP Code: %{http_code}\n" -X POST "$API_URL/projects/$PROJECT_ID/upload-file" \
      -H "X-Api-Token: $CURSEFORGE_API_KEY" \
      -H "User-Agent: $USER_AGENT" \
      -F "metadata=<metadata.json;type=application/json" \
      -F "file=@$FILE;type=application/java-archive")

    echo "Response:"
    cat response.txt
    echo "$HTTP_RESPONSE"

    echo "Uploaded $FILE_BASENAME as version $VERSION_NUMBER."
  fi
done
