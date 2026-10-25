#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/test-deps build/tests
JSON_JAR="${JSON_JAR:-build/test-deps/json-20240303.jar}"
if [[ ! -f "$JSON_JAR" ]]; then curl --fail --silent --show-error --location 'https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar' --output "$JSON_JAR"; fi
printf '%s  %s\n' '3cf6cd6892e32e2b4c1c39e0f52f5248a2f5b37646fdfbb79a66b46b618414ed' "$JSON_JAR" | sha256sum --check --status
javac -cp "$JSON_JAR" -d build/tests app/src/main/java/dev/dothome/{GridPlanner,LayoutValidator,OfflineMigration}.java tests/{GridPlannerTest,LayoutValidatorTest,OfflineMigrationTest}.java
java -cp "build/tests:$JSON_JAR" GridPlannerTest
java -cp "build/tests:$JSON_JAR" LayoutValidatorTest
java -cp "build/tests:$JSON_JAR" OfflineMigrationTest
python3 tests/check_offline.py
