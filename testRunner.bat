@echo running Java tests:

@java -cp build\jruby.jar;lib\junit.jar junit.textui.TestRunner org.jruby.test.MainTestSuite | ruby -p bin/JAVAException.rb
@echo
@echo running ruby tests:
@jruby test/testSuite.rb

