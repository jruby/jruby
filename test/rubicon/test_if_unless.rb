require 'test/unit'

class TestIfUnless < Test::Unit::TestCase

  def testBasic
    x = 'test'
    assert(if $x == $x then true else false end)
    unless x == x
      fail("Unless failed to find equality")
    end
    assert(unless x != x then true else false end)
  end
end
