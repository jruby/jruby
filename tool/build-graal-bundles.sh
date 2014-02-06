#!/bin/bash

# This script combines the public snapshots of JRuby and Graal to produce a
# tarball that will use the included Graal VM instead of your local Java. This
# means you can run jruby and get Graal without having to do any other
# configuration.

# Takes no arguments. Produces:
#    * jruby-dist-9000+graal-linux-x86_64.dev-bin.tar.gz
#    * jruby-dist-9000+graal-macosx-x86_64.dev-bin.tar.gz
# which are the artifacts to be published.

# Creates files and directories in the working directory. Always downloads the
# latest JRuby snapshot, but doesn't download Graal if it's already there, as
# it's versioned. Removes some stuff from the Graal distribution to save space.

# Chris Seaton, 6 Feb 14

rm -f jruby-dist-9000.dev-bin.tar.gz jruby-dist-9000+graal-linux-x86_64.dev-bin.tar.gz jruby-dist-9000+graal-macosx-x86_64.dev-bin.tar.gz

wget http://ci.jruby.org/snapshots/master/jruby-dist-9000.dev-bin.tar.gz || exit $?
tar -zxf jruby-dist-9000.dev-bin.tar.gz || exit $?

# Remove files we aren't going to patch so people don't use them by mistake

rm jruby-9000.dev/bin/*.bat jruby-9000.dev/bin/*.sh jruby-9000.dev/bin/*.bash jruby-9000.dev/bin/*.exe jruby-9000.dev/bin/*.dll

# Patch the jruby bash script to set JAVACMD and JRUBY_OPTS

sed -i .backup 's|if \[ -z "\$JAVACMD" \] ; then|# Modifications for distribution with Graal\
JAVACMD=\"\$JRUBY_HOME/graalvm-jdk1.8.0/bin/java\"\
JRUBY_OPTS=\"-J-server -J-d64 \$JRUBY_OPTS\"\
\
if [ -z "$JAVACMD" ] ; then|' jruby-9000.dev/bin/jruby

if diff jruby-9000.dev/bin/jruby jruby-9000.dev/bin/jruby.backup >/dev/null ; then
  echo "patch didn't work"
  exit 1
fi

rm jruby-9000.dev/bin/jruby.backup

chmod +x jruby-9000.dev/bin/jruby

# Combine with Graal for Linux

if [ ! -e openjdk-8-graalvm-b122-linux-x86_64-0.1.tar.gz ]
then
  wget http://lafo.ssw.uni-linz.ac.at/graalvm/openjdk-8-graalvm-b122-linux-x86_64-0.1.tar.gz || exit $?
fi

tar -zxf openjdk-8-graalvm-b122-linux-x86_64-0.1.tar.gz || exit $?
cp -r graalvm-jdk1.8.0 jruby-9000.dev || exit $?
rm -rf jruby-9000.dev/graalvm-jdk1.8.0/src.zip jruby-9000.dev/graalvm-jdk1.8.0/demo jruby-9000.dev/graalvm-jdk1.8.0/include jruby-9000.dev/graalvm-jdk1.8.0/sample
tar -zcf jruby-dist-9000+graal-linux-x86_64.dev-bin.tar.gz jruby-9000.dev || exit $?

# Combine with Graal for Mac

if [ ! -e openjdk-8-graalvm-b122-macosx-x86_64-0.1.tar.gz ]
then
  wget http://lafo.ssw.uni-linz.ac.at/graalvm/openjdk-8-graalvm-b122-macosx-x86_64-0.1.tar.gz || exit $?
fi

tar -zxf openjdk-8-graalvm-b122-macosx-x86_64-0.1.tar.gz || exit $?
cp -r graalvm-jdk1.8.0 jruby-9000.dev || exit $?
rm -rf jruby-9000.dev/graalvm-jdk1.8.0/src.zip jruby-9000.dev/graalvm-jdk1.8.0/demo jruby-9000.dev/graalvm-jdk1.8.0/include jruby-9000.dev/graalvm-jdk1.8.0/sample
tar -zcf jruby-dist-9000+graal-macosx-x86_64.dev-bin.tar.gz jruby-9000.dev || exit $?
