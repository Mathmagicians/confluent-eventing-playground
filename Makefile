# confluent-eventing-playground: the eventing platform, the stories it plays, and the distribution that puts them
# in one image against Confluent Cloud. `make` builds, `make help` lists the targets. What every build on the
# platform shares is in platform.mk; this file adds what only this repository has.

IMAGE := confluent-eventing-playground
BOOT := :app
# the load story at its smallest: one producer, about one event, then exit
MINIMUM := --load.concurrent=1 --load.interval=1000 --load.ttl=2

# the generated code and documents are checked before anything builds
check: generated-check
build: generated-check

include platform.mk

DISCOVERY := architecture-discovery/gradlew -p architecture-discovery
# GitHub Packages needs a token even to read: CI's GITHUB_ACTOR and GITHUB_TOKEN, or gh's on a developer machine
WITH_GH := GITHUB_ACTOR=$${GITHUB_ACTOR:-$$(gh api user -q .login)} GITHUB_TOKEN=$${GITHUB_TOKEN:-$$(gh auth token)}
TF := terraform -chdir=iac
CONFLUENT_API := https://api.confluent.cloud

.PHONY: generated-check proto-gen proto-check run-tiny docker-smoke up-product up-offer up-order up-tea-party tf-init tf-check tf-format tf-plan tf-output confluent-lookup flink-statements flink-verify discovery-build discovery-install discovery-publish platform-install platform-publish git-tag git-release gh-main-protection

##@ Generated code and documents
generated-check: proto-check arch-check   ## fail when generated code or documents are stale

proto-gen:     ## regenerate platform/src/generated from src/main/proto
	@$(GRADLE) :platform:generateProto --no-configuration-cache

proto-check: proto-gen   ## fail when platform/src/generated is not regenerated and staged
	@git status --porcelain -- platform/src/generated | grep "^.[^ ]" && { echo "platform/src/generated is out of date: make proto-gen, then git add platform/src/generated"; exit 1; } || true

##@ Smoke, the load story at its smallest
run-tiny:  ## from source with the local profile, to the log
	@$(MAKE) run ENV=local ARGS="$(MINIMUM)"

docker-smoke:   ## from the registry image TAG against ENV
	@$(MAKE) docker-run ARGS="$(MINIMUM)"

##@ Flock, one player at a time: a generator or the tea party, against ENV; settings are environment variables, e.g. OFFER_CONCURRENT=50 TTL=300 make up-offer
up-product: docker-image   ## one generator
	@$(COMPOSE) up product-generator

up-offer: docker-image   ## one generator
	@$(COMPOSE) up offer-generator

up-order: docker-image   ## one generator
	@$(COMPOSE) up order-generator

up-tea-party: docker-image   ## the tea party of REGION, default EMEA, for TTL seconds; TTL=0 until stopped
	@$(COMPOSE) up tea-party

##@ Infrastructure, Terraform Cloud creates the topics, schemas, and Flink tables from iac/, applied on main
# the workspace: TF_CLOUD_ORGANIZATION TF_WORKSPACE; the cluster, the registry, and their keys are its variables;
# the token: `terraform login` once on a developer machine, TF_TOKEN_app_terraform_io in CI
tf-init:   ## download the provider and connect the workspace
	@$(WITH_ENV) $(TF) init -input=false

tf-check: tf-init   ## formatting and validation of iac/
	@$(TF) fmt -check -diff -recursive
	@$(TF) validate

tf-format: tf-init   ## format iac/ in place
	@$(TF) fmt -recursive

tf-plan: tf-init   ## the speculative plan, no colour for logs
	@$(WITH_ENV) $(TF) plan -input=false -no-color

tf-output: tf-init   ## the facts of the workspace; OUTPUT=<name> for one, as JSON
	@$(WITH_ENV) $(TF) output $(if $(OUTPUT),-json $(OUTPUT))

confluent-lookup:   ## the ids the workspace variables want, from the management API with the Cloud API key
	@$(WITH_ENV) lookup() { printf '\n== %s\n' "$$1"; curl -sS --fail-with-body -u "$$CLOUD_API_KEY:$$CLOUD_API_SECRET" "$(CONFLUENT_API)$$2" | jq -r "$$3"; }; \
	  lookup organizations "/org/v2/organizations" '.data[] | [.id, .display_name] | @tsv'; \
	  lookup environments "/org/v2/environments" '.data[] | [.id, .display_name] | @tsv'; \
	  lookup "clusters, the Flink database is the name" "/cmk/v2/clusters?environment=$${ENVIRONMENT_ID:?set ENVIRONMENT_ID in $(ENV_FILE)}" '.data[] | [.id, .spec.display_name, .spec.kafka_bootstrap_endpoint] | @tsv'; \
	  lookup "compute pools" "/fcpm/v2/compute-pools?environment=$$ENVIRONMENT_ID" '.data[] | [.id, .spec.display_name, .spec.cloud, .spec.region] | @tsv'; \
	  lookup users "/iam/v2/users" '.data[] | [.id, .email] | @tsv'; \
	  lookup "api keys, with their scope" "/iam/v2/api-keys" '.data[] | [.id, .spec.resource.kind, .spec.resource.id, .spec.owner.id] | @tsv'; \
	  lookup "flink regions" "/fcpm/v2/regions?cloud=GCP" '.data[] | [.id, .region_name, .http_endpoint] | @tsv'

##@ Flink, the experiment beside the stories: tables over the generator's topics, declared in iac/ with their SQL under src/main/flink, read here through the statements API with the Flink key
flink-statements: tf-init   ## the Flink statements of ENVIRONMENT_ID: name, phase, the reason when one failed
	@$(WITH_ENV) org=$$(curl -sS --fail-with-body -u "$$CLOUD_API_KEY:$$CLOUD_API_SECRET" $(CONFLUENT_API)/org/v2/organizations | jq -r '.data[0].id'); \
	  endpoint=$$($(TF) output -json compute_pool | jq -r .endpoint); \
	  curl -sS --fail-with-body -u "$$FLINK_API_KEY:$$FLINK_API_SECRET" "$$endpoint/sql/v1/organizations/$$org/environments/$${ENVIRONMENT_ID:?set ENVIRONMENT_ID in $(ENV_FILE)}/statements" \
	  | jq -r '.data[] | [.name, .status.phase, (.status.detail // "" | split("\n")[0])] | @tsv'

flink-verify: flink-statements   ## every statement's phase, then the tables our Terraform set up, from tf output flink
	@$(WITH_ENV) printf '\n== tables\n'; $(TF) output -json flink | jq -r '.[] | .[]'

##@ Libraries, published to GitHub Packages for the builds that resolve them
discovery-build:   ## architecture-discovery: compile, tests, jar
	@$(DISCOVERY) build

discovery-install: ## architecture-discovery into the local Maven repository, taken first
	@$(DISCOVERY) publishToMavenLocal

discovery-publish: ## architecture-discovery to GitHub Packages, where CI takes it from
	@$(WITH_GH) $(DISCOVERY) publish

# the publish tasks keep Gradle state the configuration cache cannot hold
platform-install:  ## the platform, its test fixtures, and the stories into the local Maven repository, where a story's build takes them from first
	@$(GRADLE) publishToMavenLocal --no-configuration-cache

platform-publish:  ## the platform, its test fixtures, and the stories to GitHub Packages, where a story's build takes them from
	@$(WITH_GH) $(GRADLE) publish --no-configuration-cache

##@ Repository, git tags and GitHub settings
git-tag:   ## tag v<RELEASE> on HEAD and push it; RELEASE defaults to the next version
	@git tag -a v$(RELEASE) -m "release $(RELEASE)" && git push origin v$(RELEASE)

git-release: git-tag   ## the tag, plus <RELEASE> on the candidate image TAG in the registry
	@docker buildx imagetools create -t $(REPO):$(RELEASE) $(REPO):$(TAG)

gh-main-protection:   ## apply .github/branch-protection.json to main
	@gh api --method PUT repos/$(GITHUB_REPO)/branches/main/protection --input .github/branch-protection.json
