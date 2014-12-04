#!/bin/sh

# update _relevant_ depedncies to their most recent non-snapshot version

mvn versions:update-properties -DincludeProperties=ballroom.version,circuit.version,csp.plugin.version,jgrapht.version
