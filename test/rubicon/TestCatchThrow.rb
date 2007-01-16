$: << File.dirname($0) << File.join(File.dirname($0), "..")
require 'rubicon'

class TestCatchThrow < Rubicon::TestCase

  def testBasic
    assert(catch(:foo) {
             loop do
               loop do
                 throw :foo, true
                 break
               end
               break
               fail "should not reach here"
             end
             false
           })
  end

end

# Run these tests if invoked directly

Rubicon::handleTests(TestCatchThrow) if $0 == __FILE__
