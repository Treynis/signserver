#!/usr/bin/env bash

# OS specific support.
cygwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
esac

JAVACMD=`which java`
# Check that JAVA_HOME is set
if [ ! -n "$JAVA_HOME" ]; then
    if [ ! -n "$JAVACMD" ]
    then
        echo "You must set JAVA_HOME before running the EJBCA cli." 1>&2
        exit 1
    fi
else
    JAVACMD=$JAVA_HOME/bin/java
fi

if [ -z "$EJBCA_HOME" ] ; then
	EJBCA_FILE="$0" 
	EJBCA_HOME=`echo $(dirname $EJBCA_FILE)`
	cd $EJBCA_HOME
	cd ..
	EJBCA_HOME=`pwd`
fi

# Wich command are we running?
if [ "$1" = "batch" ] ; then 
	class_name=org.ejbca.ui.cli.batch.BatchMakeP12
elif [ "$1" = "ca" ] ; then
	class_name=org.ejbca.ui.cli.ca
elif [ "$1" = "ra" ] ; then
	class_name=org.ejbca.ui.cli.ra
elif [ "$1" = "setup" ] ; then
	class_name=org.ejbca.ui.cli.setup
elif [ "$1" = "hardtoken" ] ; then
	class_name=org.ejbca.ui.cli.hardtoken.hardtoken	
elif [ "$1" = "template" ] ; then
	JAR_DIR=${EJBCA_HOME}/lib/batik
	class_name=org.ejbca.ui.cli.SVGTemplatePrinter
elif [ "$1" = "asn1dump" ] ; then
	class_name=org.ejbca.ui.cli.Asn1Dump
elif [ "$1" = "encryptpwd" ] ; then
	class_name=org.ejbca.ui.cli.EncryptPwd
elif [ "$1" = "log" ] ; then
	class_name=org.ejbca.ui.cli.log
elif [ "$1" = "admins" ] ; then
	class_name=org.ejbca.ui.cli.admins
elif [ "$1" = "createcert" ] ; then
	class_name=org.ejbca.ui.cli.CreateCert
else
	echo "Usage: $0 [batch|ca|ra|setup|hardtoken|template|asn1dump|encryptpwd|log|admins|createcert] options"
	echo "For options information, specify a command directive"
	exit 1
fi

# discard $1 from the command line args
shift

if [ -f "$EJBCA_HOME/conf/ejbca.properties" ] ; then
    x=`cat "$EJBCA_HOME/conf/ejbca.properties" | grep appserver.home | grep -v "#appserver"`
    if [ -d "${x#*=}" ] ; then
	    APPSRV_HOME="${x#*=}"
    fi 
fi

# J2EE server classpath
if [ ! -n "$APPSRV_HOME" ]; then
    if [ -n "$JBOSS_HOME" ]; then
        APPSRV_HOME=$JBOSS_HOME
    fi
fi
if [ -n "$APPSRV_HOME" ]; then
    J2EE_DIR="${APPSRV_HOME}"/client
    if [ -r "$APPSRV_HOME"/server/lib/weblogic.jar ]; then
        echo "Using Weblogic JNDI provider..."  1>&2
        J2EE_DIR="${APPSRV_HOME}"/server/lib
    elif [ -r "$APPSRV_HOME"/lib/appserv-rt.jar ]; then
        echo "Using Glassfish JNDI provider..."  1>&2
        J2EE_DIR="${APPSRV_HOME}"/lib
    elif [ -r "$APPSRV_HOME"/j2ee/home/oc4jclient.jar ]; then
        echo "Using Oracle JNDI provider..."  1>&2
        J2EE_DIR="${APPSRV_HOME}"/j2ee/home
    elif [ -d "$APPSRV_HOME"/runtimes ]; then
        echo "Using Websphere JNDI provider..."  1>&2
        J2EE_DIR="${APPSRV_HOME}"/runtimes
    else 
        echo "Using JBoss JNDI provider..." 1>&2
    fi
else
    echo "Could not find a valid J2EE server for JNDI provider.." 1>&2
    echo "Configure appserver.home in ejbca.properties or specify a APPSRV_HOME environment variable" 1>&2
    exit 1
fi

# Check that classes exist
if [ ! -d ${EJBCA_HOME}/tmp/bin/classes ]
then    
        echo "You must build EJBCA before using the cli, use 'ant'." 1>&2
        exit 1
fi

# library classpath
CP="$EJBCA_HOME/tmp/bin/classes"$CP
if [ -d "$JAR_DIR" ] ; then
	for i in "${JAR_DIR}"/*.jar ; do
		CP="$i":"$CP"
done
fi
for i in "${J2EE_DIR}"/*.jar
do
	CP="$i":"$CP"
done
for i in "${EJBCA_HOME}"/lib/*.jar
do
	CP="$i":"$CP"
done

CP=$EJBCA_HOME/bin:$CP

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  CP=`cygpath --path --windows "$CP"`
fi
#exec "$JAVACMD" -Dlog4j.debug=1 -cp $CP $class_name "$@"
exec "$JAVACMD" -cp $CP $class_name "$@"

