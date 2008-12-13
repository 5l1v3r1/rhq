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
# This script is customizable by setting certain environment variables, which
# are described in comments in rhq-agent-env.sh found in the same directory
# as this script. The variables can also be set via that rhq-agent-env.sh file,
# which is sourced by this script.
#
# Note that if this script is to be used as an init.d script, you must ensure
# this script has the RHQ_AGENT_HOME set so it knows where to find the
# agent installation.
# =============================================================================

# ----------------------------------------------------------------------
# Subroutine that simply dumps a message iff debug mode is enabled
# ----------------------------------------------------------------------

debug_wrapper_msg ()
{
   # if debug variable is set, it is assumed to be on, unless its value is false
   if [ "x$RHQ_AGENT_DEBUG" != "x" ]; then
      if [ "$RHQ_AGENT_DEBUG" != "false" ]; then
         echo "rhq-agent-wrapper.sh: $1"
      fi
   fi
}

# Get the location of this script
RHQ_AGENT_WRAPPER_BIN_DIR_PATH=`dirname $0`
debug_wrapper_msg RHQ_AGENT_WRAPPER_BIN_DIR_PATH=$RHQ_AGENT_WRAPPER_BIN_DIR_PATH

# Read in the rhq-agent-env.sh file so we get the configured agent environment
if [ -f "${RHQ_AGENT_WRAPPER_BIN_DIR_PATH}/rhq-agent-env.sh" ]; then
   debug_wrapper_msg "Loading environment script: ${RHQ_AGENT_WRAPPER_BIN_DIR_PATH}/rhq-agent-env.sh"
   . ${RHQ_AGENT_WRAPPER_BIN_DIR_PATH}/rhq-agent-env.sh $*
fi

# The --daemon argument is required, but you can add additional arguments as appropriate
if [ "x$RHQ_AGENT_CMDLINE_OPTS" = "x" ]; then
   export RHQ_AGENT_CMDLINE_OPTS=--daemon
fi

# Determine where this script is, and change to its directory
cd $RHQ_AGENT_WRAPPER_BIN_DIR_PATH
_THIS_SCRIPT_DIR=`pwd`
_THIS_SCRIPT=${_THIS_SCRIPT_DIR}/`basename $0`

# Figure out where the RHQ Agent's home directory is and cd to it.
# If RHQ_AGENT_HOME is not defined, we will assume we are running
# directly from the agent installation's bin directory

if [ "x$RHQ_AGENT_HOME" = "x" ]; then
   RHQ_AGENT_START_SCRIPT_DIR=${_THIS_SCRIPT_DIR}
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
_PIDFILE=${RHQ_AGENT_PIDFILE_DIR}/rhq-agent.pid
debug_wrapper_msg "pidfile will be located at ${_PIDFILE}"

RHQ_AGENT_START_SCRIPT=${RHQ_AGENT_START_SCRIPT_DIR}/rhq-agent.sh

if [ ! -f $RHQ_AGENT_START_SCRIPT ]; then
   echo "ERROR! Cannot find the RHQ Agent start script"
   echo "Not found: $RHQ_AGENT_START_SCRIPT"
   exit 1
fi
debug_wrapper_msg "Start script found here: $RHQ_AGENT_START_SCRIPT"

# Sets STATUS, RUNNING and PID based on the status of the RHQ Agent
check_status ()
{
    if [ -f ${_PIDFILE} ]; then
        PID=`cat ${_PIDFILE}`
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
   if [ -f ${_PIDFILE} ]; then
      debug_wrapper_msg "Removing existing pidfile"
      rm ${_PIDFILE}
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

        # prefix the start script if needed
        if [ "x$RHQ_AGENT_RUN_PREFIX" = "x" ]; then
           if [ "x$RHQ_AGENT_RUN_AS_ME" != "x" ]; then
              RHQ_AGENT_RUN_AS="$USER"
           fi

           if [ "x$RHQ_AGENT_RUN_AS" != "x" ]; then
              debug_wrapper_msg "Will run agent as user: $RHQ_AGENT_RUN_AS"
              RHQ_AGENT_START_SCRIPT="su -m - ${RHQ_AGENT_RUN_AS} -c '${RHQ_AGENT_START_SCRIPT}'"
           fi
        else
           if [ "x$RHQ_AGENT_RUN_PREFIX_QUOTED" != "x" ]; then
              if [ "$RHQ_AGENT_RUN_PREFIX_QUOTED" = "true" ]; then
                 RHQ_AGENT_RUN_PREFIX_QUOTED=\'
              fi
              debug_wrapper_msg "Quoting the start script with: ${RHQ_AGENT_RUN_PREFIX_QUOTED}"
              RHQ_AGENT_START_SCRIPT="${RHQ_AGENT_RUN_PREFIX_QUOTED}${RHQ_AGENT_START_SCRIPT}${RHQ_AGENT_RUN_PREFIX_QUOTED}"
           fi
        
           RHQ_AGENT_START_SCRIPT="$RHQ_AGENT_RUN_PREFIX $RHQ_AGENT_START_SCRIPT"
           debug_wrapper_msg "Start script has been prefixed with: ${RHQ_AGENT_RUN_PREFIX}"
        fi

        if [ "x$RHQ_AGENT_PASSWORD_PROMPT" != "x" ]; then
           if [ "$RHQ_AGENT_PASSWORD_PROMPT" = "true" ]; then
              RHQ_AGENT_PASSWORD_PROMPT="Enter password for user $RHQ_AGENT_RUN_AS"
           fi
           echo $RHQ_AGENT_PASSWORD_PROMPT
        fi

        RHQ_AGENT_IN_BACKGROUND=${_PIDFILE}
        export RHQ_AGENT_IN_BACKGROUND

        # start the agent now!
        if [ "x$RHQ_AGENT_DEBUG" = "x" -a "$RHQ_AGENT_DEBUG" != "false" ]; then
           $RHQ_AGENT_START_SCRIPT > /dev/null 2>&1
        else
           debug_wrapper_msg "Executing agent with command: ${RHQ_AGENT_START_SCRIPT}"
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
           debug_wrapper_msg "Agent did not die yet, trying to kill it again"
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
        ${_THIS_SCRIPT} stop
        ${_THIS_SCRIPT} start
        exit 0
        ;;

*)
        echo "Usage: $0 { start | stop | kill | restart | status }"
        exit 1
        ;;
esac
