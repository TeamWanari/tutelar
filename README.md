[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
[![Build Status](https://travis-ci.org/TeamWanari/tutelar.svg?branch=master)](https://travis-ci.org/TeamWanari/tutelar)

### build/run/test locally

`make up`: start the dependencies:

| Application   | PORT  |
| ------------- | -----:|
| ReverseProxy  |  9443 |
| ReverseProxy  |  8080 |
| Database      |  5432 |
| LDAP          |   389 |
| EmailService  |  9010 |
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

`sbt test it:test`: run all test

`sbt run`: start tutelar (port 9000)

[TEST PAGE](https://lvh.me:9443/index.html) `https://lvh.me:9443/index.html`

---

[JAEGER](http://jaeger.lvh.me:8080) `http://jaeger.lvh.me:8080`

[MAILHOG](http://mailhog.lvh.me:8080) `http://mailhog.lvh.me:8080`

[RABBITMQ](http://rabbitmq.lvh.me:8080) `http://rabbitmq.lvh.me:8080`

### for docs
```
make rundocs
```
