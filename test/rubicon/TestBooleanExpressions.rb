$: << File.dirname($0) << File.join(File.dirname($0), "..")
require 'rubicon'

class TestBooleanExpressions < Rubicon::TestCase

  def testBasicConditions
    x = '0'
    x == x && assert(true)
    x != x && fail
    x == x || fail
    x != x || assert(true)
  end

end

# Run these tests if invoked directly

Rubicon::handleTests(TestBooleanExpressions) if $0 == __FILE__
