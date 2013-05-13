#!/bin/sh

echo "Tmp dir:"
find ./target/i18n/ -name "*.properties" | xargs wc -l
echo "Source Files:"
find ./src/main/java/org/jboss/as/console/client/core/ -name "*.properties" | xargs wc -l
