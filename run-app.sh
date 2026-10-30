#!/bin/sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$project_dir"
export MAVEN_USER_HOME="$project_dir/.mvn"
exec ./mvnw javafx:run
