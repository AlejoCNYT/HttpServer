#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
mvn -q clean package
java -cp target/classes com.mycompany.httpserver.HttpServer
