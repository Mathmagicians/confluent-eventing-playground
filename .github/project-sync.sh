#!/usr/bin/env bash
# The project board follows the release plans in docs/releases, archived/ aside. Each catalog row is a feature and an
# issue: its body the feature's section of each phase file, its milestone the version in the overview's file name, its
# issue field phase the row's state; the project's auto-add workflow puts the issue on the board. A row without an
# issue gets one, found by its title or created, and its link is written into the row.
# Usage: project-sync.sh <owner/repo> <branch>
set -euo pipefail
repo=$1 branch=$2
owner=${repo%%/*}
blob=https://github.com/$repo/blob/$branch
field=$(gh api "orgs/$owner/issue-fields" --jq '.[] | select(.name == "phase")')
field_id=$(jq -r '.id // empty' <<<"$field")
[ -n "$field_id" ] || { echo "$owner has no issue field phase" >&2; exit 1; }
sep=$'\x1f'

# the feature's section of a phase file under the phase's name, its links absolute into the branch
section() { # <feature> <phase> <file>
    local text
    text=$(awk -v h="## $1" '$0 == h {on = 1; next} on && /^## / {exit} on' "$3")
    [ -n "${text//[[:space:]]/}" ] || return 0
    printf '\n## %s\n%s\n' "$2" "$text" | sed -E \
        -e "s|\]\(\.\./\.\./\.\./|](${blob}/|g" \
        -e "s|\]\(\.\./\.\./|](${blob}/docs/|g" \
        -e "s|\]\(\.\./|](${blob}/docs/releases/|g" \
        -e "s|\]\(#|](${blob}/${3}#|g" \
        -e "s|\]\(([^)/:#]+\.md)|](${blob}/${3%/*}/\1|g"
}

for overview in docs/releases/*/release-*.md; do
    case $overview in docs/releases/archived/*) continue ;; esac
    dir=${overview%/*}
    version=$(basename "$overview" .md)
    version=${version##*-}
    gh api "repos/$repo/milestones?state=all&per_page=100" --jq '.[].title' | grep -qxF "$version" \
        || gh api "repos/$repo/milestones" -f title="$version" --silent
    # the catalog's rows: the line, the feature, the issue cell, the state
    awk -F'|' -v OFS="$sep" '/^## Catalog/ {on = 1; next} on && /^## / {exit}
        on && /^\| / && !/^\| Feature / {line = $0; for (i = 2; i <= 4; i++) gsub(/^ +| +$/, "", $i); print line, $2, $3, $4}' "$overview" |
    while IFS="$sep" read -r line feature cell state; do
        option=$(jq -r --arg s "$state" '.options[] | select((.name | ascii_downcase) == ($s | ascii_downcase)) | .name' <<<"$field")
        [ -n "$option" ] || { echo "the issue field phase has no option for $state, the state of $feature" >&2; exit 1; }
        body=$(printf '**State** %s · **Release** %s · written from the release plan in docs/releases on every push\n' "$state" "$version"
            for phase in Discovery Formulation Automation Verification; do
                for file in "$dir"/*"$(tr '[:upper:]' '[:lower:]' <<<"$phase")".md; do [ -f "$file" ] && section "$feature" "$phase" "$file"; done
            done)
        url=$(sed -nE 's/.*\((https:[^)]*)\).*/\1/p' <<<"$cell")
        if [ -z "$url" ]; then
            url=$(gh issue list --repo "$repo" --state all --search "in:title \"$feature\"" --json title,url \
                --jq ".[] | select(.title == \"$feature\") | .url" | head -1)
            [ -n "$url" ] || url=$(gh issue create --repo "$repo" --title "$feature" --body "" --milestone "$version")
            awk -v old="$line" -v new="${line/| $feature | |/| $feature | [#${url##*/}]($url) |}" '$0 == old {$0 = new} 1' \
                "$overview" >"$overview.new" && mv "$overview.new" "$overview"
        fi
        gh issue edit "$url" --milestone "$version" --body-file - <<<"$body" >/dev/null
        # the phase is written when it changes, the old value removed first
        values="repos/$repo/issues/${url##*/}/issue-field-values"
        current=$(gh api "$values" --jq ".[] | select(.issue_field_id == $field_id) | .single_select_option.name")
        if [ "$current" != "$option" ]; then
            [ -z "$current" ] || gh api -X DELETE "$values/$field_id" --silent
            gh api "$values" --input - --silent <<<"{\"issue_field_values\": [{\"field_id\": $field_id, \"value\": \"$option\"}]}"
        fi
        echo "$version $state #${url##*/} $feature"
    done
done
