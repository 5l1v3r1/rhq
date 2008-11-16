#!/bin/sh

# chkconfig: 2345 93 25
# description: Starts and stops the RHQ agent
#
# processname: java
# pidfile: /var/run/rhq-agent.pid

# =============================================================================
# RHQ Agent UNIX Boot Script
#
# This file is used to execute the RHQ Agent on a UNIX platform as part of
# the platform's bootup sequence.
# Run this script without any command line options for the syntax help.
#
# This script is customizable by setting the environment variables that
# are accepted by the rhq-agent.sh script - see that script for more info.
#
# Note that if this script is to be used as an init.d script, you must ensure
# this script has the RHQ_AGENT_HOME set so it knows where to find the
# agent installation.
#
# You may optionally set the RHQ_AGENT_PIDFILE_DIR environment variable if
# you want to write the pidfile to a specific location.  This value must
# be a full path to a directory with write permissions so the pidfile can
# be stored there.  If not set, its default is the agent's bin directory.
# =============================================================================

RHQ_AGENT_WRAPPER_BIN_DIR_PATH=`dirname $0`

# Read in the rhq-agent-env.sh file so we get the configured agent environment
if [ -f "${RHQ_AGENT_WRAPPER_BIN_DIR_PATH}/rhq-agent-env.sh" ]; then
   . ${RHQ_AGENT_WRAPPER_BIN_DIR_PATH}/rhq-agent-env.sh $*
fi

# Here are some environment variables you can set to customize the launch
# of the RHQ Agent. Consider setting them in the rhq-agent-env.sh before
# setting them here, it is probably better setting them in the env file instead.

# RHQ_AGENT_HOME=/path/to/agent/home
# RHQ_AGENT_PIDFILE_DIR=/var/run
# export RHQ_AGENT_DEBUG=true
# export RHQ_AGENT_JAVA_HOME=/path/to/java/installation
# export RHQ_AGENT_JAVA_EXE_FILE_PATH=/path/directly/to/java/executable
# export RHQ_AGENT_JAVA_OPTS=VM options
# export RHQ_AGENT_ADDITIONAL_JAVA_OPTS=additional VM options

# The --daemon argument is required, but you can add additional arguments as appropriate
if [ "x$RHQ_AGENT_CMDLINE_OPTS" = "x" ]; then
   export RHQ_AGENT_CMDLINE_OPTS=--daemon
fi

# Determine where this script is, and change to its directory
cd $RHQ_AGENT_WRAPPER_BIN_DIR_PATH
THIS_SCRIPT_DIR=`pwd`
THIS_SCRIPT=${THIS_SCRIPT_DIR}/`basename $0`

# Figure out where the RHQ Agent's home directory is and cd to it.
# If RHQ_AGENT_HOME is not defined, we will assume we are running
# directly from the agent installation's bin directory

if [ "x$RHQ_AGENT_HOME" = "x" ]; then
   RHQ_AGENT_START_SCRIPT_DIR=${THIS_SCRIPT_DIR}
   cd ..
   RHQ_AGENT_HOME=`pwd`
else
   RHQ_AGENT_START_SCRIPT_DIR=${RHQ_AGENT_HOME}/bin
   cd ${RHQ_AGENT_HOME} || {
      echo Cannot go to the RHQ_AGENT_HOME directory: ${RHQ_AGENT_HOME}
      exit 1
      }
fi

if [ "x$RHQ_AGENT_PIDFILE_DIR" = "x" ]; then
   RHQ_AGENT_PIDFILE_DIR=${RHQ_AGENT_HOME}/bin
fi
mkdir -p $RHQ_AGENT_PIDFILE_DIR

RHQ_AGENT_START_SCRIPT=${RHQ_AGENT_START_SCRIPT_DIR}/rhq-agent.sh

if [ ! -f $RHQ_AGENT_START_SCRIPT ]; then
   echo "ERROR! Cannot find the RHQ Agent start script"
   echo "Not found: $RHQ_AGENT_START_SCRIPT"
   exit 1
fi

PIDFILE=${RHQ_AGENT_PIDFILE_DIR}/rhq-agent.pid

# Sets STATUS, RUNNING and PID based on the status of the RHQ Agent
check_status ()
{
    if [ -f $PIDFILE ]; then
        PID=`cat $PIDFILE`
        if [ "x$PID" != "x" ] && kill -0 $PID 2>/dev/null ; then
            STATUS="RHQ Agent (pid $PID) is running"
            RUNNING=1
        else
            STATUS="RHQ Agent (pid $PID) is NOT running"
            RUNNING=0
        fi
    else
        STATUS="RHQ Agent (no pid file) is NOT running"
        RUNNING=0
    fi
}

# Ensures that the PID file no longer exists
remove_pid_file ()
{
   if [ -f $PIDFILE ]; then
      rm $PIDFILE
   fi
}

# Main processing starts here

check_status

case "$1" in
'start')
        if [ "$RUNNING" = "1" ]; then
           echo $STATUS
           exit 0
        fi

        echo Starting RHQ Agent...

        RHQ_AGENT_IN_BACKGROUND=$PIDFILE
        export RHQ_AGENT_IN_BACKGROUND

        if [ "x$RHQ_AGENT_DEBUG" = "x" ]; then
           $RHQ_AGENT_START_SCRIPT > /dev/null 2>&1
        else
           $RHQ_AGENT_START_SCRIPT
        fi

        sleep 5
        check_status
        echo $STATUS

        if [ "$RUNNING" = "1" ]; then
           exit 0
        else
           echo Failed to start - make sure the RHQ Agent is fully configured properly
           exit 1
        fi
        ;;

'stop')
        if [ "$RUNNING" = "0" ]; then
           echo $STATUS
           remove_pid_file
           exit 0
        fi

        echo Stopping RHQ Agent...

        # try to gracefully kill, but eventually beat it over the head
        echo "RHQ Agent (pid=${PID}) is stopping..."
        kill -INT $PID

        sleep 5
        check_status
        if [ "$RUNNING" = "1"  ]; then
           kill -TERM $PID
        fi

        while [ "$RUNNING" = "1"  ]; do
           sleep 2
           check_status
        done

        remove_pid_file
        echo "RHQ Agent has stopped."
        exit 0
        ;;

'kill')
        if [ "$RUNNING" = "0" ]; then
           echo $STATUS
           remove_pid_file
           exit 0
        fi

        echo Killing RHQ Agent...

        # do not try to gracefully kill, use a hard -KILL/-9
        echo "RHQ Agent (pid=${PID}) is being killed..."
        kill -9 $PID

        while [ "$RUNNING" = "1"  ]; do
           sleep 2
           check_status
        done

        remove_pid_file
        echo "RHQ Agent has been killed."
        exit 0
        ;;

'status')
        echo $STATUS
        exit 0
        ;;

'restart')
        ${THIS_SCRIPT} stop
        ${THIS_SCRIPT} start
        exit 0
        ;;

*)
        echo "Usage: $0 { start | stop | kill | restart | status }"
        exit 1
        ;;
esac
