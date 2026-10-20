# Verification · 0.0.6

The release gate, **In production**: every rule of the [catalog](release-spec-0.0.6.md) Spec green, the release checks
passed, and Wonderland playing in each of its environments.

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
| The transport is a profile beside the environment | the architecture rule of Formulation | |
| CI plays the spec on a local broker | review of `cicd.yaml` and `cicd-client-example.yaml` | |

## In production

| Environment | Tag | Image | Wonderland plays | Signed |
|---|---|---|---|---|
| `test` | | | | |
| `prod` | | | | |
