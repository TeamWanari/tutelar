FROM hseeberger/scala-sbt:8u212_1.2.8_2.13.0 as builder
WORKDIR /app
COPY build.sbt /app/build.sbt
COPY project /app/project
RUN sbt update
COPY . .
RUN sbt stage && \
    chmod -R u=rX,g=rX /app/target/universal/stage && \
    chmod u+x,g+x /app/target/universal/stage/bin/main


FROM openjdk:8-alpine
USER root
RUN apk add --no-cache bash && \
    adduser -S -u 1001 tutelar
USER 1001
EXPOSE 9000
ENTRYPOINT ["/app/bin/main"]
CMD []
COPY --from=builder --chown=1001:root /app/target/universal/stage /app
