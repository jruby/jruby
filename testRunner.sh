echo running Java tests:

JRUBY_LIB=~/development/jruby/lib
JRUBY_BASE=.
JRUBY_CLASSPATH=$JRUBY_BASE:$JRUBY_BASE/jruby.jar:$JRUBY_LIB/junit.jar

java -cp $JRUBY_CLASSPATH junit.textui.TestRunner org.jruby.test.MainTestSuite 

echo
echo running ruby tests:
java -cp $JRUBY_CLASSPATH -Djruby.home=$JRUBY_BASE org.jruby.Main test/testSuite.rb

