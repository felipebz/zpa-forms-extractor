#!/usr/bin/env bash
# Verifies the JReleaser changelog and that the release has Conventional Commit input.
# Run from the repository root with RELEASE_VERSION set.
# Usage: verify-release-changelog.sh [changelog-file]
set -euo pipefail

: "${RELEASE_VERSION:?RELEASE_VERSION must be set}"
changelog_file=${1:-build/jreleaser/release/CHANGELOG.md}

[[ -s ${changelog_file} ]] || {
  printf 'Generated changelog is missing or empty\n' >&2
  exit 1
}
changelog=$(<"${changelog_file}")
assets=(
  "zpa-forms-extractor-${RELEASE_VERSION}.zip"
  "zpa-forms-extractor-${RELEASE_VERSION}.tar"
)
for asset in "${assets[@]}"; do
  [[ ${changelog} == *"${asset}"* ]] || {
    printf 'Changelog does not contain asset filename %s\n' "${asset}" >&2
    exit 1
  }
  url="https://github.com/felipebz/zpa-forms-extractor/releases/download/${RELEASE_VERSION}/${asset}"
  [[ ${changelog} == *"${url}"* ]] || {
    printf 'Changelog does not contain asset URL %s\n' "${url}" >&2
    exit 1
  }
done

# Release Please commits ("chore(main): release X.Y.Z") are always conventional and can
# appear several times in the range (release PR branch commit plus merge commit), so they
# must never count as release input: only other commits since the previous tag do.
# The patterns live in variables: an inline `[^)]` makes Bash's [[ ]] parser fail.
conventional_re='^[a-z]+(\([^)]+\))?!?:[[:space:]]+'
release_commit_re='^chore\(main\): release [0-9]+\.[0-9]+\.[0-9]+$'
# The first release has no previous tag: its input is the whole history.
if previous_tag=$(git describe --tags --abbrev=0 HEAD^ 2>/dev/null); then
  range="${previous_tag}..HEAD"
else
  previous_tag='the start of history'
  range=HEAD
fi
has_conventional=false
while IFS= read -r subject; do
  [[ ${subject} =~ ${release_commit_re} ]] && continue
  if [[ ${subject} =~ ${conventional_re} ]]; then
    has_conventional=true
    break
  fi
done < <(git log --format=%s "${range}")
[[ ${has_conventional} == true ]] || {
  printf 'No Conventional Commit subject found between %s and the release commit\n' "${previous_tag}" >&2
  exit 1
}
