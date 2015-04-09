#!/usr/bin/env bash

echo "Launching Sample Service..."
echo "Press <ctrl-c> to exit."
docker run --tty \
           --interactive \
           --rm \
           --publish 8080:8080 \
           --name "sample-service" \
           homegrownlabs/sample-service

