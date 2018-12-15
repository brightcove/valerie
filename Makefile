.PHONY: help
help: ; @cat .make/help

.PHONY: check
check: ; @gradlew check

# You can set these variables from the command line.
SPHINXBUILD  := python -msphinx
SPHINXPROJ   := Valerie
DOCS_SRC     := docs/
DOCS_OUT     := ${DOCS_SRC}_build

.PHONY: docs

docs: Makefile ; @${SPHINXBUILD} -M html "${DOCS_SRC}" "${DOCS_OUT}"
