#!/bin/sh
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


# ----- Verify and Set Required Environment Variables -------------------------

if [ -z "$JRUBY_HOME" ] ; then
  ## resolve links - $0 may be a link to  home
  PRG=$0
  progname=`basename $0`
  
  while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '.*/.*' > /dev/null; then
	PRG="$link"
    else
	PRG="`dirname $PRG`/$link"
    fi
  done
  
  JRUBY_HOME_1=`dirname "$PRG"`
  # echo "Guessing JRUBY_HOME from jruby.sh to ${JRUBY_HOME_1}" 
    if [ -d ${JRUBY_HOME_1}/samples ] ; then 
	JRUBY_HOME=${JRUBY_HOME_1}
	# echo "Setting JRUBY_HOME to $JRUBY_HOME"
    fi
fi

if [ -z "$JRUBY_OPTS" ] ; then
  JRUBY_OPTS=""
fi

if [ -z "$JAVA_HOME" ] ; then
  echo You must set JAVA_HOME to point at your Java Development Kit installation
  exit 1
fi


# ----- Cygwin Unix Paths Setup -----------------------------------------------

# Cygwin support.  $cygwin _must_ be set to either true or false.
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  *) cygwin=false ;;
esac
 
# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
  [ -n "$JRUBY_HOME" ] &&
    JRUBY_HOME=`cygpath --unix "$JRUBY_HOME"`
    [ -n "$JAVA_HOME" ] &&
    JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi


# ----- Set Up The System Classpath -------------------------------------------

CP="$JRUBY_HOME/jruby.jar:$JRUBY_HOME:$JRUBY_HOME/build/classes"

if [ -f "$JAVA_HOME/lib/tools.jar" ] ; then
  CP=$CP:"$JAVA_HOME/lib/tools.jar"
fi

for i in $JRUBY_HOME/lib/*.jar; do
  CP=$CP:$i
done


# ----- Cygwin Windows Paths Setup --------------------------------------------

# convert the existing path to windows
if $cygwin ; then
   CP=`cygpath --path --windows "$CP"`
   JRUBY_HOME=`cygpath --path --windows "$JRUBY_HOME"`
   JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
fi


# ----- Set Up JRUBY_BASE If Necessary -------------------------------------

if [ -z "$JRUBY_BASE" ] ; then
  JRUBY_BASE=$JRUBY_HOME
fi


# ----- Execute The Requested Command -----------------------------------------

# echo "Using CLASSPATH:  $CP"
# echo "Using JRUBY_BASE: $JRUBY_BASE"
# echo "Using JRUBY_HOME: $JRUBY_HOME"
# echo "Using JAVA_HOME:  $JAVA_HOME"

# shift
#  touch $JRUBY_BASE/logs/jruby.out
DEBUG=""
if [ "$1" = "JAVA_DEBUG" ]; then
  DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y"
  shift
else
  if [ "$1" = "JPROFILER" ]; then
    export LD_LIBRARY_PATH=/home/jpetersen/jprofiler/bin/linux-x86
    DEBUG="-Xrunjprofiler:port=8000,noexit -Xbootclasspath/a:/home/jpetersen/jprofiler/bin/agent.jar"
    shift
  else if [ "$1" = "HPROF" ]; then
      DEBUG="-Xrunhprof:cpu=samples"
      shift
  fi
  fi
fi

EN_US=
if [ "$1" = "EN_US" ]; then
  EN_US="-Duser.language=en -Duser.country=US"
  shift
fi

	
  $JAVA_HOME/bin/java $DEBUG -classpath $CP \
  -Djruby.base=$JRUBY_BASE \
  -Djruby.home=$JRUBY_HOME \
  -Djruby.lib=$JRUBY_BASE/lib \
  -Djruby.script=jruby.sh \
  -Djruby.shell=/bin/sh \
  $EN_US \
     org.jruby.Main $JRUBY_OPTS "$@" 
#     \
#  >> $JRUBY_BASE/logs/jruby.out 2>&1 &

