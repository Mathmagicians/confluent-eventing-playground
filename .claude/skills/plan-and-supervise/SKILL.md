---
name: plan-and-supervise
description: Use when writing or updating a release plan in docs/releases, when asked to "assess and plan the work left", when a feature enters a release's catalog or moves between the phases Discovery, Formulation, Automation, and Verification, when a decision is settled with themathmagician round by round, or when supervising subagents that implement a release's tasks. Holds the spec-driven method, where plans live, how the plan stays current, how decisions and sign-offs run in chat, and how a subagent is briefed and checked.
---

# Plan and Supervise

themathmagician is the architect of record. Plans are read line by line, every decision is theirs, and a change
starts behind an explicit go. The plan in `docs/releases/` is the deliverable and the source-controlled state of the
project; the dialogue is how it gets right. CLAUDE.md holds the working agreement, the README what we build and how;
this skill holds how a release is planned and supervised.

## The method: spec driven, in Cucumber's BDD vocabulary

The method is the README's, How we work: Cucumber's three BDD practices (cucumber.io, Rose and Nagy's *The BDD
Books*) and a verification, over an executable spec, the Gherkin features and the architecture rules. A release
groups the features it releases, as many as themathmagician decides. Each phase has its skill and its file:

| Phase | Skill | File | Gate that closes it | Applies to |
|---|---|---|---|---|
| **Discovery** | `discovery` | `*discovery.md` | **Examples agreed** | one feature |
| **Formulation** | `formulation` | `*formulation.md` | **Spec signed** | one feature |
| **Automation** | `automation` | `*automation.md` | **Spec green** | one feature |
| **Verification** | `verification` | `*verification.md` | **In production** | the release |

- Each phase skill holds its quality checks. A gate runs in three steps: Claude runs the checks, presents the
  evidence in chat, and themathmagician signs off. A gate opens when every check passes; a failed check is presented
  as a finding.
- A feature moves through the phases on its own, so a release holds features in different phases. It ships its
  rows that are Spec green; the others move to the next release's catalog, and the sync moves their cards to that
  release's milestone.
- Every task carries its phase and its owner. Owners follow CLAUDE.md: themathmagician writes the features, step
  bodies, drivers, unit tests, and production code; Claude writes the plumbing and the step skeletons, and reviews.
  In the plan, Claude's tasks carry the owner `pair`.
- A sign-off is themathmagician's yes in chat on the presented evidence. The dates in a plan are facts: the day a
  gate was signed, the day a file was written or updated.
- Each gate's evidence lives in the phase file the gate closes; the feature's row in the overview records the
  gate's date and links to it.
- A task names the scenario or rule it turns green, linked to its file and line. The scenarios live in the feature
  files and the results in the reports, the branch's `cicd` badge, the `cucumber-report` artifact,
  `build/reports/cucumber/index.html` locally; the plan links to both.
- A decision that shapes the architecture is an ADR in `docs/adr.md`, Nygard's format: title, date, status, context,
  decision, consequences, and enforced by: the architecture rule or test that checks it, or review. The plan refers to
  it as `ADR-NNN [Title]`, see "ADR numbers are taken at write time".

## Where the plan lives

- `docs/releases/`, a folder per release, versions monotonic:

      release-<version>/              a planned release
      this-pr/                        the current release
      archived/release-<version>/     a release in production, trimmed to pointers

  Each folder holds the overview, headed `# Release <version>`, and one file per phase, `*discovery.md`,
  `*formulation.md`, `*automation.md`, `*verification.md`: `01_discovery.md` and on in `this-pr/`, `discovery.md`
  and on in a planned folder.
- The overview is the catalog, `| Feature | Issue | State | Examples agreed | Spec signed | Spec green | Map |`: each
  feature with its state, Discovery, Formulation, Automation, Verification, or Parked, and the date of each gate it
  passed, linked to its evidence; a feature's rules live in its map. Beside it the branch's `cicd` badge. Its
  header: **Version** | **Date** | **Updated** | **Branch**.
- The board, Mathmagicians project 5, follows the catalogs: `make gh-project-sync` gives each row its issue, linked
  in the Issue cell, its milestone the overview's version, its body the feature's sections of the phase files, and
  its issue field `phase` the row's state. themathmagician runs it after a gate is signed or a row moves to another
  release.
- A planned folder moves into `this-pr/` when its branch starts. Once the release is in production, `this-pr/` moves
  to `archived/release-<version>/`; `main` takes changes by pull request, so the move rides in the next release's
  first one. Plain `mv`, themathmagician stages. A feature the release does not ship moves to the next release's
  catalog first.
- A question in a map carries its facts and options, the drafted choice marked "as drawn"; its answer goes into the
  map with its date.
- References to an accepted ADR are `ADR-NNN [Title]`. A new ADR is proposed in the map by its title only; it gets
  its number when it is written into `docs/adr.md`.
- Prose in the plan follows the `voice` skill; questions are the one place alternatives are written down.

## Naming and placement

- A feature, a rule, a task, and a brief are named by what they are: "Messaging capability: cli, kafka, confluent",
  "ConfluentKeys, checked at startup". The release version is the one number in a name.
- Work goes where themathmagician puts it, in their words: a feature, its release, and its version come from them. A
  split, a new release, or a new feature is theirs to name; a question in a map is the place to raise one.

## Keeping the plan current

- A change updates every document it makes stale, in the same change: the plan, the skills, `make help`, and the
  README lines, written in word for word once themathmagician agrees them.
- An implemented task shrinks to pointers: the files, classes, and tests that carry it, and its gate date. Snippets
  and run output leave the plan; git holds them.
- Open work keeps its detail: the questions with their facts, the findings still standing, the notes a later task
  needs.
- Detail that belongs to another feature moves to that feature's section in full, and the first place points there.

## Investigate before you claim

- Trace the code path and cite `file:line` for every statement about behaviour. Check every caller before saying
  something is or is not handled (`grep -rn`).
- Answer the question that was asked. When asked whether the existing design covers something, explain what it
  does, with the README section or ADR that designed it, and whether a gap exists; new guards or semantics are
  options themathmagician asks for.
- A fact from the docs is fetched, a fact from the code is read, a fact from a run is shown. A claim from memory or
  an earlier session is checked before it is presented.

## Tests

The test layers and their rules are the README's, Testing: unit tests, BDD with Cucumber, load. A task names the
scenario or test that proves it, and a new class comes with its `<Class>Test`.

## Subagent brief, posted for approval before any agent starts

```
Task: <name, as automation.md names it>
Feature and rule: <links into its map and the overview>
Goal: <one sentence>
Files: create / modify / delete
Red first: <the scenario or test that fails before and passes after>
Questions: <none | list>
Agent: <agent type>
themathmagician's actions: <a secret to set, a command to run, none>
```

Wait for the yes. Launch one agent per task with the brief, the phase skill, and the README sections it touches.

- The brief covers the ask: the smallest change that does what themathmagician asked. A wider idea is one optional
  line after the brief.
- A rule outside the brief, an ADR line, or a test asserting a new behaviour stops the agent: it goes to
  themathmagician as a decision, and the closing report names it. `docs/adr.md` holds themathmagician's text,
  written after the yes on it.
- Source comments say what the code does, per the voice skill; dates, decisions, and sign-offs live in the plan.
- The report that closes a task assesses what was built against the brief and the spec, and names each deviation
  with the decision behind it.

## Supervising a task

1. The red run's output shown. 2. The green run's output shown. 3. `make build` green. 4. The diff reviewed against
the brief and the README standards, CLAUDE.md, Review protocol. 5. The plan updated: the task to pointers, the
evidence under its feature. 6. themathmagician stages and commits.

A finding in themathmagician's review reopens the same task, and is fixed and reported there.

## Hard rules

- CLAUDE.md, Security settings, holds for every agent: git is read-only, credentials stay with `make`, and
  themathmagician runs git writes. Agents prepare files and command lines.
- File changes go through Edit and Write; shell text tools read and search. Transient files go to the scratchpad.

## Sign-offs happen in chat

Anything that needs themathmagician's sign-off, a decision, a rule, a brief, a gate's evidence, is presented in the
conversation in full. The plan is the record.

## ADR numbers are taken at write time

A plan refers to a future ADR by its title. The number is the next free one in `docs/adr.md` at the moment the ADR
is written; numbers grow by one per decision.

## Settling a decision round by round

A clear instruction is acted on and reported. A round is for what themathmagician has not decided, settled in chat
one round at a time; the plan and the file follow the sign-off.

- **One item per round.** One decision per message; a decision the answer raises goes into the next round.
- **Re-present the full change every round**, as themathmagician will read it in the file: a change to a file as the
  block it becomes, a decision as `a) … b) …` with one line of pro and con each, the drafted choice marked "as
  drawn". A mid-round correction is applied to the block and the whole block shown again.
- **"I don't understand" asks for facts**: where the item comes from, why it matters, a concrete example, and a
  suggested solution. A comparison request gets the comparison, without steering. A decision themathmagician may
  have taken in another session or document is checked there first.
- **"Gibberish" falls back to the code.** The next answer quotes the class, the property, or the make target with
  `file:line`, names things by their names in the code, and drops paraphrase and metaphor. A briefing for a decision
  states the problem, the options, the effort, and the blast radius, in that order and compactly.
- **Sign-off semantics.** A briefing ends with the status in three labelled lists: **Running** (agents at work),
  **Next** (signed, waiting its turn), **Needs your decision** (briefed, unsigned). "y" signs the last presented
  block; "prepare", "go on", "continue" bring the next draft. After the yes: the plan, then the file, then a sweep of
  the documents for the words the round replaced.
- **Language.** themathmagician's words, precisely, and the voice skill's; terse chat without glyphs.
