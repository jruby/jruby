#!/bin/bash

# This script combines the public snapshots of JRuby and Graal to produce a
# tarball that will use the included Graal VM instead of your local Java. This
# means you can run JRuby and get Graal without having to do any other
# configuration. It doesn't automatically set -X+T - so you aren't using
# Truffle by default - just Graal.

# Takes no arguments. Produces:
#    * jruby-dist-$version+graal-linux-x86_64-bin.tar.gz
#    * jruby-dist-$version+graal-macosx-x86_64-bin.tar.gz
# which are the artifacts to be published.

# Run in the root directory. Run -Pdist first.

echo "build-graal-bundles does not work with an unreleased Graal"
exit 1

version=`cat VERSION`

tar -zxf maven/jruby-dist/target/jruby-dist-$version-bin.tar.gz || exit $?

# Remove files we aren't going to patch so people don't use them by mistake

rm jruby-$version/bin/*.bat jruby-$version/bin/*.sh jruby-$version/bin/*.bash jruby-$version/bin/*.exe jruby-$version/bin/*.dll || exit $?

# Patch the jruby bash script to set JAVACMD and JRUBY_OPTS

sed -i.backup 's|if \[ -z "\$JAVACMD" \] ; then|# Modifications for distribution with Graal\
JAVACMD=\"\$JRUBY_HOME/graalvm-jdk1.8.0/bin/java\"\
JRUBY_OPTS=\"-J-server -J-d64 \$JRUBY_OPTS\"\
\
if [ -z "$JAVACMD" ] ; then|' jruby-$version/bin/jruby || exit $?

if diff jruby-$version/bin/jruby jruby-$version/bin/jruby.backup >/dev/null ; then
  echo "patch didn't work"
  exit 1
fi

rm jruby-$version/bin/jruby.backup || exit $?

chmod +x jruby-$version/bin/jruby || exit $?

function pack {
    # $1 ... platform (linux, ...)
    # $2 ... jdk-release (b122, ...)
    # $3 ... graal-release (0.1, ...)

    buildname=openjdk-8-graalvm-$2-$1-x86_64-$3.tar.gz

    if [ ! -e $buildname ]
    then
      wget http://lafo.ssw.uni-linz.ac.at/graalvm/$buildname || exit $?
    fi

    tar -zxf $buildname || exit $?
    chmod -R +w graalvm-jdk1.8.0
    
    cp -r graalvm-jdk1.8.0 jruby-$version || exit $?
    rm -rf jruby-$version/graalvm-jdk1.8.0/src.zip jruby-$version/graalvm-jdk1.8.0/demo jruby-$version/graalvm-jdk1.8.0/include jruby-$version/graalvm-jdk1.8.0/sample || exit $?
    targetname=jruby-dist-$version+graal-$1-x86_64-bin.tar.gz
    tar -zcf $targetname jruby-$version || exit $?
    shasum -a 1 $targetname > $targetname.sha1 || exit $?
    
    rm -rf jruby-master || exit $?
    
    cp -r jruby-$version jruby-master || exit $?
    targetname=jruby-dist-master+graal-$1-x86_64-bin.tar.gz
    tar -zcf $targetname jruby-master || exit $?
    shasum -a 1 $targetname > $targetname.sha1 || exit $?
}

pack "linux" "b132" "0.7"
pack "macosx" "b132" "0.7"
