#!/bin/sh
# https://github.com/docker/for-linux/issues/264#issuecomment-543794125
ping -c1 -q host.docker.internal 2>&1 | grep "bad address" >/dev/null && echo "$(netstat -nr | grep '^0\.0\.0\.0' | awk '{print $2}') host.docker.internal" >> /etc/hosts && echo "Hosts File Entry Added for Linux!!!!!" || true
nginx -g "daemon off;"
