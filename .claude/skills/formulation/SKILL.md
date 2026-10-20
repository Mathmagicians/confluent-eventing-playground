---
name: formulation
description: The Formulation phase of the spec-driven method. An agreed rule's examples become Gherkin scenarios and architecture rules, with step skeletons throwing PendingException, listed per idea in a release's formulation.md, and the gate Spec signed moves the rule on to Automation. Use when scenarios or architecture rules are written for an agreed rule, when step skeletons are generated, and before presenting a rule for Spec signed.
---

# Formulation

Formulation turns a rule's examples into executable spec: Gherkin scenarios and architecture rules. Its file is
`formulation.md` in the release folder. The gate that closes it is **Spec signed**, one rule at a time. The method,
the folders, and the other phases are in `plan-and-supervise`.

## The spec

- Scenarios follow the README's rules under Testing, BDD with Cucumber: one capability per feature, in domain
  language; `Given` sets up state, `When` is one action, `Then` asserts an observable outcome; up to three `And`
  per keyword; `Scenario Outline` for variations of one behaviour. An existing step is found before a new one is
  written.
- An architecture rule is an ArchUnit test in an `*ArchitectureTest` class. It enforces an ADR, and the ADR's
  "Enforced by" names it.
- themathmagician writes the scenarios and the rules; Claude reviews them for language and structure and writes the
  step skeletons, each throwing `PendingException` (CLAUDE.md, Roles).
- `formulation.md` lists per idea the tasks: the feature file, the architecture rules, the skeletons, each with its
  owner and done-when. The scenarios live in the feature files, linked by file and line.

## Gate: Spec signed

The gate closes Formulation for one rule, in three steps: Claude runs the checks, presents the evidence in chat,
and themathmagician signs off.

| Check | How |
|---|---|
| Each example of the rule is a scenario | the feature file read against the map |
| Each new step is defined, a skeleton throwing `PendingException` | `make bdd-snippets` finds every step defined |
| The new scenarios and architecture rules run red for the reason they name; the existing ones stay green | the output of `make bdd` and `make arch-verify` |
| Each ADR the rule raised has its enforcing rule in the spec, or is marked review | the ADR's "Enforced by" in `docs/adr.md` |
| The features follow the README's BDD rules | review against Testing, BDD with Cucumber |

Evidence presented: the scenarios and rules, linked to file and line, and the red run's output. `formulation.md`
records the evidence under the idea. On the yes, the overview's Rules table records the date, linked to that
evidence, and the rule's phase becomes Automation.
