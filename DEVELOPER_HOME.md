## Developer home

### Requirements
- sbt
- docker
- docker-compose
- make (optional, but very useful)

### Quick start
1. `git clone git@github.com:TeamWanari/tutelar.git`
2. `cd tutelar`
3. `make up` (if you don't have make: `docker-compose -f docker-compose.dev.yml -p tutelar up -d`)
4. `sbt run`
5. open [TEST PAGE](https://lvh.me:9443/index.html) `https://lvh.me:9443/index.html`

### Setup OAuth2 providers (optional)
You have to add the providers configs to `.env` file (or create the `.env` file if not exists yet) 

_TODO: how to get GitHub/Google/Facebook/Microsoft OAuth2 credentials_

```
GITHUB_CLIENT_ID=REPLACE_IT
GITHUB_CLIENT_SECRET=REPLACE_IT
GOOGLE_CLIENT_ID=REPLACE_IT
GOOGLE_CLIENT_SECRET=REPLACE_IT
FACEBOOK_CLIENT_ID=REPLACE_IT
FACEBOOK_CLIENT_SECRET=REPLACE_IT
MICROSOFT_CLIENT_ID=REPLACE_IT
MICROSOFT_CLIENT_SECRET=REPLACE_IT
```
And add this line too:
```
MODULES_ENABLED=health,basic,email,ldap,totp,facebook,google,github,microsoft
```

### Basic sbt tasks
- `sbt run` start the application on port 9000
- `sbt test` run tests
- `sbt it:test` run integration tests
- `sbt fmt` format all files
- `sbt "; project docs ; previewAuto"` run documentation site on port 4000

### make tasks
- `make up` start dependency services
- `make stop` stop dependency services
- `make down` stop dependency services and delete all persisted data
- `make psql` open the postgres database console
- `make mongo` open the mongodb console
- `make rundocs` run documentation site on port 4000


### Service dependencies
This services starting when you hit `make up`
- postgres
- mongodb
- openldap
- rabbitmq
- mailhog (smtp mock server with ui)
- jaeger (tracer service for logs)
- nginx (reverse proxy for custom local domains + hosting the test client page)

##### Custom local domains
- [TEST PAGE](https://lvh.me:9443/index.html) `https://lvh.me:9443/index.html`
- [SMTP MOCK UI](http://mailhog.lvh.me:8080) `http://mailhog.lvh.me:8080`
- [TRACER](http://jaeger.lvh.me:8080) `http://jaeger.lvh.me:8080`
- [RABBITMQ](http://rabbitmq.lvh.me:8080) `http://rabbitmq.lvh.me:8080`

##### Used ports
| Application   | PORT  |
| ------------- | -----:|
| ReverseProxy  |  9443 |
| ReverseProxy  |  8080 |
| Database      |  5432 |
| LDAP          |   389 |
| SMTP mock UI  |  8025 |
| Jaeger UI     | 16686 |
| Jaeger        |  5775 |
| Jaeger        |  6831 |
| Jaeger        |  6832 |
| Jaeger        |  5778 |
| Jaeger        | 16686 |
| Jaeger        | 14268 |
| Jaeger        |  9411 |
| RabbitMQ UI   | 15672 |
| RabbitMQ      |  5672 |
