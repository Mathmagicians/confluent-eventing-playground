---
name: automation
description: The Automation phase of the spec-driven method. Step bodies, drivers, unit tests, production code, and plumbing turn a signed feature's spec green, listed as tasks per feature in a release's automation.md, and the gate Spec green, with ADR compliance, moves the feature on to Verification. Use when planning or doing a feature's implementation tasks and before presenting a feature for Spec green.
---

# Automation

Automation makes a signed feature's spec pass. Its file is the release folder's `*automation.md`. The gate that
closes it is **Spec green**, one feature at a time. The method, the folders, and the other phases are in
`plan-and-supervise`.

## The tasks

- A task names its owner, themathmagician or `pair`, and the scenario or rule it turns green.
- Owners follow CLAUDE.md, Roles: themathmagician writes the step bodies, drivers, unit tests, and production code;
  `pair` writes the plumbing: build, make, compose, workflows, test infrastructure.
- Each change starts on a go and goes in as one Edit at a time (CLAUDE.md, Pairing protocol). A new class comes with
  its `<Class>Test`.
- Something external, a dependency, an image, a plugin, an Action, enters on a go on its name and origin.
- `*automation.md` carries a task's core snippets while the task is open. An implemented task shrinks to pointers:
  the files and tests that carry it.

## Gate: Spec green

The gate closes Automation for one feature, in three steps: Claude runs the checks, presents the evidence in chat,
and themathmagician signs off.

| Check | How |
|---|---|
| The feature's scenarios pass, and its `@manual` checks are played | the output of `make bdd`; for `@manual`, what themathmagician saw, with its date |
| The feature's architecture rules and unit tests pass | the output of `make build` and `make arch-verify` |
| Each accepted ADR the feature touches is complied with | its enforcing rule passes, or review cites `file:line`; the row in `*verification.md`'s ADR compliance table |
| The build is clean | `make build` green: zero warnings under `-Xlint:all -Werror`, `spotlessCheck` |

Evidence presented: the green run's output, the `@manual` checks, and the ADR compliance rows. `*automation.md`
records the evidence under the feature. On the yes, the feature's row records the date in its Spec green cell,
linked to that evidence, and its State becomes Verification.
