# The AS7 Web Management Interface

## Prerequisites

- JDK 7
- JBoss 8

In order to work on the console you a need running JBoss 
instance on your local host. You can download it here:

http://www.jboss.org/jbossas/downloads

You can run JBoss in either the 'domain' or 'standalone' mode.

## Running in hosted mode

1. Make sure JBoss 7 is started
2. Make sure you build the top level module first (mvn -Pdev clean install).
3. cd 'build/app'

Start the GWT shell with

	mvn gwt:<run|debug>

When the hosted browser is started, it's enough to hit the 'refresh' button to recompile
and verify changes. You can get the OOPHM Plugin, required for attaching your browser to the
hosted mode execution here: http://gwt.google.com/samples/MissingPlugin/MissingPlugin.html

NOTE: you need to add user with JBoss 8 add-user script.

## Running in web mode

	cd build/app
	mvn package

Produces a war file in target/*-resources.jar, which needs to be deployed as a JBoss Module.


## EAP Build Profile

To run a customised EAP build (L&F) follow these steps:

1.) Create a dedicated version number (i.e. 1.0.0.EAP.CR2)
2.) Rebuild with the EAP profile enabled:

	mvn -Peap clean install


## Development Profile

Due to the increased number of permutations (additional languages) the full compile times have increased quiet drastically. To work around this problem during development, we've added a development build profile that restricts the languages to english and the browser permutations to firefox. Follow steps to use 'dev' profile:
1. Build
	mvn -Pdev clean install
2. Run in shell
	cd 'build/app'
	mvn -Pdev gwt:run

## Bind Address

In some cases you may want to bind both the AS and the hosted mode to a specific address. A typical scenario is running a different OS (i.e windows) in a virtual machine. To make such a setup work you need to bind the hosted mode environment and the application server to a specific inet address that can be access from the virtual machine:

1.) start the AS on a specific address:

	./bin/standalone.sh -Djboss.bind.address=192.168.2.126 -Djboss.bind.address.management=192.168.2.126`

2.) launch hosted mode on a specific address:

	mvn clean -Dgwt.bindAddress=192.168.2.126 gwt:run

## Problems?

Please post any questions to the jboss as 7 mailing list:
jboss-as7-dev@lists.jboss.org

Have fun.
