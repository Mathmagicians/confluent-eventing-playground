---
name: formulation
description: The Formulation phase of the spec-driven method. An agreed feature's examples become Gherkin scenarios and architecture rules, with step skeletons throwing PendingException, listed per feature in a release's formulation.md, and the gate Spec signed moves the feature on to Automation. Use when scenarios or architecture rules are written for an agreed feature, when step skeletons are generated, and before presenting a feature for Spec signed.
---

# Formulation

Formulation turns a feature's examples into executable spec: Gherkin scenarios and architecture rules. Its file is
the release folder's `*formulation.md`. The gate that closes it is **Spec signed**, one feature at a time. The
method, the folders, and the other phases are in `plan-and-supervise`.

## The spec

- Scenarios follow the README's rules under Testing, BDD with Cucumber: one capability per feature, in domain
  language; `Given` sets up state, `When` is one action, `Then` asserts an observable outcome; up to three `And`
  per keyword; `Scenario Outline` for variations of one behaviour. An existing step is found before a new one is
  written.
- An architecture rule is an ArchUnit test in an `*ArchitectureTest` class. It enforces an ADR, and the ADR's
  "Enforced by" names it.
- themathmagician writes the scenarios and the rules; Claude reviews them for language and structure and writes the
  step skeletons, each throwing `PendingException` (CLAUDE.md, Roles).
- A check played by hand is a scenario tagged `@manual` and gets no step skeletons; `@wip` and `@manual` are the
  README's, Testing. The repository's own features, the tooling's, live in `src/test/resources/features/` at the root.
- `*formulation.md` lists per feature the tasks: the feature file, the architecture rules, the skeletons, each with
  its owner and done-when. The scenarios live in the feature files, linked by file and line.

## Gate: Spec signed

The gate closes Formulation for one feature, in three steps: Claude runs the checks, presents the evidence in chat,
and themathmagician signs off.

| Check | How |
|---|---|
| Each example of the map is a scenario | the feature file read against the map |
| Each new step is defined, a skeleton throwing `PendingException` | `make bdd-snippets` finds every step defined |
| The new scenarios and architecture rules run red for the reason they name; the existing ones stay green | the output of `make bdd` and `make arch-verify`; a `@manual` scenario has no run |
| Each ADR the feature raised has its enforcing rule in the spec, or is marked review | the ADR's "Enforced by" in `docs/adr.md` |
| The features follow the README's BDD rules | review against Testing, BDD with Cucumber |

Evidence presented: the scenarios and rules, linked to file and line, and the red run's output. `*formulation.md`
records the evidence under the feature. On the yes, the feature's row records the date in its Spec signed cell,
linked to that evidence, and its State becomes Automation.
