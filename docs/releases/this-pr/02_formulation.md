# Formulation

Each feature's scenarios and architecture rules, by link, and the step skeletons, for the features of the
[catalog](release-spec-0.0.6.md) that passed Examples agreed. The evidence of Spec signed, the red run, is linked from
the feature's row in the catalog.

## Load on demand: the transport and its environment

| Task | Owner | Done when |
|---|---|---|
| The spec: the Git and CI bullet on `load-run.yaml` in the README, and `.github/environment-test.json` and `environment-prod.json`, applied by `make gh-main-protection` | pair | themathmagician signs it |
| The red run: a dispatch from `eventing-without-confluent` with `transport=kafka`, in the empty `kafka-test` | pair | the run shown, red at startup |

Spec signed 2026-10-21. The run,
[39508585975](https://github.com/Mathmagicians/wonderland-eventing-playground/actions/runs/39508585975), from
`eventing-without-confluent` with `transport=kafka`, deployed to `kafka-test` with `ENV=test` and skipped the load:
the registry holds no `latest` under the new image name until `main` builds it.

## Project sync: the board follows the catalog

Spec signed 2026-10-21. The spec is
[the_board_follows_the_catalog.feature](../../../src/test/resources/features/the_board_follows_the_catalog.feature),
`@manual`, checked on the runs of `make gh-project-sync`.

| Task | Owner | Done when |
|---|---|---|
| The map's scenarios in `the_board_follows_the_catalog.feature`, `@manual` | pair | each example of the map is a scenario |
| The first sync with the Issue cells empty | pair | the run shows the issues created and the links written into the rows |
| A gate signed and synced | pair | the card is in the row's new state |

## CI plays each transport

| Task | Owner | Done when |
|---|---|---|
| `@confluent` on the cluster's and the Flink table's features | pair | done 2026-10-22: `make bdd-snippets TRANSPORT=cli` lists none of them |
| The generator's, the tea party's, and the purse's scenarios play on cli as written, the drivers following the transport | pair | done 2026-10-22: `make bdd-snippets TRANSPORT=cli` lists them, every step defined |
