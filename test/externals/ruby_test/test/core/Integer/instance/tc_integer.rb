#####################################################################
# tc_integer?.rb
#
# Test case for the Integer#integer? method.
#####################################################################
require 'test/unit'

class TC_Integer_IsInteger_Instance < Test::Unit::TestCase
   def setup
      @int1 = 65
   end

   def test_integer_basic
      assert_respond_to(@int1, :integer?)
   end

   def test_integer
      assert_nothing_raised{ @int1.integer? }
      assert(@int1.integer?)
      assert(-1.integer?)
      assert(0.integer?)
   end

   def test_integer_expected_errors
      assert_raises(ArgumentError){ @int1.integer?(2) }
   end

   def teardown
      @int1 = nil
   end
end
