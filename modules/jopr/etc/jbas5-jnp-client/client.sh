#!/bin/sh
# -------------------------------------------------------------------------
# a script for a JBoss remote RMI client
# -------------------------------------------------------------------------

# $Id: client.sh 558 2009-05-05 03:52:49Z ips $


MAIN_JAR_NAME=jbas5-jnp-client-1.0.jar
MAIN_CLASS=test.RmiClient

JBOSS_HOME=/devel/jboss-5.1.0.CR1
JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8788"


# Find MAIN_JAR, or we can't continue

MAIN_JAR=target/$MAIN_JAR_NAME
if [  ! -f "$MAIN_JAR" ] 
then
  echo Could not locate $MAIN_JAR%. Please check that you are in the
  echo bin directory when running this script.
  exit 1
fi

if [ "`uname`" == "Darwin" ]
then
    JAVA_HOME=/usr
fi

if [ "$JAVA_HOME" == "" ]
then
  JAVA=java

  echo JAVA_HOME is not set.  Unexpected results may occur.
  echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
fi


JAVA=$JAVA_HOME/bin/java


# only include jbossall-client.jar in classpath, if
# JBOSS_CLASSPATH was not yet set
#if not "%JBOSS_CLASSPATH%" == "" GOTO HAVE_JB_CP
# set JBOSS_CLASSPATH=%JBOSS_HOME%/client/jbossall-client.jar

# For the call to new InitialContext() (using org.jnp.interfaces.NamingContextFactory)...
JBOSS_CLASSPATH=$JBOSS_CLASSPATH:$JBOSS_HOME/client/jnp-client.jar
JBOSS_CLASSPATH=$JBOSS_CLASSPATH:$JBOSS_HOME/common/lib/jboss-security-aspects.jar
JBOSS_CLASSPATH=$JBOSS_CLASSPATH:$JBOSS_HOME/client/jbosssx-client.jar
JBOSS_CLASSPATH=$JBOSS_CLASSPATH:$JBOSS_HOME/client/jboss-aop-client.jar
JBOSS_CLASSPATH=$JBOSS_CLASSPATH:$JBOSS_HOME/client/jboss-common-core.jar
# For the call to InitialContext.lookup()...
JBOSS_CLASSPATH=$JBOSS_CLASSPATH:$JBOSS_HOME/client/jboss-#oting.jar
JBOSS_CLASSPATH=$JBOSS_CLASSPATH:$JBOSS_HOME/client/jboss-aspect-jdk50-client.jar
JBOSS_CLASSPATH=$JBOSS_CLASSPATH:$JBOSS_HOME/client/trove.jar
JBOSS_CLASSPATH=$JBOSS_CLASSPATH:$JBOSS_HOME/client/javassist.jar
JBOSS_CLASSPATH=$JBOSS_CLASSPATH:$JBOSS_HOME/client/jboss-security-spi.jar
JBOSS_CLASSPATH=$JBOSS_CLASSPATH:$JBOSS_HOME/client/jboss-javaee.jar
# For remote invocations on the ProfileService proxy (e.g. ProfileService.getViewManager())...
JBOSS_CLASSPATH=$JBOSS_CLASSPATH:$JBOSS_HOME/client/concurrent.jar
JBOSS_CLASSPATH=$JBOSS_CLASSPATH:$JBOSS_HOME/client/jboss-client.jar
JBOSS_CLASSPATH=$JBOSS_CLASSPATH:$JBOSS_HOME/client/jboss-mdr.jar
JBOSS_CLASSPATH=$JBOSS_CLASSPATH:$JBOSS_HOME/client/jboss-integration.jar

JBOSS_CLASSPATH=$JBOSS_CLASSPATH:$JBOSS_HOME/lib/jboss-managed.jar
JBOSS_CLASSPATH=$JBOSS_CLASSPATH:$JBOSS_HOME/lib/jboss-metatype.jar

#:HAVE_JB_CP

JBOSS_CLASSPATH=$JBOSS_CLASSPATH:$MAIN_JAR

# Setup JBoss sepecific properties
JBOSS_ENDORSED_DIRS=$JBOSS_HOME/lib/endorsed

set -x
"$JAVA" $JAVA_OPTS "-Djava.endorsed.dirs=$JBOSS_ENDORSED_DIRS" -classpath "$JBOSS_CLASSPATH" $MAIN_CLASS $*

