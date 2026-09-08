---
name: architect-pair
description: The playbooks of the senior architect pairing with the human who writes the code. Use for every plumbing step, CI triage, a release, adding something external, and for the stack's lessons on GitHub Actions, Terraform with Confluent Cloud, Cucumber on the JUnit Platform, and Gradle with make. CLAUDE.md holds the agreement, the dry skill holds the homes, this skill holds how a step runs.
---

# Architect pair

`CLAUDE.md` says who owns what and how a review reads. `/dry` says where every fact lives. This skill says how a
step runs, and keeps what this stack taught us in `references/`.

## Off limits

`.env.*.private` is never read, opened, or sourced by an agent. Git is read-only for an agent: no `add`, `commit`,
`push`, `pull`, `checkout`, `rebase`, `tag`, `stash`, `config`, no branch changes. When something needs staging,
name the paths in one line and stop.

## A step

1. Intent becomes acceptance criteria in two sentences, checked against the README's architecture and roadmap.
2. A proposal: one recommendation and its reason, the files it touches, anything external named with its origin.
   A design statement from the human is discussion. The change starts after "go".
3. The change, one file at a time, through edits the human sees. Check `/dry` for the home before adding.
4. Verification by the command that proves the file, see below. A failure is reported with its output.
5. The report: outcome first, then what changed, then the plan for the next step, then stop. Findings outside the
   step are one line each, never silent fixes.

## Plumbing done

| Changed | Proven by |
|---|---|
| Makefile | `make <target>`, and `make help` reads right |
| `build.gradle` | `./gradlew build` |
| `iac/` | `make tf-check`, then `make tf-plan` |
| `compose.yaml` | `docker compose config` |
| a workflow | the push, then `gh run watch` |
| the BDD suite | `make bdd-snippets` without credentials, `make bdd` with them |

Docs move in the same change: the README line, the `##` comment that feeds `make help`, the sample env file's
names.

## CI triage

1. `gh run list --workflow <file> --branch main --limit 5` for the run.
2. `gh run view <id> --json jobs` for the job and the failed step.
3. The job log through `gh api repos/<owner>/<repo>/actions/jobs/<job id>/logs` when `--log-failed` comes back
   empty. Grep for `Error`, `Caused by`, `make: ***`.
4. Match against the known causes in `references/github-actions.md`. A transient cause is a `gh run rerun`.

## Release

Merge to `main` tags the next patch by itself. A minor or major version pauses `cicd.yaml` around the merge, then a
dispatch with the version; the exact sequence is in the README under Git and CI. Never a version injected into the
build, never a tag before the test, never a force push, never a deleted tag.

## Something external

A dependency, a plugin, a repository, a GitHub Action, a tool in a workflow: name it with its origin and its latest
version, `gh api repos/<action>/releases/latest` for an action, wait for the go, pin it with the reason and the
date, and state it in the message that adds it.

## Lessons this stack taught

- Workflows are thin `make` callers, one target per step, a secret or variable bound to an env var of the same name.
- Presentation lives in the workflow: job summaries, artifacts. The Makefile runs things.
- Gradle owns up-to-date checks; make expresses dependencies between targets.
- `make` targets, never raw `docker` or `terraform` by hand; a missing target is proposed, not worked around.
- The sample env file is the source of variable names; GitHub holds endpoints as variables and keys as secrets.
- Docs say what we do, once. Code is the documentation. No version-source remarks, no tool names in the team's
  files.
- One Terraform resource per concept, `for_each` over a map before a second copy of a block.
- A race between two workflows on one push is fixed by a rerun, not by coupling them.

## References

- `references/github-actions.md`: secrets and variables, summaries, artifacts, required checks, triage.
- `references/terraform-confluent.md`: the provider, the workspace, topics, schemas with references.
- `references/cucumber-junit-platform.md`: the runner, the reports, the Spring context, drivers, containers.
- `references/gradle-make.md`: the image build record, floating versions, the make conventions.
