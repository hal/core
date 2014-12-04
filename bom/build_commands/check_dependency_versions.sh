#!/bin/sh

# verify if newer version for _relevant_ dependecies are available:

mvn versions:display-property-updates -DincludeProperties=ballroom.version,circuit.version,csp.plugin.version,jgrapht.version

