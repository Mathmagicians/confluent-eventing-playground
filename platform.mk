# The platform's targets, shared by every build that plays stories on it: this repository's, and a story's own.
# A story's build sets the variables, includes this file, and adds its targets below the include:
#
#   IMAGE := purse
#   PLATFORM_MK ?= ../../platform.mk
#   include $(PLATFORM_MK)
#
# Every target runs the Gradle wrapper of the directory it runs in, with platform.gradle applied, see there.

GRADLE := ./gradlew
# the image and container name
IMAGE ?= $(notdir $(CURDIR))
# the Boot project that builds the image: :app in this repository, the root in a story's build
BOOT ?=
# Gradle's version and the next one, from the root project, evaluated once, on first use
VERSION = $(eval VERSION := $(shell $(GRADLE) -q :version))$(VERSION)
RELEASE = $(eval RELEASE := $(shell $(GRADLE) -q :nextVersion))$(RELEASE)
# the GitHub repository, owner/name, from the origin remote; the registry, GHCR under the owner, lowercase
GITHUB_REPO = $(eval GITHUB_REPO := $(shell git remote -v 2>/dev/null | awk '/^origin.*\(fetch\)/ { print $$2 }' | sed -E 's|.*github.com[:/]||; s|\.git$$||'))$(GITHUB_REPO)
REGISTRY ?= ghcr.io/$(firstword $(subst /, ,$(GITHUB_REPO)))
REPO = $(shell echo $(REGISTRY)/$(IMAGE) | tr A-Z a-z)
# the tags docker-publish pushes; the registry tag docker-run, bdd-published and git-release take
TAGS ?= $(VERSION)
TAG ?= latest
# what the image reads, passed through to bdd and docker-run; locally from .env.<ENV>.private, sourced into the
# command's shell only, in CI from the environment
CREDENTIALS := KAFKA_BOOTSTRAP_SERVERS KAFKA_API_KEY KAFKA_API_SECRET SCHEMA_REGISTRY_REST_ENDPOINT SCHEMA_REGISTRY_API_KEY SCHEMA_REGISTRY_API_SECRET
ENV ?= test
# where .env.<ENV>.private lives: here, or the repository root for a story's build inside it
ENV_DIR ?= .
ENV_FILE := $(ENV_DIR)/.env.$(ENV).private
WITH_ENV := test ! -f $(ENV_FILE) || { set -a; . $(ENV_FILE); set +a; };
# the flock's environment: the profile and the credentials from .env.<ENV>.private
COMPOSE := $(WITH_ENV) ENV=$(ENV) VERSION=$(VERSION) docker compose

.DEFAULT_GOAL := build
.PHONY: help check build test run clean version next-version docker-image docker-repo docker-publish docker-image-exists docker-run bdd bdd-published bdd-snippets up down arch-verify arch-gen arch-check changed

# sections are the ##@ lines, targets the ## comments, this file's first
HELP_FILES := $(lastword $(MAKEFILE_LIST)) $(filter-out $(lastword $(MAKEFILE_LIST)),$(MAKEFILE_LIST))
help:      ## this list
	@printf '\n   (\\ /)\n   ( . .)   %s %s\n  c(")(")\n' "$(IMAGE)" "$(VERSION)"
	@sed -nE 's/^##@ (.*)/\n\1/p; s/^([a-z-]+):.*## *(.*)/  \1\t\2/p' $(HELP_FILES) | expand -t 24
	@echo

##@ Build, the CI gate
check: arch-verify build docker-image bdd   ## architecture, build, image, features, in this order

build:     ## compile, unit tests, jars
	@$(GRADLE) build

test:      ## unit tests
	@$(GRADLE) test

run:       ## from source with profile ENV, default test; ARGS pick the story and its settings, e.g. ARGS="--story=tea-party --tea-party.region=APAC"
	@$(WITH_ENV) SPRING_PROFILES_ACTIVE=$(ENV) $(GRADLE) $(BOOT):bootRun $(if $(ARGS),--args="$(ARGS)")

clean:     ## remove build output
	@$(GRADLE) clean

version:   ## from git tags: 1.2.3 at tag v1.2.3, 1.2.4-SNAPSHOT after it
	@echo $(VERSION)

next-version:   ## the SNAPSHOT without its suffix
	@echo $(RELEASE)

##@ Image, built by buildpacks, pushed to the registry, run from it
docker-image:   ## build IMAGE:VERSION; skipped while the jars and the version are unchanged
	@$(GRADLE) $(BOOT):bootBuildImage -Pimage=$(IMAGE):$(VERSION)

docker-repo:   ## the registry path, REGISTRY/IMAGE
	@echo $(REPO)

docker-publish: docker-image   ## push to the registry under each tag in TAGS
	@for tag in $(TAGS); do docker tag $(IMAGE):$(VERSION) $(REPO):$$tag && docker push $(REPO):$$tag; done

docker-image-exists:   ## exit 0 when the registry has a latest image
	@docker manifest inspect $(REPO):latest > /dev/null

docker-run:   ## the registry image TAG with profile ENV, default test; ARGS pick the story and its settings, e.g. ARGS="--story=tea-party --tea-party.region=APAC"
	@$(WITH_ENV) docker run --rm --pull always -e SPRING_PROFILES_ACTIVE=$(ENV) $(addprefix -e ,$(CREDENTIALS)) $(REPO):$(TAG) $(ARGS)

##@ Flock, docker compose plays the image from compose.yaml against ENV, default test; settings are environment variables, e.g. TTL=0 make up
up: docker-image   ## every player in compose.yaml, for TTL seconds, default 60; TTL=0 until stopped
	@$(COMPOSE) up

down:      ## stop the flock
	@$(COMPOSE) down

##@ Features, Cucumber runs the image against the Confluent test cluster: the platform's features and every story's
bdd: docker-image   ## against the local image, built first when stale
	@$(WITH_ENV) $(GRADLE) bdd -Pimage=$(IMAGE):$(VERSION)

bdd-published:   ## against the registry image TAG
	@$(WITH_ENV) $(GRADLE) bdd -Pimage=$(REPO):$(TAG)

bdd-snippets:  ## snippets for undefined steps, no execution
	@$(GRADLE) bdd -PdryRun || true

##@ Architecture, the hexagon from the annotations in the code, verified by tests and drawn under docs/generated/architecture
arch-verify:   ## the hexagonal rule and the module boundaries
	@$(GRADLE) $(BOOT):test --tests '*ArchitectureTest' --tests '*ArchitectureDocumentationTest'

arch-gen:      ## write docs/generated/architecture from the code
	@$(GRADLE) $(BOOT):renderDiagrams

# the plantuml sources are compared line-sorted, the Documenter writes dependency lines in varying order; the svg
# files follow, so they must be in git but are not compared
arch-check: arch-gen   ## fail when docs/generated/architecture is not regenerated and staged
	@git status --porcelain -- docs/generated/architecture | grep '^??' && { echo "docs/generated/architecture has files not in git: git add docs/generated/architecture"; exit 1; } || true
	@for f in $$(git status --porcelain -- docs/generated/architecture/plantuml | awk '/^.[^ ]/ { print $$2 }'); do \
	  [ "$$(git show ":$$f" | sort | cksum)" = "$$(sort "$$f" | cksum)" ] || { echo "$$f is out of date: make arch-gen, then git add docs/generated/architecture"; exit 1; }; \
	done

##@ Repository
changed:   ## true when a file under PATHS changed between commit BASE and HEAD, false when not; no or unknown BASE is true
	@if [ -z "$(BASE)" ] || ! git cat-file -e "$(BASE)" 2>/dev/null; then echo true; \
	elif git diff --quiet "$(BASE)" HEAD -- $(PATHS); then echo false; \
	else echo true; fi
