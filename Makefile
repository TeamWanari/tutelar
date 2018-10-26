compose=docker-compose -f docker-compose.dev.yml -p tutelar

default: help

up: ## Spin up services
	$(compose) up -d && \
	sleep 1 && \
	$(compose) exec db bash -c "psql -U postgres -f /init/database.sql"

down: ## Destroy all services and volumes
	$(compose) down -v

psql: ## Open psql console
	$(compose) exec db bash -c "psql -U postgres"

sbt: ## Open sbt console
	$(compose) exec backend bash -c "sbt"

backend: ## Open backend console
	$(compose) exec backend bash

test: ## Run tests
	$(compose) exec backend bash -c "sbt test it:test"

compile: ## Compile
	$(compose) exec backend bash -c "sbt clean compile"

build: ## Build
	./scripts/build.sh

help: ## This help message
	@fgrep -h "##" $(MAKEFILE_LIST) | fgrep -v fgrep | sed -e 's/\\$$//' -e 's/:.*#/: #/' | column -t -s '##'

.PHONY: test