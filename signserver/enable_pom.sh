#!/bin/bash

FILES=modules/*
for f in $FILES
do
	echo "Processing $f"
	rm $f/nbproject.disabled -rf
	mv $f/nbproject $f/nbproject.disabled
done


### Install CESeCore jars not in an repository and without POM
mvn install:install-file -Dfile=lib/ext/cesecore-client-1.1.2.jar -DgroupId=se.primekey.signserver.labs.cesecoremvn -DartifactId=cesecore-client -Dversion=1.1.2 -Dpackaging=jar
mvn install:install-file -Dfile=lib/ext/cesecore-entity-1.1.2.jar -DgroupId=se.primekey.signserver.labs.cesecoremvn -DartifactId=cesecore-entity -Dversion=1.1.2 -Dpackaging=jar
