#!/bin/bash

set -e

cd "$(dirname "$0")"

mkdir -p bin

javac \
    -cp "lib/mysql-connector-java-8.0.29.jar" \
    -d bin \
    src/*.java

java \
    -cp "bin:lib/mysql-connector-java-8.0.29.jar" \
    Mytix