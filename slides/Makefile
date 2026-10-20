# The talk, Slidev over slides.md. Node, npm, and the browser the export prints with live here; the repository's
# Makefile passes DIAGRAMS, the folder of the committed diagrams, and PDF, the file the export writes.

# the npm bin of this folder, never a download at run time
SLIDEV := npx --no-install slidev
# a variable the repository's Makefile passes, checked by the recipe that needs it
need = $(if $($1),,$(error $1 is passed by the repository's Makefile: run make slides or make slides-pdf there))

.DEFAULT_GOAL := help
.PHONY: help present pdf diagrams

help:      ## this list
	@sed -nE 's/^([a-z_-]+):.*## *(.*)/  \1\t\2/p' $(lastword $(MAKEFILE_LIST)) | expand -t 16

present: node_modules diagrams   ## live reload and presenter notes, in the browser
	@$(SLIDEV) slides.md --open

pdf: node_modules diagrams   ## slides.md as the PDF the repository commits
	@$(call need,PDF)mkdir -p $(dir $(PDF))
	@$(SLIDEV) export slides.md --output $(PDF)

# npm leaves install scripts off, so the browser the export prints with is fetched here, into the user's cache,
# outside the repository; a browser already there is kept
node_modules: package-lock.json   ## the packages of the lockfile and the export's browser, when the lockfile changes
	@npm ci --no-audit --no-fund
	@npx --no-install playwright install chromium
	@touch $@

diagrams:   ## the committed diagrams, copied fresh into public/generated, where slides.md finds them
	@$(call need,DIAGRAMS)rm -rf public/generated && mkdir -p public/generated && cp $(DIAGRAMS)/*.svg public/generated/
