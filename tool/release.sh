#!/bin/sh

# Notes on this script:
#  1. on failed run you must undo the version commit and probably should drop in nexus staging repo
#  2. Assumes jruby you are using in your PATH works

export REPO=${REPO:=jruby}

java_version=$(java -version 2>&1 | head -1 | awk -F\" -e '{ print $2 }' | awk -F. '{print $1 "." $2 }')

if [ "$java_version" != "1.8" ]; then
   echo "You must use Java 1.8 to release JRuby"
   exit 1
fi

[[ -z "$JRUBY_VERSION" ]] && { echo "set JRUBY_VERSION to something" ; exit 1; }

export WINDOWS_VERSION=`echo $JRUBY_VERSION|sed -e 's/\./_/g'`
echo $JRUBY_VERSION > VERSION

set -x

mvn
git add VERSION core/pom.xml lib/pom.xml  pom.xml shaded/pom.xml
git commit -m "Version $JRUBY_VERSION updated for release"
cd ..
rm -rf release
git clone $REPO release
cd release
pwd
mvn clean deploy -Prelease
jruby -S rake post_process_artifacts

cd release

# Install4j has changed how it writes this out so fix it in post
mv jruby_windows-x32_${WINDOWS_VERSION}.exe jruby_windows_${WINDOWS_VERSION}.exe
mv jruby_windows-x32_${WINDOWS_VERSION}.exe.md5 jruby_windows_${WINDOWS_VERSION}.exe.md5
mv jruby_windows-x32_${WINDOWS_VERSION}.exe.sha1 jruby_windows_${WINDOWS_VERSION}.exe.sha1
mv jruby_windows-x32_${WINDOWS_VERSION}.exe.sha256 jruby_windows_${WINDOWS_VERSION}.exe.sha256
mv jruby_windows-x32_${WINDOWS_VERSION}.exe.sha512 jruby_windows_${WINDOWS_VERSION}.exe.sha512

# At this point you should run scripts on platforms to make sure things look ok.
# FIXME: we should consider running a collection of scripts we can automate here and add to CI.

