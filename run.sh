#!/bin/bash
# Genera el jar (sense executar els tests) i l'executa.
#
# Ús:
#   ./run.sh          menú d'exemples
#   ./run.sh 4        executa directament l'exemple 4
#   ./run.sh tots     executa tots els exemples

set -e
cd "$(dirname "$0")"

JAR=target/dam-m0486-ra1-pr13-exemples.jar

echo "Generant $JAR..."
mvn -q -DskipTests package

java -jar "$JAR" "$@"
