###################################################################
# tc_to_a.rb
#
# Test suite for the NilClass#to_a instance method.
###################################################################
require "test/unit"

class TC_NilClass_ToA_Instance < Test::Unit::TestCase
   def setup
      @x = nil
   end
   
   def test_basic
      assert_respond_to(@x, :to_a)
      assert_nothing_raised{ @x.to_a } 
      assert_equal([], @x.to_a)
   end
end
