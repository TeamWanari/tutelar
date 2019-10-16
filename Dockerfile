FROM openjdk:8-alpine
USER root
RUN apk add --no-cache bash && \
    adduser -S -u 1001 tutelar
USER 1001
EXPOSE 9000
ENTRYPOINT ["/app/bin/main"]
CMD []
# Files from Travis build
COPY --chown=1001:root . /app
RUN ls /app
