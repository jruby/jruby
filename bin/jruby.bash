#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# jruby.bash - Start Script for the JRuby interpreter
# -----------------------------------------------------------------------------

# ----- Set variable defaults -------------------------------------------------

readonly java_class=org.jruby.Main
readonly JRUBY_SHELL=/bin/sh

# Detect cygwin and mingw environments
cygwin=false
case "$(uname)" in
    CYGWIN*) cygwin=true ;;
    MINGW*)
        jruby.exe "$@"
        exit $?
        ;;
esac
readonly cygwin

use_exec=true
java_opts_from_files=""

NO_BOOTCLASSPATH=false
VERIFY_JRUBY=false
print_environment_log=false

if [ -z "$JRUBY_OPTS" ]; then
    JRUBY_OPTS=""
fi

if [ -z "$JAVA_STACK" ]; then
    JAVA_STACK=-Xss2048k
fi

declare -a java_args
declare -a ruby_args

# Force OpenJDK-based JVMs to use /dev/urandom for random number generation
# See https://github.com/jruby/jruby/issues/4685 among others.
# OpenJDK tries really hard to prevent you from using urandom.
# See https://bugs.openjdk.java.net/browse/JDK-6202721
# Non-file URL causes fallback to slow threaded SeedGenerator.
# See https://bz.apache.org/bugzilla/show_bug.cgi?id=56139
if [ -r "/dev/urandom" ]; then
    JAVA_SECURITY_EGD="file:/dev/urandom"
fi

# Gather environment information as we go
environment_log=$'JRuby Environment\n================='
add_log() {
    environment_log+=$'\n'"$1"
}

# Logic to process "arguments files" on both Java 8 and Java 9+
process_java_opts() {
    local java_opts_file="$1"
    if [ -r "$java_opts_file" ]; then
        add_log
        add_log "Adding Java options from: $java_opts_file"

        while read -r line; do
            if [ "$line" ]; then
                add_log "  $line"
            fi
        done < "$java_opts_file"

        # On Java 9+, add an @argument for the given file.
        # On earlier versions the file contents will be read and expanded on the Java command line.
        if $use_modules; then
            java_opts_from_files="$java_opts_from_files @$java_opts_file"
        else
            java_opts_from_files="$java_opts_from_files $(cat "$java_opts_file")"
        fi
    fi
}

# Pure shell dirname/basename
dir_name() {
    local filename="$1" trail
    case $filename in
        */*[!/]*)
            trail=${filename##*[!/]}
            filename=${filename%%"$trail"}
            result=${filename%/*}
            ;;
        *[!/]*)
            trail=${filename##*[!/]}
            result="."
            ;;
        *)
            result="/"
            ;;
    esac
}

base_name() {
    local filename="$1" trail
    case $filename in
        */*[!/]*)
            trail=${filename##*[!/]}
            filename=${filename%%"$trail"}
            result=${filename##*/}
            ;;
        *[!/]*)
            trail=${filename##*[!/]}
            result=${filename%%"$trail"}
            ;;
        *)
            result="/"
            ;;
    esac
}

# Resolve all symlinks in a chain
resolve_symlinks() {
    local cur_path sym sym_base dirname basename
    cur_path="$1"
    while [ -h "$cur_path" ]; do
        # 1) cd to directory of the symlink
        # 2) cd to the directory of where the symlink points
        # 3) get the physical pwd
        # 4) append the basename
        sym="$(readlink "$cur_path")"

        dir_name "$cur_path"
        dirname="$result"

        sym_base="$(cd -P -- "$dirname" >/dev/null && pwd -P)"

        dir_name "$sym"
        dirname="$result"

        base_name "$sym"
        basename="$result"

        cur_path="$(cd "$sym_base" && cd "$dirname" && pwd -P)/$basename"
    done
    result="$cur_path"
}

# ----- Determine JRUBY_HOME based on this executable's path ------------------

# get the absolute path of the executable
dir_name "$BASH_SOURCE"
BASE_DIR="$(cd -P -- "$result" >/dev/null && pwd -P)"
base_name "$BASH_SOURCE"
resolve_symlinks "$BASE_DIR/$result"
SELF_PATH="$result"

JRUBY_HOME="${SELF_PATH%/*/*}"

# ----- File paths for various options and files we'll process later ----------

# Module options to open up packages we need to reflect
readonly jruby_module_opts_file="$JRUBY_HOME/bin/.jruby.module_opts"

# Cascading .java_opts files for localized JVM flags
readonly installed_jruby_java_opts_file="$JRUBY_HOME/bin/.jruby.java_opts"
readonly home_jruby_java_opts_file="$HOME/.jruby.java_opts"
readonly pwd_jruby_java_opts_file="$PWD/.jruby.java_opts"

# Options from .dev_mode.java_opts for "--dev" mode, to reduce JRuby startup time
readonly dev_mode_opts_file="$JRUBY_HOME/bin/.dev_mode.java_opts"

# Default JVM Class Data Sharing Archive (jsa) file for JVMs that support it
readonly jruby_jsa_file="$JRUBY_HOME/lib/jruby.jsa"

# ----- Initialize environment log --------------------------------------------

add_log
add_log "JRuby executable:"
add_log "  $BASH_SOURCE"
add_log "JRuby command line options:"
add_log "  $*"
add_log "Current directory:"
add_log "  $PWD"

add_log
add_log "Environment:"
add_log "  JRUBY_HOME: $JRUBY_HOME"
add_log "  JRUBY_OPTS: $JRUBY_OPTS"
add_log "  JAVA_OPTS: $JAVA_OPTS"

# ----- Discover JVM and prep environment to run it ---------------------------

# Determine where the java command is and ensure we have a good JAVA_HOME
if [ -z "$JAVACMD" ]; then
    if [ -z "$JAVA_HOME" ]; then
        readonly java_home_command="/usr/libexec/java_home"
        if [ -r "$java_home_command" ] \
            && [ -x "$java_home_command" ] \
            && [ ! -d "$java_home_command" ]
        then
            # use java_home command when none is set (on MacOS)
            JAVA_HOME="$("$java_home_command")"
            JAVACMD="$JAVA_HOME"/bin/java
        else
            # Linux and others have a chain of symlinks
            resolve_symlinks "$(command -v java)"
            JAVACMD="$result"

            # export separately from command execution
            dir_name "$JAVACMD"
            dir_name "$result"
            JAVA_HOME="$result"
        fi
    elif $cygwin; then
        JAVACMD="$(cygpath -u "$JAVA_HOME")/bin/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
else
    resolve_symlinks "$(command -v "$JAVACMD")"
    expanded_javacmd="$result"
    if [ -z "$JAVA_HOME" ] && [ -x "$expanded_javacmd" ]; then
        dir_name "$expanded_javacmd"
        dir_name "$result"
        JAVA_HOME="$result"
    fi
fi

# Detect modularized Java if modules file is present or a MODULES line appears in release
if [ -f "$JAVA_HOME"/lib/modules ] \
    || {
        [ -f "$JAVA_HOME"/release ] \
        && grep -q ^MODULES "$JAVA_HOME"/release
    }
then
    use_modules=true
else
    use_modules=false
fi
readonly use_modules

add_log "  JAVACMD: $JAVACMD"
add_log "  JAVA_HOME: $JAVA_HOME"

if $use_modules; then
    add_log
    add_log "Detected Java modules at $JAVA_HOME"
fi

# ----- Process .java_opts files ----------------------------------------------

# We include options on the java command line in the following order:
#
# * JRuby installed bin/.jruby.java_opts (empty by default)
# * user directory .jruby.java_opts
# * current directory .jruby.java_opts
# * dev mode options from bin/.dev_mode.java_opts, if --dev is specified
# * module options from bin/.jruby.module_opts if modules are detected
# * JAVA_OPTS environment variable
# * command line flags

# Add local and global .jruby.java_opts
process_java_opts "$installed_jruby_java_opts_file"
process_java_opts "$home_jruby_java_opts_file"
process_java_opts "$pwd_jruby_java_opts_file"

# Capture some Java options to be passed separately
JAVA_OPTS_TEMP=""
for opt in $JAVA_OPTS; do
    case $opt in
        -Xmx*) JAVA_MEM="$opt" ;;
        -Xss*) JAVA_STACK="$opt" ;;
        *) JAVA_OPTS_TEMP="$JAVA_OPTS_TEMP $opt" ;;
    esac
done

JAVA_OPTS="$JAVA_OPTS_TEMP"

# ----- Set up the JRuby class/module path ------------------------------------

CP_DELIMITER=":"

# add main jruby jar to the classpath
JRUBY_ALREADY_ADDED=false
for j in "$JRUBY_HOME"/lib/jruby.jar "$JRUBY_HOME"/lib/jruby-complete.jar; do
    if [ ! -e "$j" ]; then
        continue
    fi
    if [ "$JRUBY_CP" ]; then
        JRUBY_CP="$JRUBY_CP$CP_DELIMITER$j"
    else
        JRUBY_CP="$j"
    fi
    if $JRUBY_ALREADY_ADDED; then
        echo "WARNING: more than one JRuby JAR found in lib directory" 1>&2
    fi
    JRUBY_ALREADY_ADDED=true
done

if $cygwin; then
    JRUBY_CP="$(cygpath -p -w "$JRUBY_CP")"
fi

# ----- Add additional jars from lib to classpath -----------------------------

if [ "$JRUBY_PARENT_CLASSPATH" ]; then
    # Use same classpath propagated from parent jruby
    CP="$JRUBY_PARENT_CLASSPATH"
else
    # add other jars in lib to CP for command-line execution
    for j in "$JRUBY_HOME"/lib/*.jar; do
        case "${j#"$JRUBY_HOME/lib/"}" in
            jruby.jar|jruby-complete.jar) continue
        esac
        if [ "$CP" ]; then
            CP="$CP$CP_DELIMITER$j"
        else
            CP="$j"
        fi
    done

    if [ "$CP" ] && $cygwin; then
        CP="$(cygpath -p -w "$CP")"
    fi
fi

if $cygwin; then
    # switch delimiter only after building Unix style classpaths
    CP_DELIMITER=";"
fi

readonly CP_DELIMITER

# ----- Continue processing JRuby options into JVM options --------------------

# Split out any -J argument for passing to the JVM.
# Scanning for args is aborted by '--'.
set -- $JRUBY_OPTS "$@"
while [ $# -gt 0 ]
do
    case $1 in
        # Stuff after '-J' in this argument goes to JVM
        -J-Xmx*) JAVA_MEM="${1#-J}" ;;
        -J-Xss*) JAVA_STACK="${1#-J}" ;;
        -J)
            "$JAVACMD" -help
            echo "(Prepend -J in front of these options when using 'jruby' command)" 1>&2
            exit
            ;;
        -J-X)
            "$JAVACMD" -X
            echo "(Prepend -J in front of these options when using 'jruby' command)" 1>&2
            exit
            ;;
        -J-classpath|-J-cp)
            CP="$CP$CP_DELIMITER$2"
            CLASSPATH=""
            shift
            ;;
        -J-ea*)
            VERIFY_JRUBY=true
            java_args+=("${1#-J}")
            ;;
        -J-Djava.security.egd=*) JAVA_SECURITY_EGD=${1#-J-Djava.security.egd=} ;;
        # This must be the last check for -J
        -J*) java_args+=("${1#-J}") ;;
        # Pass -X... and -X? search options through
        -X*...|-X*\?) ruby_args+=("$1") ;;
        # Match -Xa.b.c=d to translate to -Da.b.c=d as a java option
        -X*.*) java_args+=("-Djruby.${1#-X}") ;;
        # Match switches that take an argument
        -C|-e|-I|-S)
            ruby_args+=("$1" "$2")
            shift
            ;;
        # Run with JMX management enabled
        --manage)
            java_args+=("-Dcom.sun.management.jmxremote")
            java_args+=("-Djruby.management.enabled=true")
            ;;
        # Don't launch a GUI window, no matter what
        --headless) java_args+=("-Djava.awt.headless=true") ;;
        # Run under JDB
        --jdb)
            if [ -z "$JAVA_HOME" ]; then
                JAVACMD='jdb'
            else
                if $cygwin; then
                    JAVACMD="$(cygpath -u "$JAVA_HOME")/bin/jdb"
                else
                    JAVACMD="$JAVA_HOME/bin/jdb"
                fi
            fi
            JDB_SOURCEPATH="${JRUBY_HOME}/core/src/main/java:${JRUBY_HOME}/lib/ruby/stdlib:."
            java_args+=("-sourcepath" "$JDB_SOURCEPATH")
            JRUBY_OPTS+=("-X+C")
            ;;
        --client|--server|--noclient)
            echo "Warning: the $1 flag is deprecated and has no effect most JVMs" 1>&2
            ;;
        --dev)
            process_java_opts "$dev_mode_opts_file"
            # For OpenJ9 use environment variable to enable quickstart and shareclasses
            export OPENJ9_JAVA_OPTIONS="-Xquickstart -Xshareclasses"
            ;;
        --sample) java_args+=("-Xprof") ;;
        --record)
            java_args+=("-XX:+FlightRecorder" "-XX:StartFlightRecording=dumponexit=true")
            ;;
        --no-bootclasspath) NO_BOOTCLASSPATH=true ;;
        --ng*)
            echo "Error: Nailgun is no longer supported" 1>&2
            exit 1
            ;;
        --environment) print_environment_log=true ;;
        # warn but ignore
        --1.8|--1.9|--2.0) echo "warning: $1 ignored" 1>&2 ;;
        # Abort processing on the double dash
        --) break ;;
        # Other opts go to ruby
        -*) ruby_args+=("$1") ;;
        # Abort processing on first non-opt arg
        *) break ;;
    esac
    shift
done

# Force JDK to use specified java.security.egd rand source
if [ -n "$JAVA_SECURITY_EGD" ]; then
    java_args+=("-Djava.security.egd=$JAVA_SECURITY_EGD")
fi

# The rest of the arguments are for ruby
ruby_args+=("$@")
shift $#

JAVA_OPTS="$JAVA_OPTS $JAVA_MEM $JAVA_STACK"

JFFI_OPTS="-Djffi.boot.library.path=$JRUBY_HOME/lib/jni"

CLASSPATH="${CP}${CP_DELIMITER}${CLASSPATH}"

# ----- Tweak console environment for cygwin ----------------------------------

if $cygwin; then
    use_exec=false
    JRUBY_HOME="$(cygpath --mixed "$JRUBY_HOME")"
    JRUBY_SHELL="$(cygpath --mixed "$JRUBY_SHELL")"

    ruby_arg_first="${ruby_args[1]}"
    if [ "${ruby_arg_first:0:1}" = "/" ] \
        && [ -f "$ruby_arg_first" ] \
        || [ -d "$ruby_arg_first" ]
    then
        ruby_args=("$(cygpath -w "$ruby_arg_first")" "${ruby_args[@]:1}")
    fi

    # fix JLine to use UnixTerminal
    if stty -icanon min 1 -echo > /dev/null 2>&1; then
        JAVA_OPTS="$JAVA_OPTS -Djline.terminal=jline.UnixTerminal"
    fi

fi

# ----- Module and Class Data Sharing flags for Java 9+ -----------------------

if $use_modules; then
    # Use module path instead of classpath for the jruby libs
    classpath_args=(--module-path "$JRUBY_CP" -classpath "$CLASSPATH")

    # Switch to non-boot path since we can't use bootclasspath on 9+
    NO_BOOTCLASSPATH=true

    # Add base opens we need for Ruby compatibility
    process_java_opts "$jruby_module_opts_file"

    # Allow overriding default JSA file location
    if [ -z "$JRUBY_JSA" ]; then
        JRUBY_JSA="$jruby_jsa_file"
    fi

    # If we have a jruby.jsa file, enable AppCDS
    if [ -f "$JRUBY_JSA" ]; then
        add_log
        add_log "Detected Class Data Sharing archive:"
        add_log "  $JRUBY_JSA"

        JAVA_OPTS="$JAVA_OPTS -XX:+UnlockDiagnosticVMOptions -XX:SharedArchiveFile=$JRUBY_JSA"
    fi
else
    classpath_args=(-classpath "$JRUBY_CP$CP_DELIMITER$CLASSPATH")
fi

# ----- Final prepration of the Java command line -----------------------------

# Include all options from files at the beginning of the Java command line
JAVA_OPTS="$java_opts_from_files $JAVA_OPTS"

# Don't quote JAVA_OPTS; we want it to expand
jvm_command=("$JAVACMD" $JAVA_OPTS "$JFFI_OPTS" "${java_args[@]}")

if $NO_BOOTCLASSPATH || $VERIFY_JRUBY; then
    jvm_command+=("${classpath_args[@]}")
else
    jvm_command+=(-Xbootclasspath/a:"$JRUBY_CP" \
        -classpath "$CLASSPATH" "-Djruby.home=$JRUBY_HOME")
fi

jvm_command+=("-Djruby.home=$JRUBY_HOME" \
    "-Djruby.lib=$JRUBY_HOME/lib" "-Djruby.script=jruby" \
    "-Djruby.shell=$JRUBY_SHELL" \
    "$java_class" "${ruby_args[@]}")

add_log
add_log "Java command line:"
add_log "  ${jvm_command[*]}"

if $print_environment_log; then
    echo "$environment_log"
    exit 0
fi

# ----- Run JRuby! ------------------------------------------------------------

if $use_exec; then
    exec "${jvm_command[@]}"
else
    "${jvm_command[@]}"

    # Record the exit status immediately, or it will be overridden.
    JRUBY_STATUS=$?

    if $cygwin; then
        stty icanon echo > /dev/null 2>&1
    fi

    exit $JRUBY_STATUS
fi
