#!/usr/bin/env bash

docker-compose -f docker-compose.build.yml -p tutelar_build build || stopAndExit
docker-compose -f docker-compose.build.yml -p tutelar_build up -d || stopAndExit
docker-compose -f docker-compose.build.yml -p tutelar_build exec backend sbt test it:test || stopAndExit
docker-compose -f docker-compose.build.yml -p tutelar_build exec backend cp -a /app/target/docker/stage/. /app/dist/ || stopAndExit
docker-compose -f docker-compose.build.yml -p tutelar_build down || stopAndExit
cd ./dist || stopAndExit
docker build --build-arg BUILD_VERSION=${1:-development} -t tutelar:${1:-latest} . || stopAndExit

stopAndExit () {
  docker-compose -f docker-compose.build.yml -p tutelar_build down
  exit 1
}