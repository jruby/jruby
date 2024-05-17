#!/bin/sh
# shellcheck disable=1007
# -----------------------------------------------------------------------------
# jruby.bash - Start Script for the JRuby interpreter
# -----------------------------------------------------------------------------

# ----- Guarantee local variables are available -------------------------------
if command -v local >/dev/null; then
    :
elif command -v typeset >/dev/null; then
    # ksh93 and older have typeset but not local, and expand aliases at parse
    # time so require re-sourcing the script
    alias local=typeset
    if [ -z "$KSH_VERSION" ] || (eval : '"${.sh.version}"' >/dev/null 2>&1); then
        # shellcheck source=/dev/null
        . "$0"
        exit
    fi
else
    echo >&2 "Error: Your shell does not support local variables. Re-run this script with one that does (e.g. bash, ksh)"
    exit 1
fi

# ----- Helper functions ------------------------------------------------------

# esceval [ARGUMENT...]
#
# Escape ARGUMENT for safe use with eval
# Returns escaped arguments via $REPLY
# Thanks to @mentalisttraceur for original implementation:
# https://github.com/mentalisttraceur/esceval
esceval()
{
    local escaped= unescaped= output=
    REPLY=

    [ $# -gt 0 ] || return 0
    while true; do
        escaped=\'
        unescaped=$1
        while true; do
            case $unescaped in
                (*\'*)
                    escaped="$escaped${unescaped%%\'*}'\''"
                    unescaped=${unescaped#*\'}
                    ;;
                (*) break ;;
            esac
        done
        escaped=$escaped$unescaped\'
        shift
        [ $# -gt 0 ] || break
        output="$output $escaped"
    done
    REPLY="$output $escaped"
}

# assign LISTNAME ELEMENT [ELEMENT...]
#
# Assign ELEMENT to the list named by LISTNAME.
assign() {
    local listname="$1"
    local REPLY=
    shift

    esceval "$@"
    eval "$listname=\"\${REPLY}\""
}

# append LISTNAME ELEMENT [ELEMENT...]
#
# Append ELEMENT to the list named by LISTNAME.
append() {
    local listname="$1"
    local REPLY=
    shift

    esceval "$@"
    eval "$listname=\"\${$listname} \${REPLY}\""
}

# prepend LISTNAME ELEMENT [ELEMENT...]
#
# Prepend ELEMENT to the list named by LISTNAME, preserving order.
prepend() {
    local listname="$1"
    local REPLY=
    shift

    esceval "$@"
    eval "$listname=\"\${REPLY} \${$listname}\""
}

# extend LISTNAME1 LISTNAME2
#
# Append the elements stored in the list named by LISTNAME2
# to the list named by LISTNAME1.
extend() {
    eval "$1=\"\${$1} \${$2}\""
}

# preextend LISTNAME1 LISTNAME2
#
# Prepend the elements stored in the list named by LISTNAME2
# to the named by LISTNAME1, preserving order.
preextend() {
    eval "$1=\"\${$2} \${$1}\""
}

# echo [STRING...]
#
# Dumb echo, i.e. print arguments joined by spaces with no further processing
echo() {
    printf "%s\n" "$*"
}

# ----- Set variable defaults -------------------------------------------------

readonly java_class=org.jruby.Main
JRUBY_SHELL=/bin/sh

# Detect cygwin and mingw environments
cygwin=false
case "$(uname)" in
    CYGWIN*) cygwin=true ;;
    MINGW*)
        release_id=$(awk -F= '$1=="ID" { print $2; }' /etc/os-release 2> /dev/null)
        case $release_id in
            "msys2") ;;
            *)
                jruby.exe "$@"
                exit $?
                ;;
        esac
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

java_args=""
ruby_args=""

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
readonly cr='
'
environment_log="JRuby Environment$cr================="
add_log() {
    environment_log="${environment_log}${cr}${*}"
}

# Logic to process "arguments files" on both Java 8 and Java 9+
process_java_opts() {
    local java_opts_file="$1" java_opts=
    if [ -r "$java_opts_file" ]; then
        add_log
        add_log "Adding Java options from: $java_opts_file"

        while read -r line; do
            if [ "$line" ]; then
                java_opts="${java_opts} ${line}"
                add_log "  $line"
            fi
        done < "$java_opts_file"

        # On Java 9+, add an @argument for the given file.
        # On earlier versions the file contents will be read and expanded on the Java command line.
        if $use_modules; then
            java_opts_from_files="$java_opts_from_files @$java_opts_file"
        else
            java_opts_from_files="$java_opts_from_files $java_opts"
        fi
    fi
}

# Pure shell dirname/basename
dir_name() {
    local filename="$1" trail=
    case $filename in
        */*[!/]*)
            trail=${filename##*[!/]}
            filename=${filename%%"$trail"}
            REPLY=${filename%/*}
            ;;
        *[!/]*)
            trail=${filename##*[!/]}
            REPLY="."
            ;;
        *)
            REPLY="/"
            ;;
    esac
}

base_name() {
    local filename="$1" trail=
    case $filename in
        */*[!/]*)
            trail=${filename##*[!/]}
            filename=${filename%%"$trail"}
            REPLY=${filename##*/}
            ;;
        *[!/]*)
            trail=${filename##*[!/]}
            REPLY=${filename%%"$trail"}
            ;;
        *)
            REPLY="/"
            ;;
    esac
}

# Determine whether path is absolute and contains no relative segments or symlinks
path_is_canonical() {
    local path=
    for path; do
        case $path in
            ([!/]*) return 1 ;;
            (./*|../*) return 1 ;;
            (*/.|*/..) return 1 ;;
            (*/./*|*/../*) return 1 ;;
        esac
        while [ "$path" ]; do
            [ -h "$path" ] && return 1
            path="${path%/*}"
        done
    done
    return 0
}

# Resolve directory to its canonical value
resolve_dir() {
    # Some shells (dash, ksh) resolve relative paths by default before cd'ing, i.e.
    # cd /foo/bar/../baz = cd /foo/baz
    # This is fine unless bar is a symlink, in which case the second form is
    # invalid. Passing -P to cd fixes this behaviour.
    REPLY="$(cd -P -- "$1" && pwd)"
}

# Resolve symlink until it's not a symlink
resolve_file() {
    local current="$1" target=

    while [ -h "$current" ]; do
        target="$(readlink "$current")" || return
        case $target in
            (/*) current="$target" ;;
            # handle relative symlinks
            (*) dir_name "$current"; current="$REPLY/$target" ;;
        esac
    done
    REPLY="$current"
}

# Resolve path to its canonical value
resolve() {
    local target="$1" base=
    REPLY=

    # Verify target actually exists (and isn't too deep in symlinks)
    if ! [ -e "$target" ]; then
        echo >&2 "Error: No such file or directory: $target"
        return 1
    fi

    # Realpath is way faster than repeatedly calling readlink, so use it if possible
    if command -v realpath >/dev/null; then
        REPLY="$(realpath "$target")" && return
    fi

    # Take shortcut for directories
    if [ -d "$target" ]; then
        resolve_dir "$target" && return
    fi

    # Ensure $target is not a symlink
    resolve_file "$target" || return
    target="$REPLY"

    # Resolve parent directory if it's not absolute
    if ! path_is_canonical "$target"; then
        dir_name "$target"
        resolve_dir "$REPLY" || return
        base="$REPLY"

        base_name "$target"
        target="$base/$REPLY"
    fi
    REPLY="$target"
}

# ----- Determine JRUBY_HOME based on this executable's path ------------------

# get the absolute path of the executable
if [ "$BASH" ]; then
    # shellcheck disable=2128,3028
    script_src="$BASH_SOURCE"
else
    script_src="$0"
fi
dir_name "$script_src"
BASE_DIR="$(cd -P -- "$REPLY" >/dev/null && pwd -P)"
base_name "$script_src"
resolve "$BASE_DIR/$REPLY"
SELF_PATH="$REPLY"

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
add_log "  $script_src"
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
            resolve "$(command -v java)"
            JAVACMD="$REPLY"
        fi
    elif $cygwin; then
        JAVACMD="$(cygpath -u "$JAVA_HOME")/bin/java"
    else
        resolve "$JAVA_HOME/bin/java"
        JAVACMD="$REPLY"
    fi
else
    resolve "$(command -v "$JAVACMD")"
    JAVACMD="$REPLY"
fi

# export separately from command execution
dir_name "$JAVACMD"
dir_name "$REPLY"
JAVA_HOME="$REPLY"

# Detect modularized Java
java_is_modular() {
    # check that modules file is present
    if [ -f "$JAVA_HOME"/lib/modules ]; then
        return 0
    fi

    # check if a MODULES line appears in release
    if [ -f "$JAVA_HOME"/release ] && grep -q ^MODULES "$JAVA_HOME"/release; then
        return 0
    fi

    return 1
}

if java_is_modular; then
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
# shellcheck disable=2086
set -- $JRUBY_OPTS "$@"
# increment pointer, permute arguments
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
            append java_args "${1#-J}"
            ;;
        -J-Djava.security.egd=*) JAVA_SECURITY_EGD=${1#-J-Djava.security.egd=} ;;
        # This must be the last check for -J
        -J*) append java_args "${1#-J}" ;;
        # Pass -X... and -X? search options through
        -X*...|-X*\?) append ruby_args "$1" ;;
        # Match -Xa.b.c=d to translate to -Da.b.c=d as a java option
        -X*.*) append java_args -Djruby."${1#-X}" ;;
        # Match switches that take an argument
        -[CeIS])
            append ruby_args "$1" "$2"
            shift
            ;;
        # Run with JMX management enabled
        --manage)
            append java_args -Dcom.sun.management.jmxremote
            append java_args -Djruby.management.enabled=true
            ;;
        # Don't launch a GUI window, no matter what
        --headless) append java_args -Djava.awt.headless=true ;;
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
            append java_args -sourcepath "$JDB_SOURCEPATH"
            append ruby_args -X+C
            ;;
        --client|--server|--noclient)
            echo "Warning: the $1 flag is deprecated and has no effect most JVMs" 1>&2
            ;;
        --dev)
            process_java_opts "$dev_mode_opts_file"
            # For OpenJ9 use environment variable to enable quickstart and shareclasses
            export OPENJ9_JAVA_OPTIONS="-Xquickstart -Xshareclasses"
            ;;
        --sample) append java_args -Xprof ;;
        --record)
            append java_args -XX:+FlightRecorder -XX:StartFlightRecording=dumponexit=true
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
        -*) append ruby_args "$1" ;;
        # Abort processing on first non-opt arg
        *) break ;;
    esac
    shift
done

# Force JDK to use specified java.security.egd rand source
if [ -n "$JAVA_SECURITY_EGD" ]; then
    append java_args "-Djava.security.egd=$JAVA_SECURITY_EGD"
fi

# The rest of the arguments are for ruby
append ruby_args "$@"

JAVA_OPTS="$JAVA_OPTS $JAVA_MEM $JAVA_STACK"

JFFI_OPTS="-Djffi.boot.library.path=$JRUBY_HOME/lib/jni"

CLASSPATH="${CP}${CP_DELIMITER}${CLASSPATH}"

# ----- Tweak console environment for cygwin ----------------------------------

if $cygwin; then
    use_exec=false
    JRUBY_HOME="$(cygpath --mixed "$JRUBY_HOME")"
    JRUBY_SHELL="$(cygpath --mixed "$JRUBY_SHELL")"

    eval set -- "$ruby_args"

    case $1 in
        /*)
            if [ -f "$1" ] || [ -d "$1" ]; then
                # replace first element of ruby_args with cygwin form
                win_arg="$(cygpath -w "$1")"
                shift
                set -- "$win_arg" "$@"
                assign ruby_args "$@"
            fi
            ;;
    esac

    # fix JLine to use UnixTerminal
    if stty -icanon min 1 -echo > /dev/null 2>&1; then
        JAVA_OPTS="$JAVA_OPTS -Djline.terminal=jline.UnixTerminal"
    fi

fi

# ----- Module and Class Data Sharing flags for Java 9+ -----------------------

if $use_modules; then
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
fi

# ----- Final prepration of the Java command line -----------------------------

# Include all options from files at the beginning of the Java command line
JAVA_OPTS="$java_opts_from_files $JAVA_OPTS"

# Don't quote JAVA_OPTS; we want it to expand
# shellcheck disable=2086
prepend java_args "$JAVACMD" $JAVA_OPTS "$JFFI_OPTS"

if $NO_BOOTCLASSPATH || $VERIFY_JRUBY; then
    if $use_modules; then
        # Use module path instead of classpath for the jruby libs
        append java_args --module-path "$JRUBY_CP" -classpath "$CLASSPATH"
    else
        append java_args -classpath "$JRUBY_CP$CP_DELIMITER$CLASSPATH"
    fi
else
    append java_args -Xbootclasspath/a:"$JRUBY_CP"
    append java_args -classpath "$CLASSPATH"
    append java_args -Djruby.home="$JRUBY_HOME"
fi

append java_args -Djruby.home="$JRUBY_HOME" \
    -Djruby.lib="$JRUBY_HOME/lib" \
    -Djruby.script=jruby \
    -Djruby.shell="$JRUBY_SHELL" \
    "$java_class"
extend java_args ruby_args

eval set -- "$java_args"

add_log
add_log "Java command line:"
add_log "  $*"

if $print_environment_log; then
    echo "$environment_log"
    exit 0
fi

# ----- Run JRuby! ------------------------------------------------------------

if $use_exec; then
    exec "$@"
else
    "$@"

    # Record the exit status immediately, or it will be overridden.
    JRUBY_STATUS=$?

    if $cygwin; then
        stty icanon echo > /dev/null 2>&1
    fi

    exit $JRUBY_STATUS
fi
