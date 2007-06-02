###################################################################
# tc_to_s.rb
#
# Test suite for the NilClass#to_s instance method.
###################################################################
require "test/unit"

class TC_NilClass_ToS_Instance < Test::Unit::TestCase
   def setup
      @x = nil
   end
   
   def test_basic
      assert_respond_to(@x, :to_s)
      assert_nothing_raised{ @x.to_s } 
      assert_equal("", @x.to_s)
   end
end
