#!/bin/bash

# Build ucs

# Run mvn on ucs

echo "*** Building and packaging UCS modules ***"

mvn clean verify | tee mvn_res

grep "BUILD SUCCESS" mvn_res
EXIT_CODE=$?
rm mvn_res
exit $EXIT_CODE