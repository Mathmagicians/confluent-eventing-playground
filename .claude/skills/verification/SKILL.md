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

Before the pull request, the sequence `cicd.yaml` plays, on the developer machine: `make check` on each transport
`make ci-transports` lists, `TRANSPORT=<transport>`, which is `generated-check`, `arch-verify`, `build`,
`docker-image`, `bdd`, the order of the `ci` and `bdd` jobs. A red step is the finding, and the pull request waits.

On the pull request:

| Check | How |
|---|---|
| Each feature the release ships is Spec green; the others moved to the next release's catalog | the overview's catalog |
| `make check` green on each transport CI plays | the output of each run, the last lines into the file |
| `ci`, `bdd (<transport>)`, `cd`, `iac`, `architecture-discovery`, `cicd-client-example` green on the PR | `gh pr checks`, each run by its id into the file |
| Review findings above Nit resolved, or deferred with a reason | the review, CLAUDE.md, Review protocol; a deferred finding names the release and the feature that takes it |
| README, `make help`, and `docs/adr.md` say what the code does | each read against the diff |
| Each accepted ADR the release touches has its row, with evidence | the ADR compliance table |

After the merge:

| Check | How |
|---|---|
| `v<version>` is on the merge commit, and the registry holds the image under the version and `latest` | the `tag` job's summary of the run on `main`; `gh api repos/<repo>/tags` |
| Wonderland plays in each of its environments, `test` and `prod` | `test`: the `bdd (<transport>)` deployment of the run on `main`; `prod`: a `load-run.yaml` dispatch from `main`; `gh api repos/<repo>/deployments` lists both |

Evidence presented: the runs by id, the PR's checks, the `tag` job's summary, and each environment's deployment.
`*verification.md` records each check with its evidence and date. On the yes, the release is in production, and the
bookkeeping below closes it.

## The bookkeeping, on the yes

In this order, each change seen by themathmagician, who stages and commits; the next release's first pull request
carries it all.

1. The overview: every row Spec green reads Archive, its gate cells filled and linked; the header names the merged
   pull request and the run on `main`, and the badge goes, since the merge deletes the branch. A row not Spec green
   moves to the next release's catalog first, with its sections of the phase files.
2. The phase files: each task to pointers, each `Open` line closed with its date or handed to the release that takes
   it, named `<version>, <feature>`. A link into another release's folder is a link only when that folder is
   `archived/`; a live release is named.
3. The folders: `this-pr/` moves to `archived/release-<version>/`, the next release's folder into `this-pr/`, plain
   `mv`; its overview names its branch and carries the branch's `cicd` badge. The branch starts from `origin/main`,
   since the merge is a squash and the old branch holds nothing `main` lacks:
   `git fetch origin && git switch -c <branch> origin/main`.
4. The links from the new `this-pr/` into the archive follow the move, `../archived/release-<version>/`.
5. The board: `make gh-project-sync` on the new branch moves the archived rows' cards to `archive`, closes their
   milestone, and writes their bodies with links into `main`, alive once this pull request merges; the live rows'
   bodies link into the branch. An issue is closed by `closes #N` in a commit that reaches `main`.
