echo running Java tests:

java -cp build/jruby.jar;lib/junit.jar junit.textui.TestRunner org.jruby.test.MainTestSuite 
@echo
@echo running ruby tests:
@jruby.sh test/testSuite.rb

