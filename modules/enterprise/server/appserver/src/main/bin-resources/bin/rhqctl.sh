#!/bin/sh

# =============================================================================
# RHQ Storage Installer Script
#
# This file is used to complete the installation of the RHQ storage Server on a
# UNIX platform.
#
# This script is customizable by setting the following environment variables:
#
#    RHQ_STORAGE_DEBUG - If this is defined, the script will emit debug
#                       messages. If this is not defined or set to "false"
#                       debug messages are not emitted.
#
#    RHQ_SERVER_HOME - Defines where the server's home install directory is.
#                      If not defined, it will be assumed to be the parent
#                      directory of the directory where this script lives.
#
#    RHQ_SERVER_JBOSS_HOME - The location of the AS instance that will
#                            host RHQ. If this is set, it overrides any
#                            JBOSS_HOME that might be set. If this not
#                            set, JBOSS_HOME is used as a fallback. If
#                            neither is set, it is assumed the AS bundled
#                            under RHQ_SERVER_HOME/jbossas is to be used.
#
#    RHQ_SERVER_JAVA_HOME - The location of the JRE that the server will
#                           use. This will be ignored if
#                           RHQ_SERVER_JAVA_EXE_FILE_PATH is set.
#                           If this and RHQ_SERVER_JAVA_EXE_FILE_PATH are
#                           not set, the Server's embedded JRE will be used.
#
#    RHQ_SERVER_JAVA_EXE_FILE_PATH - Defines the full path to the Java
#                                    executable to use. If this is set,
#                                    RHQ_SERVER_JAVA_HOME is ignored.
#                                    If this is not set, then
#                                    $RHQ_SERVER_JAVA_HOME/bin/java
#                                    is used. If this and
#                                    RHQ_SERVER_JAVA_HOME are not set, the
#                                    Server's embedded JRE will be used.
#
#    RHQ_STORAGE_INSTALLER_JAVA_OPTS - Java VM command line options to be
#                        passed into the Java VM. If this is not defined
#                        this script will pass in a default set of options.
#                        If you only want to add options to the defaults,
#                        then you will want to use
#                        RHQ_STORAGE_ADDITIONAL_JAVA_OPTS instead.
#
#    RHQ_STORAGE_INSTALLER_ADDITIONAL_JAVA_OPTS - additional Java VM command
#                                   line options to be passed into the VM.
#                                   This is added to RHQ_STORAGE_JAVA_OPTS; it
#                                   is mainly used to augment the
#                                   default set of options. This can be
#                                   left unset if it is not needed.
#
# If the embedded JRE is to be used but is not available, the fallback
# JRE to be used will be determined by the JAVA_HOME environment variable.
# =============================================================================

debug_msg ()
{
   # if debug variable is set, it is assumed to be on, unless its value is false
   if [ -n "$RHQ_STORAGE_DEBUG" ] && [ "$RHQ_STORAGE_DEBUG" != "false" ]; then
      echo $1
   fi
}

# ----------------------------------------------------------------------
# Determine what specific platform we are running on.
# Set some platform-specific variables.
# ----------------------------------------------------------------------

case "`uname`" in
   CYGWIN*) _CYGWIN=true
            ;;
   Linux*)  _LINUX=true
            ;;
   Darwin*) _DARWIN=true
            ;;
   SunOS*) _SOLARIS=true
            ;;
   AIX*)   _AIX=true
            ;;
esac

# only certain platforms support the -e argument for readlink
if [ -n "${_LINUX}${_SOLARIS}${_CYGWIN}" ]; then
   _READLINK_ARG="-e"
fi

# ----------------------------------------------------------------------
# Determine the RHQ Server installation directory.
# If RHQ_SERVER_HOME is not defined, we will assume we are running
# directly from the server installation's bin directory.
# ----------------------------------------------------------------------

if [ -z "$RHQ_SERVER_HOME" ]; then
   _DOLLARZERO=`readlink $_READLINK_ARG "$0" 2>/dev/null || echo "$0"`
   RHQ_SERVER_HOME=`dirname "$_DOLLARZERO"`/..
else
   if [ ! -d "$RHQ_SERVER_HOME" ]; then
      echo "ERROR! RHQ_SERVER_HOME is not pointing to a valid directory"
      echo "RHQ_SERVER_HOME: $RHQ_SERVER_HOME"
      exit 1
   fi
fi

cd "$RHQ_SERVER_HOME"
RHQ_SERVER_HOME=`pwd`

debug_msg "RHQ_SERVER_HOME: $RHQ_SERVER_HOME"

# ----------------------------------------------------------------------
# Determine what JBossAS instance to use.
# If RHQ_SERVER_JBOSS_HOME and JBOSS_HOME are both not defined, we will
# assume we are to run the embedded AS instance from the RHQ
# installation directory - RHQ_SERVER_HOME/jbossas
# ----------------------------------------------------------------------

if [ -z "$RHQ_SERVER_JBOSS_HOME" ]; then
   if [ -z "$JBOSS_HOME" ]; then
      RHQ_SERVER_JBOSS_HOME="${RHQ_SERVER_HOME}/jbossas"
   else
      if [ ! -d "$JBOSS_HOME" ]; then
         echo "ERROR! JBOSS_HOME is not pointing to a valid AS directory"
         echo "JBOSS_HOME: $JBOSS_HOME"
         exit 1
      fi
      RHQ_SERVER_JBOSS_HOME="${JBOSS_HOME}"
   fi
else
   if [ ! -d "$RHQ_SERVER_JBOSS_HOME" ]; then
      echo "ERROR! RHQ_SERVER_JBOSS_HOME is not pointing to a valid AS directory"
      echo "RHQ_SERVER_JBOSS_HOME: $RHQ_SERVER_JBOSS_HOME"
      exit 1
   fi
fi

cd "$RHQ_SERVER_JBOSS_HOME"
RHQ_SERVER_JBOSS_HOME=`pwd`

debug_msg "RHQ_SERVER_JBOSS_HOME: $RHQ_SERVER_JBOSS_HOME"

if [ ! -f "${RHQ_SERVER_JBOSS_HOME}/jboss-modules.jar" ]; then
   echo "ERROR! RHQ_SERVER_JBOSS_HOME is not pointing to a valid AS instance"
   echo "Missing ${RHQ_SERVER_JBOSS_HOME}/jboss-modules.jar"
   exit 1
fi

# we want the rest of this script to be able to assume cwd is the RHQ install dir
cd "$RHQ_SERVER_HOME"

# ----------------------------------------------------------------------
# if we are on a Mac and JAVA_HOME is not set, then set it to /usr
# as this is the default location.
# ----------------------------------------------------------------------

if [ -z "$JAVA_HOME" ]; then
   if [ -n "$_DARWIN" ]; then
     debug_msg "Running on Mac OS X, setting JAVA_HOME to /usr"
     JAVA_HOME=/usr
   fi
fi

# ----------------------------------------------------------------------
# Create the logs directory
# ----------------------------------------------------------------------

_LOG_DIR_PATH="${RHQ_SERVER_HOME}/logs"
if [ -n "$_CYGWIN" ]; then
   _LOG_DIR_PATH=`cygpath --windows --path "$_LOG_DIR_PATH"`
fi
if [ ! -d "${_LOG_DIR_PATH}" ]; then
   mkdir "${_LOG_DIR_PATH}"
fi

# ----------------------------------------------------------------------
# Find the Java executable and verify we have a VM available
# ----------------------------------------------------------------------

if [ -z "$RHQ_SERVER_JAVA_EXE_FILE_PATH" ]; then
   if [ -z "$RHQ_SERVER_JAVA_HOME" ]; then
      RHQ_SERVER_JAVA_HOME="${RHQ_SERVER_HOME}/jre"
      if [ -d "$RHQ_SERVER_JAVA_HOME" ]; then
         debug_msg "Using the embedded JRE"
      else
         debug_msg "No embedded JRE found - will try to use JAVA_HOME: $JAVA_HOME"
         RHQ_SERVER_JAVA_HOME="$JAVA_HOME"
      fi
   fi
   debug_msg "RHQ_SERVER_JAVA_HOME: $RHQ_SERVER_JAVA_HOME"
   RHQ_SERVER_JAVA_EXE_FILE_PATH="${RHQ_SERVER_JAVA_HOME}/bin/java"
fi
debug_msg "RHQ_SERVER_JAVA_EXE_FILE_PATH: $RHQ_SERVER_JAVA_EXE_FILE_PATH"

if [ ! -f "$RHQ_SERVER_JAVA_EXE_FILE_PATH" ]; then
   echo "There is no JVM available."
   echo "Please set RHQ_SERVER_JAVA_HOME or RHQ_SERVER_JAVA_EXE_FILE_PATH appropriately."
   exit 1
fi

# ----------------------------------------------------------------------
# Prepare the VM command line options to be passed in
# ----------------------------------------------------------------------

if [ -z "$RHQ_STORAGE_INSTALLER_JAVA_OPTS" ]; then
   RHQ_STORAGE_INSTALLER_JAVA_OPTS="-Xms512M -Xmx512M -XX:PermSize=128M -XX:MaxPermSize=128M -Djava.net.preferIPv4Stack=true -Dorg.jboss.resolver.warning=true"
fi

# Add the JVM opts that we always want to specify, whether or not the user set RHQ_CCM_JAVA_OPTS.
if [ -n "$RHQ_STORAGE_DEBUG" ] && [ "$RHQ_STORAGE_DEBUG" != "false" ]; then
   _RHQ_LOGLEVEL="DEBUG"
else
   _RHQ_LOGLEVEL="INFO"
fi

# debugging the logging level now for development/testing
RHQ_STORAGE_INSTALLER_JAVA_OPTS="${RHQ_STORAGE_INSTALLER_JAVA_OPTS} -Djava.awt.headless=true -Drhq.server.properties-file=${RHQ_SERVER_HOME}/bin/rhq-server.properties -Drhq.control.logdir=${RHQ_SERVER_HOME}/logs -Drhq.control.loglevel=${_RHQ_LOGLEVEL} -Drhq.server.basedir=${RHQ_SERVER_HOME}"

# Sample JPDA settings for remote socket debugging
#RHQ_STORAGE_INSTALLER_JAVA_OPTS="${RHQ_STORAGE_INSTALLER_JAVA_OPTS} -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=y"

debug_msg "RHQ_STORAGE_INSTALLER_JAVA_OPTS: $RHQ_STORAGE_INSTALLER_JAVA_OPTS"
debug_msg "RHQ_STORAGE_INSTALLER_ADDITIONAL_JAVA_OPTS: $RHQ_STORAGE_INSTALLER_ADDITIONAL_JAVA_OPTS"

# ----------------------------------------------------------------------
# We need to add our own modules to the core set of JBossAS modules.
# ----------------------------------------------------------------------
_RHQ_MODULES_PATH="${RHQ_SERVER_HOME}/modules"
_INTERNAL_MODULES_PATH="${RHQ_SERVER_JBOSS_HOME}/modules"
if [ -n "$_CYGWIN" ]; then
   _RHQ_MODULES_PATH=`cygpath --windows --path "$_RHQ_MODULES_PATH"`
   _INTERNAL_MODULES_PATH=`cygpath --windows --path "$_INTERNAL_MODULES_PATH"`
fi
_JBOSS_MODULEPATH="${_RHQ_MODULES_PATH}:${_INTERNAL_MODULES_PATH}"
debug_msg "_JBOSS_MODULEPATH: $_JBOSS_MODULEPATH"

echo "Starting RHQ Control..."

# start the AS instance with our main installer module
"$RHQ_SERVER_JAVA_EXE_FILE_PATH" ${RHQ_STORAGE_INSTALLER_JAVA_OPTS} ${RHQ_STORAGE_INSTALLER_ADDITIONAL_JAVA_OPTS} -jar "${RHQ_SERVER_JBOSS_HOME}/jboss-modules.jar" -mp "$_JBOSS_MODULEPATH" org.rhq.rhq-server-control "$@"

_EXIT_STATUS=$?
exit $_EXIT_STATUS
