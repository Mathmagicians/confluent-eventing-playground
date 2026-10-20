---
name: automation
description: The Automation phase of the spec-driven method. Step bodies, drivers, unit tests, production code, and plumbing turn a signed rule's spec green, listed as tasks per idea in a release's automation.md, and the gate Spec green, with ADR compliance, moves the rule on to Verification. Use when planning or doing a rule's implementation tasks and before presenting a rule for Spec green.
---

# Automation

Automation makes a signed rule's spec pass. Its file is `automation.md` in the release folder. The gate that closes
it is **Spec green**, one rule at a time. The method, the folders, and the other phases are in `plan-and-supervise`.

## The tasks

- A task names its owner, themathmagician or `pair`, and the scenario or rule it turns green.
- Owners follow CLAUDE.md, Roles: themathmagician writes the step bodies, drivers, unit tests, and production code;
  `pair` writes the plumbing: build, make, compose, workflows, test infrastructure.
- Each change starts on a go and goes in as one Edit at a time (CLAUDE.md, Pairing protocol). A new class comes with
  its `<Class>Test`.
- Something external, a dependency, an image, a plugin, an Action, enters on a go on its name and origin.
- `automation.md` carries a task's core snippets while the task is open. An implemented task shrinks to pointers:
  the files and tests that carry it.

## Gate: Spec green

The gate closes Automation for one rule, in three steps: Claude runs the checks, presents the evidence in chat,
and themathmagician signs off.

| Check | How |
|---|---|
| The rule's scenarios pass | the output of `make bdd` |
| The rule's architecture rules and unit tests pass | the output of `make build` and `make arch-verify` |
| Each accepted ADR the rule touches is complied with | its enforcing rule passes, or review cites `file:line`; the row in `verification.md`'s ADR compliance table |
| The build is clean | `make build` green: zero warnings under `-Xlint:all -Werror`, `spotlessCheck` |

Evidence presented: the green run's output and the ADR compliance rows. `automation.md` records the evidence under
the idea. On the yes, the overview's Rules table records the date, linked to that evidence, and the rule's phase
becomes Verification.
