#!/bin/bash

#
# Bruno Bonfils, <asyd@asyd.net>
# January 2007
# 
# Create a key via a netHSM device # 
# Example:
#

if [ -z "$EJBCA_HOME" ] ; then
	EJBCA_FILE="$0" 
	EJBCA_HOME=`echo $(dirname $(dirname $EJBCA_FILE))`
fi

JAVACMD=`which java`
# Check that JAVA_HOME is set
if [ ! -n "$JAVA_HOME" ]; then
    if [ ! -n "$JAVACMD" ]
    then
        echo "You must set JAVA_HOME before running the EJBCA cli."
        exit 1
    fi
else
    JAVACMD=$JAVA_HOME/bin/java
fi

if [ -z $NFAST_HOME ]; then
        echo "Warning: NFAST_HOME not set, using default to /opt/nfast"
        NFAST_HOME=/opt/nfast
fi

NFAST_JARS=$NFAST_HOME/java/classes

CLASSES=$EJBCA_HOME/lib/bcprov-jdk15.jar
CLASSES=$CLASSES:$EJBCA_HOME/lib/bcmail-jdk15.jar
CLASSES=$CLASSES:$EJBCA_HOME/lib/cert-cvc.jar
CLASSES=$CLASSES:$EJBCA_HOME/lib/jline-0.9.94.jar
CLASSES=$CLASSES:$EJBCA_HOME/lib/log4j.jar
CLASSES=$CLASSES:$EJBCA_HOME/tmp/bin/classes
# use this instead if you want build from eclipse
#CLASSES=$CLASSES:$EJBCA_HOME/out/classes

# Add nfast's JARs to classpath
for jar in rsaprivenc.jar nfjava.jar kmjava.jar kmcsp.jar jutils.jar
do
        CLASSES="$CLASSES:$NFAST_JARS/$jar"
done

# Finally run java
#set -x
$JAVACMD -cp $CLASSES org.ejbca.ui.cli.ClientToolBox NCipherHSMKeyTool ${@}
