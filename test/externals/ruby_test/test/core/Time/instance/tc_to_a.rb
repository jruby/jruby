########################################################################
# tc_to_a.rb
#
# Test case for the Time#to_a instance method.
########################################################################
require 'test/unit'

class TC_Time_ToA_InstanceMethod < Test::Unit::TestCase
   def setup
      @time = Time.gm(2007)
   end

   def test_to_a_basic
      assert_respond_to(@time, :to_a)
      assert_nothing_raised{ @time.to_a }
      assert_kind_of(Array, @time.to_a)
   end

   def test_to_a
      assert_equal([0, 0, 0, 1, 1, 2007, 1, 1, false, 'UTC'], @time.to_a)
   end

   def test_to_a_expected_errors
      assert_raise(ArgumentError){ @time.to_a(1) }
      assert_raise(NoMethodError){ @time.to_a = 1 }
   end

   def teardown
      @time = nil
   end
end
