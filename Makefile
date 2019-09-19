compose=docker-compose -f docker-compose.dev.yml -p tutelar

default: help

up: ## Spin up services
	$(compose) up -d

stop: ## Stop services
	$(compose) stop

down: ## Destroy all services and volumes
	$(compose) down -v

psql: ## Open psql console
	$(compose) exec db bash -c "psql -U postgres"

rundocs: ## Start docs localhost:4000
	sbt "; project docs ; previewAuto"

help: ## This help message
	@fgrep -h "##" $(MAKEFILE_LIST) | fgrep -v fgrep | sed -e 's/\\$$//' -e 's/:.*#/: #/' | column -t -s '##'
