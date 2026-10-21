# Verification

The release gate, **In production**: every feature the [catalog](release-0.0.8.md) ships Spec green, the release
checks passed, and Wonderland playing in each of its environments.

## Release checks

| Check | Evidence | Signed |
|---|---|---|
| `make check` green | | |
| the PR's checks green | | |
| Review findings above Nit resolved, or deferred with a reason | | |
| README, `make help`, and `docs/adr.md` say what the code does | | |

## ADR compliance

| ADR | Checked by | Evidence |
|---|---|---|

## In production

| Environment | Tag | Image | Wonderland plays | Signed |
|---|---|---|---|---|
| `test` | | | | |
| `prod` | | | | |
