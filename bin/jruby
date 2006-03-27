#!/bin/bash
# -----------------------------------------------------------------------------
# jruby.sh - Start Script for the JRuby interpreter
#
# Environment Variable Prequisites
#
#   JRUBY_BASE    (Optional) Base directory for resolving dynamic portions
#                 of a JRuby installation.  If not present, resolves to
#                 the same directory that JRUBY_HOME points to.
#
#   JRUBY_HOME    (Optional) May point at your JRuby "build" directory.
#                 If not present, the current working directory is assumed.
#
#   JRUBY_OPTS    (Optional) Default JRuby command line args
#
#   JAVA_HOME     Must point at your Java Development Kit installation.
#
# -----------------------------------------------------------------------------

cygwin=false

# ----- Identify OS we are running under --------------------------------------
case "`uname`" in
CYGWIN*) cygwin=true
esac

# ----- Verify and Set Required Environment Variables -------------------------

if [ -z "$JRUBY_HOME" ] ; then
  ## resolve links - $0 may be a link to  home
  PRG=$0
  progname=`basename "$0"`
  
  while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '.*/.*' > /dev/null; then
	PRG="$link"
    else
	PRG="`dirname $PRG`/$link"
    fi
  done
  
  JRUBY_HOME_1=`dirname "$PRG"`           # the ./bin dir
  JRUBY_HOME_1=`dirname "$JRUBY_HOME_1"`  # the . dir
  if [ -d "${JRUBY_HOME_1}/lib" ] ; then 
	JRUBY_HOME="${JRUBY_HOME_1}"
  fi
else
  if $cygwin; then
    JRUBY_HOME=`cygpath -u "$JRUBY_HOME"`
  fi
fi

if [ -z "$JRUBY_OPTS" ] ; then
  JRUBY_OPTS=""
fi

if [ -z "$JAVA_HOME" ] ; then
  echo You must set JAVA_HOME to point at your Java Development Kit installation
  exit 1
else
  if $cygwin; then
    JAVA_HOME=`cygpath -u "$JAVA_HOME"`
  fi
fi

# ----- Set Up The System Classpath -------------------------------------------

if [ "$CLASSPATH" != "" ]; then
  CP="$CLASSPATH"
  if $cygwin; then
    CP=`cygpath -p -w "$CP"`
  fi
else
  CP=""
fi

if $cygwin; then
   CP_DELIMETER=";"
else 
   CP_DELIMETER=":"
fi

for i in "$JRUBY_HOME"/lib/*.jar; do
  CP=$CP$CP_DELIMETER$i
done

# ----- Set Up JRUBY_BASE If Necessary -------------------------------------

if [ -z "$JRUBY_BASE" ] ; then
  JRUBY_BASE="$JRUBY_HOME"
fi

# ----- Execute The Requested Command -----------------------------------------

DEBUG=""
if [ "$1" = "JAVA_DEBUG" ]; then
  DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y"
  shift
else
  if [ "$1" = "JPROFILER" ]; then
    export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$JPROFILER_PATH/bin/linux-x86
    DEBUG="-Xrunjprofiler:port=8000,noexit -Xbootclasspath/a:/$JPROFILER_PATH/bin/agent.jar"
    shift
  else if [ "$1" = "HPROF" ]; then
      DEBUG="-Xrunhprof:cpu=samples"
      shift
  fi
  fi
fi

if $cygwin; then
  JAVA_HOME=`cygpath --mixed "$JAVA_HOME"`
  JRUBY_BASE=`cygpath --mixed "$JRUBY_BASE"`
  JRUBY_HOME=`cygpath --mixed "$JRUBY_HOME"`
fi

EN_US=
if [ "$1" = "EN_US" ]; then
  EN_US="-Duser.language=en -Duser.country=US"
  shift
fi

  "$JAVA_HOME/bin/java" $DEBUG -ea -classpath "$CP" \
  "-Djruby.base=$JRUBY_BASE" \
  "-Djruby.home=$JRUBY_HOME" \
  "-Djruby.lib=$JRUBY_BASE/lib" \
  -Djruby.script=jruby.sh \
  -Djruby.shell=/bin/sh \
  $EN_US \
  org.jruby.Main $JRUBY_OPTS "$@" 

