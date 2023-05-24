#!/bin/bash

# Build ucs

# Run mvn on ucs
# https://stackoverflow.com/questions/65092032/maven-build-failed-but-exit-code-is-still-0
echo "*** Building and packaging UCS modules ***"
# mvn clean org.jacoco:jacoco-maven-plugin:0.8.6:prepare-agent install org.jacoco:jacoco-maven-plugin:0.8.6:report | tee mvn_res
mvn clean package -DskipTests=true | tee mvn_res

grep "BUILD SUCCESS" mvn_res
EXIT_CODE=$?
rm mvn_res
exit $EXIT_CODE