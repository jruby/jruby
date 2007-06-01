###############################################################################
# tc_sum.rb
#
# Test case for the String#sum instance method.
###############################################################################
require "test/unit"

class TC_String_Sum_Instance < Test::Unit::TestCase
   def setup
      @string1 = "now is the time"
      @string2 = "hello\n&^*!@#world"
   end

   def test_sum_basic
      assert_respond_to(@string1, :sum)
      assert_nothing_raised{ @string1.sum }
      assert_kind_of(Fixnum, @string1.sum)
   end

   def test_sum
      assert_equal(1408, @string1.sum)
      assert_equal(128, @string1.sum(8))
      assert_equal(1400, @string2.sum)
   end

   def test_sum_edge_cases
      assert_equal(1408, @string1.sum(0))
      assert_equal(0, @string1.sum(1))
      assert_equal(1408, @string1.sum(-1))
      assert_equal(0, @string1.sum(7.5))
   end

   def test_sum_expected_errors
      assert_raises(ArgumentError){ @string1.sum(5, 6) }
      assert_raises(TypeError){ @string1.sum('test') }
      assert_raises(TypeError){ @string1.sum(nil) }
   end

   def teardown
      @string1 = nil
      @string2 = nil
   end
end
