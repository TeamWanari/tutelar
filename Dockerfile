FROM hseeberger/scala-sbt:11.0.3_1.2.8_2.13.0 as builder
COPY build.sbt /app/build.sbt
COPY project /app/project
WORKDIR /app
RUN sbt update test:update it:update
COPY . .
RUN sbt compile test stage


FROM openjdk:11
WORKDIR /app
COPY --from=builder /app/target/universal/stage /app
USER root
RUN useradd --system --create-home --uid 1001 --gid 0 tutelar && \
    chmod -R u=rX,g=rX /app && \
    chmod u+x,g+x /app/bin/tutelar && \
    chown -R 1001:root /app
USER 1001

EXPOSE 9000
ENTRYPOINT ["/app/bin/tutelar"]
CMD []
