#!/bin/bash

set -e

cd "$(dirname "$0")"

mkdir -p bin

javac \
    -cp "lib/*" \
    -d bin \
    src/*.java

java \
    -cp "bin:lib/*" \
    Mytix