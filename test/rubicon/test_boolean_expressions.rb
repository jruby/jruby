require 'test/unit'

class TestBooleanExpressions < Test::Unit::TestCase

  def testBasicConditions
    x = '0'
    x == x && assert(true)
    x != x && fail
    x == x || fail
    x != x || assert(true)
  end

end
