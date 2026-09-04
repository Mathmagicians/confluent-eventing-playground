GRADLE := ./gradlew
IMAGE := confluent-eventing-playground
# same as version in build.gradle
VERSION := 0.0.1-SNAPSHOT
# publish needs REGISTRY, e.g. ghcr.io/<owner>; TAGS is a space separated list
REGISTRY ?=
TAGS ?= $(VERSION)
REPO := $(shell echo $(REGISTRY)/$(IMAGE) | tr A-Z a-z)

.PHONY: help build test bdd bdd-snippets check clean image publish up up-product up-offer up-order down proto-gen proto-check
.DEFAULT_GOAL := build

help:      ## this list
	@grep -hE '^[a-z-]+:.*##' $(MAKEFILE_LIST) | sed -E 's/:.*##[[:space:]]*/\t/' | sort | column -t -s "$$(printf '\t')"

build: proto-check     ## generated code, compile, unit tests, jar
	$(GRADLE) build

test:      ## unit tests
	$(GRADLE) test

bdd:       ## integration tests: Cucumber features with Testcontainers, against the image from make image
	$(GRADLE) bdd

bdd-snippets:  ## step-definition snippets for undefined steps, no execution
	$(GRADLE) bdd -PdryRun || true

check: proto-check build image bdd   ## the CI gate, in this order

clean:     ## remove build output
	$(GRADLE) clean

image:     ## container image confluent-eventing-playground:0.0.1-SNAPSHOT, used by compose.yaml and the integration tests
	$(GRADLE) bootBuildImage

# Generator settings are environment variables, defaults in compose.yaml:
#   OFFER_CONCURRENT=50 OFFER_INTERVAL=100 make up-offer
#   REGION=APAC TTL=300 make up
up:        ## the swarm: all generators
	docker compose up

up-product:   ## one generator
	docker compose up product-generator

up-offer:     ## one generator
	docker compose up offer-generator

up-order:     ## one generator
	docker compose up order-generator

down:      ## stop the swarm
	docker compose down

publish:   ## push $(IMAGE):$(VERSION) to $(REGISTRY) under each tag in $(TAGS)
	@test -n "$(REGISTRY)" || { echo "REGISTRY is required, e.g. make publish REGISTRY=ghcr.io/<owner>"; exit 1; }
	for tag in $(TAGS); do docker tag $(IMAGE):$(VERSION) $(REPO):$$tag && docker push $(REPO):$$tag; done

proto-gen:     ## generate proto files
	$(GRADLE) generateProto --no-configuration-cache

proto-check: proto-gen   ## generated code matches the schema and is staged
	@git status --porcelain -- src/generated | grep "^.[^ ]" && { echo "src/generated is out of date: make proto-gen, then git add src/generated"; exit 1; } || true
