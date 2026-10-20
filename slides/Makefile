# The talk, Slidev over slides.md, played by the slides service of the repository's compose.yaml from the image of
# the Dockerfile here. The repository's Makefile passes COMPOSE, its compose run from the repository root.

# where the talk plays, the port of the slides service
TALK := http://localhost:3030
# a variable the repository's Makefile passes, checked by the recipe that needs it
need = $(if $($1),,$(error $1 is passed by the repository's Makefile: run make slides-present or make slides-pdf there))

.DEFAULT_GOAL := help
.PHONY: help present pdf

help:      ## this list
	@sed -nE 's/^([a-z_-]+):.*## *(.*)/  \1\t\2/p' $(lastword $(MAKEFILE_LIST)) | expand -t 16

# the browser opens once the talk answers, within two minutes
present:   ## live reload and presenter notes, on localhost:3030, opened in the browser
	@$(call need,COMPOSE)$(COMPOSE) build slides
	@(for i in $$(seq 120); do curl -sfo /dev/null $(TALK) && exec open $(TALK); sleep 1; done) &
	@$(COMPOSE) run --rm --service-ports slides

pdf:   ## slides.md as the PDF the repository commits, under docs/generated/slides
	@$(call need,COMPOSE)$(COMPOSE) run --rm --build slides export slides.md --output /pdf/architecture-discoverability.pdf
