---
name: discovery
description: The Discovery phase of the spec-driven method. Example mapping turns a feature into a story, rules, examples, and questions in a release's discovery.md, and the gate Examples agreed moves the feature on to Formulation. Use when a feature enters a release's catalog, when writing or updating discovery.md, and before presenting a feature for Examples agreed.
---

# Discovery

Discovery turns a feature into a story, rules, an example per rule, and the questions still open, by example
mapping (Matt Wynne, cucumber.io). Its file is the release folder's `*discovery.md`. The gate that closes it is
**Examples agreed**, one feature at a time. The method, the folders, and the other phases are in
`plan-and-supervise`.

## The example map

```markdown
## <the feature, as the catalog names it>

**Goal:** <themathmagician's words for what the feature delivers>

- <a fact behind the feature, with its link: file:line or the docs>

**Story:** I <do what>, <so that>.

### Rule: <one behaviour, stated as what is>
- <the command or the situation>: <what is observed>

**Questions**
1. <the question, its facts with links>: <option>, or <option>?

**Decisions for docs/adr.md, by title**
- <title>
```

- The feature, its goal, and its place in a release are themathmagician's words. Claude drafts the map from the
  conversation and the code; each rule and each answer is themathmagician's.
- A story is what someone wants, in their words: a developer, the platform, a reader of the architecture.
- A rule is one behaviour. Its examples are concrete and observable: a make target with its variables, what a
  command prints, what lands on a topic, what the log says.
- A question carries its facts with links and its options, one line each.
- The words follow the voice skill; the domain words are the README's.

## The facts behind the map

- Code is traced and cited as `file:line`, and docs are fetched; a claim from memory is checked before it is
  presented, see "Investigate before you claim" in `plan-and-supervise`.
- A question is presented in chat, one per round, with its facts and options. The answer goes into the map with
  its date, as a rule, an example, or an ADR.

## Gate: Examples agreed

The gate closes Discovery for one feature, in three steps: Claude runs the checks, presents the evidence in chat,
and themathmagician signs off.

| Check | How |
|---|---|
| Each rule has an example | the map lists one or more per rule |
| Each example is observable | it names a command, a message, a log line, or a topic |
| Each question the map raised is answered | the answer is in the map, with its date |
| Each architectural decision the map raised is accepted | `docs/adr.md` lists it as `ADR-NNN`, Status Accepted |
| The words are the team's | the map reads true against the voice skill and the README's domain words |

Evidence presented: the feature's rules with their examples, its answered questions, and its ADRs by number. On the
yes, the feature's row records the date in its Examples agreed cell, linked to its map, and its State becomes
Formulation.
