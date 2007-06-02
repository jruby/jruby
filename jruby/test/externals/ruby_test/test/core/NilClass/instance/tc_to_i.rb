###################################################################
# tc_to_i.rb
#
# Test suite for the NilClass#to_i instance method.
###################################################################
require "test/unit"

class TC_NilClass_ToI_Instance < Test::Unit::TestCase
   def setup
      @x = nil
   end
   
   def test_basic
      assert_respond_to(@x, :to_i)
      assert_nothing_raised{ @x.to_i } 
      assert_equal(0, @x.to_i)
      assert_equal(0.0, @x.to_i)
   end
end
