# Tutelar
A micro-service for authentication.

[![Build Status](https://travis-ci.org/TeamWanari/tutelar.svg?branch=master)](https://travis-ci.org/TeamWanari/tutelar)
![GitHub tag (latest SemVer pre-release)](https://img.shields.io/github/v/tag/TeamWanari/tutelar?include_prereleases)
[![Build Status](https://img.shields.io/badge/-Documatation-blue)](https://teamwanari.github.io/tutelar)

#### Supported authentications:
 - Username-Password
 - Email-Password
 - LDAP/AD
 - OAuth2 (Facebook, Google, GitHub)
 - Time-based One-time Password
 
#### Supported JWT formats:
 - Symmetric: HMD5, HS224, HS256, HS384, HS512
 - Asymmetric: RS256, RS384, RS512, ES256, ES384, ES512
 
#### Supported databases:
 - PostgreSQL
 - MongoDB
 - in memory (just for testing)
 
#### Supported logging:
 - Jaeger tracer
 - console/file human readable
 - console/file json (LogstashEncoder)

#### How to run
See [Docker images](https://hub.docker.com/r/teamwanari/tutelar/tags)

_TODO_


#### Contributing
See [developer home](DEVELOPER_HOME.md)

[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
[![Mergify Status](https://img.shields.io/endpoint.svg?url=https://gh.mergify.io/badges/TeamWanari/tutelar&style=flat)](https://mergify.io)
