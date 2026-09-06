# confluent-eventing-playground
#
# The one entry point for humans and CI. Every workflow step is a target here, so what runs on GitHub is what
# runs on your machine. `make` builds, `make help` lists the targets by section.

GRADLE := ./gradlew
IMAGE := confluent-eventing-playground
# Gradle's version, derived from git tags. Evaluated once, on first use.
VERSION = $(eval VERSION := $(shell $(GRADLE) -q version))$(VERSION)
# the version a release gets, default Gradle's next. Evaluated once, on first use.
RELEASE = $(eval RELEASE := $(shell $(GRADLE) -q nextVersion))$(RELEASE)
# the GitHub repository, owner/name, from the origin remote. Evaluated once, on first use.
GITHUB_REPO = $(eval GITHUB_REPO := $(shell git remote -v 2>/dev/null | awk '/^origin.*\(fetch\)/ { print $$2 }' | sed -E 's|.*github.com[:/]||; s|\.git$$||'))$(GITHUB_REPO)
# registry for docker-publish, docker-run, docker-smoke, bdd-published and git-release: GHCR under the repository owner
REGISTRY ?= ghcr.io/$(firstword $(subst /, ,$(GITHUB_REPO)))
# GHCR requires a lowercase repository path
REPO = $(shell echo $(REGISTRY)/$(IMAGE) | tr A-Z a-z)
# tags pushed by docker-publish, a space separated list
TAGS ?= $(VERSION)
# registry image tag for docker-run, docker-smoke, bdd-published and git-release
TAG ?= latest
# the image under test in bdd: the local build, or a registry image through bdd-published
BDD_IMAGE ?= $(IMAGE):$(VERSION)
# the minimum load, one producer and about one event; everything else is the image's own defaults
MINIMUM := --load.concurrent=1 --load.interval=1000 --load.ttl=2
# Confluent credentials, passed through to bdd, docker-run and docker-smoke
CREDENTIALS := KAFKA_BOOTSTRAP_SERVERS KAFKA_API_KEY KAFKA_API_SECRET SCHEMA_REGISTRY_URL SCHEMA_REGISTRY_API_KEY SCHEMA_REGISTRY_API_SECRET
# locally they live in .env.<ENV>.private, sourced into the command's shell only, CI has them in the environment
ENV ?= test
ENV_FILE := .env.$(ENV).private
WITH_ENV := test ! -f $(ENV_FILE) || { set -a; . ./$(ENV_FILE); set +a; };

.DEFAULT_GOAL := build
.PHONY: help check build test run run-tiny clean version next-version proto-gen proto-check docker-image docker-repo docker-publish docker-image-exists docker-run docker-smoke bdd bdd-published bdd-snippets up up-product up-offer up-order down tf-init tf-check tf-plan git-tag git-release gh-main-protection

# sections are the ##@ lines, targets are the ## comments; the tab before each description is expanded to one column
help:      ## this list
	@printf '\n   (\\ /)\n   ( . .)   %s %s\n  c(")(")\n' "$(IMAGE)" "$(VERSION)"
	@sed -nE 's/^##@ (.*)/\n\1/p; s/^([a-z-]+):.*## *(.*)/  \1\t\2/p' $(MAKEFILE_LIST) | expand -t 24
	@echo

##@ Build, the CI gate, gradle powered
check: proto-check build docker-image bdd   ## generated code, build, image, integration tests, in this order

build: proto-check     ## compile, unit tests, jar
	$(GRADLE) build

test:      ## unit tests
	$(GRADLE) test

run:       ## one generator from source with profile ENV, default test; properties defaults, or ARGS="--load.type=order --load.ttl=10"; credentials from .env.<ENV>.private
	$(WITH_ENV) SPRING_PROFILES_ACTIVE=$(ENV) $(GRADLE) bootRun $(if $(ARGS),--args="$(ARGS)")

run-tiny:  ## the minimum load from source with the local profile: one producer, about one event, to the log
	$(MAKE) run ENV=local ARGS="$(MINIMUM)"

clean:     ## remove build output
	$(GRADLE) clean

version:   ## the version, from git tags: 1.2.3 at tag v1.2.3, 1.2.4-SNAPSHOT after it
	@echo $(VERSION)

next-version:   ## the next version, the SNAPSHOT without its suffix
	@echo $(RELEASE)

##@ Protobuf, generated code is committed under src/generated
proto-gen:     ## regenerate src/generated from src/main/proto
	$(GRADLE) generateProto --no-configuration-cache

proto-check: proto-gen   ## fail when src/generated is not regenerated and staged
	@git status --porcelain -- src/generated | grep "^.[^ ]" && { echo "src/generated is out of date: make proto-gen, then git add src/generated"; exit 1; } || true

##@ Docker, the image built by buildpacks, the registry, and running one generator from it
docker-image:   ## build the container image
	$(GRADLE) bootBuildImage

docker-repo:   ## the registry image path, REGISTRY/IMAGE in lowercase
	@echo $(REPO)

docker-publish:   ## push the image to the registry under each tag in TAGS
	for tag in $(TAGS); do docker tag $(IMAGE):$(VERSION) $(REPO):$$tag && docker push $(REPO):$$tag; done

docker-image-exists:   ## exit 0 when the registry has a latest image
	docker manifest inspect $(REPO):latest > /dev/null

docker-run:   ## one generator from the registry image TAG; image defaults, or ARGS="--load.type=order --load.ttl=60"; credentials from .env.<ENV>.private
	$(WITH_ENV) docker run --rm --pull always $(addprefix -e ,$(CREDENTIALS)) $(REPO):$(TAG) $(ARGS)

docker-smoke:   ## the minimum load from the registry image TAG, one producer and about one event
	$(MAKE) docker-run ARGS="$(MINIMUM)"

##@ Integration tests, Cucumber runs an image against the Confluent test cluster
bdd:       ## against the local image, or BDD_IMAGE=<reference>; credentials from .env.<ENV>.private
	$(WITH_ENV) $(GRADLE) bdd -Pimage=$(BDD_IMAGE)

bdd-published:   ## against the registry image TAG
	$(MAKE) bdd BDD_IMAGE=$(REPO):$(TAG)

bdd-snippets:  ## step-definition snippets for undefined steps, no execution
	$(GRADLE) bdd -PdryRun || true

##@ Docker compose controls the swarm of workers, all generators from compose.yaml; settings are environment variables, e.g. OFFER_CONCURRENT=50 TTL=300 make up-offer
up:        ## all generators
	VERSION=$(VERSION) docker compose up

up-product:   ## one generator
	VERSION=$(VERSION) docker compose up product-generator

up-offer:     ## one generator
	VERSION=$(VERSION) docker compose up offer-generator

up-order:     ## one generator
	VERSION=$(VERSION) docker compose up order-generator

down:      ## stop the swarm
	VERSION=$(VERSION) docker compose down

##@ Infrastructure, Terraform Cloud creates the topics on the Confluent cluster from iac/, applied on main
# the workspace: TF_CLOUD_ORGANIZATION TF_WORKSPACE; the cluster and its Kafka API key are Terraform variables of the workspace, see iac/variables.tf
# the token: `terraform login` once on a developer machine, TF_TOKEN_app_terraform_io in CI
TF := terraform -chdir=iac

tf-init:   ## download the provider and connect the workspace; settings from .env.<ENV>.private
	$(WITH_ENV) $(TF) init -input=false

tf-check: tf-init   ## formatting and validation of iac/
	$(TF) fmt -check -diff -recursive
	$(TF) validate

tf-plan: tf-init   ## what Terraform Cloud would apply, a speculative plan, no colour for logs; settings from .env.<ENV>.private
	$(WITH_ENV) $(TF) plan -input=false -no-color

##@ Repository, git tags and GitHub settings
git-tag:   ## git tag v<RELEASE> on HEAD and push it; RELEASE defaults to Gradle's next version
	git tag -a v$(RELEASE) -m "release $(RELEASE)" && git push origin v$(RELEASE)

git-release: git-tag   ## the tag, plus <RELEASE> on the candidate image TAG in the registry
	docker buildx imagetools create -t $(REPO):$(RELEASE) $(REPO):$(TAG)

gh-main-protection:   ## apply .github/branch-protection.json to main
	gh api --method PUT repos/$(GITHUB_REPO)/branches/main/protection --input .github/branch-protection.json
