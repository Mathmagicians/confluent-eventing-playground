---
theme: default
title: Architecture Discoverability and Verification
info: Faglig Fredag, 30 October 2026
layout: cover
---

# Architecture Discoverability and Verification

Agata Przybyszewska, PhD

Senior Enterprise Architect · DSV

Architect, DevOps engineer, Coder · MathMagicians

Faglig Fredag · 30 October 2026

---
layout: section
---

# Background

---

# Big enterprise, stale docs
What problem are we trying to solve?

Compliance, audit, governance.

Stop gates - you want it deployed? 

Then ... it goes stale.

The thing worse than no documentation is stale documentation.
---


# Tech stack
Why Java? Enterprise

*$\mu$-services*: Java 25, Spring Boot, etc ...

*Eventing:* Kafka, Confluent[^1] , Cloudflare, CLI

*Runtime:* Java 25, Docker + Docker Compose to run it

*Testing:* TDD, BDD with Cucumber

*Architecture discovery:* JMolecules, PlantUML +  our PlantUML plugin

*IaC:* Terraform, Terraform Cloud

*SDLC:* GitHub Actions, Makefile, Gradle[^2] 

[^1]: expensive

[^2]: ... and the slides you see run from a cosy Docker container with Slidev

---

# Meet Wonderland, a toy platform

Built to explore the core ideas

Platform Engineering is a skill

DX, make it easy - red carpet to do the right thing

Keep it simple, hide aways complexity and deep tech stacks

---

# Spec driven

Discovery → Formulation → Automation → Verification

Each release ends in production

---
layout: statement
---

# Must architecture docs always go stale?

---

# Three questions

How do we discover it?

How do we keep it in sync as life goes on?

How do we specify it?

---

# Wonderland plays stories on Kafka

---
layout: section
---

# 1 · Declare it in the code

---

# The hexagon

---

# The annotation is the spec

---
layout: section
---

# 2 · Verify it

---

# Two rules, plain unit tests

---

# Live: break the hexagon

---
layout: section
---

# 3 · Clients get the checks for free

---

# The purse extends the rules

---

# The purse reuses the steps

---
layout: section
---

# 4 · One model, three levels

---

# Code in, model out

---

# Three levels

---

# The model

---

# Facts found, views chosen

---

# Level 1 · The forest

---

# Level 2 · The trees

---

# Level 3 · The hexagon

---

# Where the facts live

---

# Same stories, three transports

---
layout: section
---

# 5 · The agent round trip

---

# What the agent gets

---

# The skill: generate a platform client

---

# Live: an agent builds a new purse

---

# Gates the agent can’t argue with

---

# Back in the model

---

# Three answers

---
layout: center
---

# Thanks! Questions?
