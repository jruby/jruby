$: << File.dirname($0) << File.join(File.dirname($0), "..")
require 'rubicon'

class TestIfUnless < Rubicon::TestCase

  def testBasic
    x = 'test'
    assert(if $x == $x then true else false end)
    unless x == x
      fail("Unless failed to find equality")
    end
    assert(unless x != x then true else false end)
  end
end

# Run these tests if invoked directly

Rubicon::handleTests(TestIfUnless) if $0 == __FILE__
