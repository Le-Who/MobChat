#!/usr/bin/env bash
set -euo pipefail

ONLY_VERSION=${ONLY_VERSION:-}
DRY_RUN=${DRY_RUN:-0}
ALLOW_GRADLE_RESTART=${ALLOW_GRADLE_RESTART:-}

mod_version=$(sed -n 's/^mod_version=\(.*\)/\1/p' gradle.properties | tr -d '\r')
archive_base=$(sed -n 's/^archives_base_name=\(.*\)/\1/p' gradle.properties | tr -d '\r')
: "${mod_version:?Missing mod_version in gradle.properties}"
: "${archive_base:?Missing archives_base_name in gradle.properties}"

run_build() {
  ./gradlew runDatagen "$@" </dev/null
  ./gradlew build "$@" -x test -x compileTestJava --build-cache --parallel </dev/null && return 0
  [[ -n "$ALLOW_GRADLE_RESTART" ]] || return 1
  echo "Gradle failed; retrying with a fresh daemon..."
  ./gradlew build "$@" -x test -x compileTestJava --build-cache --parallel --no-daemon </dev/null
}

# Format: minecraft_version  yarn_mappings       loader_version  loom_version      fabric_version
VERSIONS=$(cat <<'EOF'
1.20    1.20+build.1       0.17.2    1.11-SNAPSHOT   0.83.0+1.20
1.20.1  1.20.1+build.10    0.17.2    1.11-SNAPSHOT   0.92.1+1.20.1
1.20.2  1.20.2+build.4     0.17.2    1.11-SNAPSHOT   0.91.6+1.20.2
1.20.3  1.20.3+build.1     0.17.2    1.11-SNAPSHOT   0.91.1+1.20.3
1.20.4  1.20.4+build.3     0.17.2    1.11-SNAPSHOT   0.97.0+1.20.4
1.20.5  1.20.5+build.1     0.17.2    1.11-SNAPSHOT   0.97.8+1.20.5
1.20.6  1.20.6+build.3     0.17.2    1.11-SNAPSHOT   0.100.8+1.20.6
1.21    1.21+build.9       0.17.2    1.11-SNAPSHOT   0.102.0+1.21
1.21.1  1.21.1+build.3     0.17.2    1.11-SNAPSHOT   0.116.3+1.21.1
1.21.2  1.21.2+build.1     0.17.2    1.11-SNAPSHOT   0.106.1+1.21.2
1.21.3  1.21.3+build.2     0.17.2    1.11-SNAPSHOT   0.114.1+1.21.3
1.21.4  1.21.4+build.8     0.17.2    1.11-SNAPSHOT   0.119.3+1.21.4
1.21.5  1.21.5+build.1     0.17.2    1.11-SNAPSHOT   0.128.1+1.21.5
1.21.6  1.21.6+build.1     0.17.2    1.11-SNAPSHOT   0.128.2+1.21.6
1.21.7  1.21.7+build.6     0.17.2    1.11-SNAPSHOT   0.128.2+1.21.7
EOF
)

while read -r mc_version _yarn_mapping loader_version _loom_version fabric_version; do
  [[ -z "$mc_version" || "$mc_version" =~ ^# ]] && continue
  [[ -n "$ONLY_VERSION" && "$ONLY_VERSION" != "$mc_version" ]] && continue

  echo "****"
  echo "Building for MC $mc_version  Fabric API $fabric_version"
  echo "****"

  gradle_properties=("-Pminecraft_version=$mc_version" "-Ploader_version=$loader_version"
    "-Pfabric_version=$fabric_version" "-Pmod_version=$mod_version" "-Parchives_base_name=$archive_base")
  jar="$archive_base-$mod_version+$mc_version.jar"

  if [[ "$DRY_RUN" == "1" ]]; then
    cat <<EOD
[DRY RUN] run './gradlew runDatagen ${gradle_properties[*]}'
[DRY RUN] run './gradlew build ${gradle_properties[*]} -x test -x compileTestJava --build-cache --parallel'
[DRY RUN] copy $jar and $jar.sha512 to the repository root
[DRY RUN] download fabric-api-$fabric_version.jar from FabricMC
EOD
    echo
    continue
  fi

  echo "Running build (datagen + mixin target validation)"
  run_build "${gradle_properties[@]}"
  if ! compgen -G "build/classes/java/main/*refmap.json" > /dev/null; then
    echo "Error: mixin refmap missing; target validation did not run" >&2
    exit 1
  fi
  cp -- "build/libs/$jar" "build/libs/$jar.sha512" .

  # Safe Forge/NeoForge packaging
  case "$mc_version" in
    "1.20.1") suffix="forge" ;;
    "1.21.1") suffix="neoforge" ;;
    *)        suffix="" ;;
  esac

  if [[ -n "$suffix" ]]; then
    connector_jar="${jar%.jar}-$suffix.jar"
    cp -- "$jar" "$connector_jar"
    marker_dir=$(mktemp -d)
    trap 'rm -f -- "$marker_dir/${suffix^^}"; rmdir -- "$marker_dir"' EXIT
    touch "$marker_dir/${suffix^^}"
    zip -j "$connector_jar" "$marker_dir/${suffix^^}"
    sha512sum "$connector_jar" > "$connector_jar.sha512"
    rm -- "$marker_dir/${suffix^^}"
    rmdir -- "$marker_dir"
    trap - EXIT
  fi

  # download Fabric API
  api_jar="fabric-api-${fabric_version}.jar"
  url="https://github.com/FabricMC/fabric/releases/download/${fabric_version//+/%2B}/${api_jar}"
  wget -q -O "$api_jar" "$url"

  echo
done <<< "$VERSIONS"
