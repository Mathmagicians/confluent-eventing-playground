# Verification

The release gate, **In production**: every feature the [catalog](release-spec-0.0.6.md) ships Spec green, the release
checks passed, and Wonderland playing in each of its environments.

## Release checks

| Check | Evidence | Signed |
|---|---|---|
| `make check` green | | |
| `ci`, `cd`, `iac`, `architecture-discovery`, `cicd-client-example` green on the PR | | |
| Review findings above Nit resolved, or deferred with a reason | | |
| README, `make help`, and `docs/adr.md` say what the code does | | |

## ADR compliance

| ADR | Checked by | Evidence |
|---|---|---|
| The board writes a feature's phase to the catalog through the issue field Phase | the `@manual` scenarios of the_catalog_follows_the_board.feature | |
| A release ships the features that are Spec green | the `@manual` scenarios of a_release_ships_what_is_ready.feature | |
| CI plays each transport in a matrix | review of `cicd.yaml` and `cicd-client-example.yaml` | |

## In production

| Environment | Tag | Image | Wonderland plays | Signed |
|---|---|---|---|---|
| `test` | | | | |
| `prod` | | | | |
