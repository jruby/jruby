###################################################################
# tc_to_f.rb
#
# Test suite for the NilClass#to_f instance method.
###################################################################
require "test/unit"

class TC_NilClass_ToF_Instance < Test::Unit::TestCase
   def setup
      @x = nil
   end
   
   def test_basic
      assert_respond_to(@x, :to_f)
      assert_nothing_raised{ @x.to_f } 
      assert_equal(0.0, @x.to_f)
   end
end
