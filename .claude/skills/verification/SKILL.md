---
name: verification
description: The Verification phase of the spec-driven method. A release's verification.md holds the release checks on the pull request, the ADR compliance table, and the gate In production, Wonderland playing in each of its environments after the merge. Use when the features of a release are Spec green, when preparing its pull request, and after the merge to check Wonderland in production.
---

# Verification

Verification closes a release: the release checks on its pull request, then, after the merge, Wonderland in
production in each of its environments. Its file is the release folder's `*verification.md`. The gate that closes
it is **In production**, once per release. The method, the folders, and the other phases are in
`plan-and-supervise`.

## Gate: In production

The gate runs in three steps: Claude runs the checks, presents the evidence in chat, and themathmagician signs off.

On the pull request:

| Check | How |
|---|---|
| Each feature the release ships is Spec green; the others moved to the next release's catalog | the overview's catalog |
| `make check` green | the run's output |
| `ci`, `cd`, `iac`, `architecture-discovery`, `cicd-client-example` green on the PR | the PR's checks |
| Review findings above Nit resolved, or deferred with a reason | the review, CLAUDE.md, Review protocol |
| README, `make help`, and `docs/adr.md` say what the code does | each read against the diff |
| Each accepted ADR the release touches has its row, with evidence | the ADR compliance table |

After the merge:

| Check | How |
|---|---|
| `v<version>` is on the tested commit, and the registry holds the image under the version and `latest` | the summary of the `tag` job |
| Wonderland plays in each of its environments, `test` and `prod` | the run in each environment |

Evidence presented: the runs, the PR's checks, the `tag` job's summary, and each environment's run.
`*verification.md` records each check with its evidence and date. On the yes, the release is in production, and its
folder moves to `archived/release-<version>/` as `plan-and-supervise`, Where the plan lives, says.
