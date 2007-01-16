require 'test/unit'

class TestCatchThrow < Test::Unit::TestCase

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
