# GitHub Actions

## Shape

- A job is `env` bindings plus `run: make <target>` steps. Setup actions only where a version must be pinned.
- A secret binds to an env var of the same name: `KAFKA_API_KEY: ${{ secrets.KAFKA_API_KEY }}`. The one exception
  is a name a tool dictates, `TF_TOKEN_app_terraform_io: ${{ secrets.TF_API_TOKEN }}`, with a comment saying so.
- Endpoints are GitHub variables, `${{ vars.X }}`; keys are secrets, `${{ secrets.X }}`. A missing secret is an
  empty string, not an error: the symptom downstream was `Value not specified for key ';' in JAAS config`.
- `gradle/actions/setup-gradle` with `add-job-summary: never`, so the summary carries our own tables only.

## Summaries and artifacts

- The step after the make target writes the summary: a table with `printf`, a text file in a fenced block.
- A plan or a report into the summary: `make tf-plan | tee plan.txt && result=0 || result=$?`, append the file, then
  `exit $result`, with `shell: bash` for pipefail.
- Artifacts: `actions/upload-artifact`, `if: ${{ !cancelled() && steps.<id>.outcome != 'skipped' }}`, so a red
  suite still ships its report.
- A `run:` starting with `{` needs a block scalar `run: |`, YAML reads a brace as a mapping.

## Required checks

- A required check must be reported on every pull request. A workflow filtered by `paths` leaves the check pending
  forever. The pattern here: the job always runs, detects changes with `make changed`, and skips its own steps.
- The live protection can drift from `.github/branch-protection.json`; `make gh-main-protection` applies the file.
  A check named for a job that no longer exists blocks every merge.

## Triage

- `gh run list --workflow <file> --branch main --limit 5`, `gh run view <id> --json jobs`.
- `gh run view --log-failed` can come back empty; `gh api repos/<owner>/<repo>/actions/jobs/<job id>/logs` does not.
- Known causes: a secret read as a variable or the reverse; a stale check name in the branch protection; a floating
  snapshot re-published by a sibling workflow while the build resolved it, fixed by `gh run rerun`.
- Action versions: `gh api repos/<owner>/<action>/releases/latest --jq .tag_name`, compared with what is in use.

## Releases

- The merge tags the next patch. Minor or major: `gh workflow disable cicd.yaml`, merge, `gh workflow enable
  cicd.yaml`, `gh workflow run cicd.yaml --ref main -f version=<version>`.
- Badges: `actions/workflows/<file>/badge.svg?branch=main` for pull-request workflows, without the branch for a
  scheduled one.

## Git over HTTPS

- A push over about a megabyte can fail with `HTTP 400`, `curl 22`. `git config http.postBuffer 157286400` raises
  the request buffer for the repository; `git config http.version HTTP/1.1` is the fallback.
