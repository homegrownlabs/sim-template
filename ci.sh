#!/usr/bin/env bash

set -x

rm -rf sample-sim
lein install

if lein new sim-test sample-sim; then
  echo "Project generated succesfully"
  exit 0
else
  echo "Project failed to generate"
  exit 1
fi

