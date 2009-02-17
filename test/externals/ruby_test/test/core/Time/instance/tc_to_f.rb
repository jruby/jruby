###############################################################################
# tc_to_f.rb
#
# Test case for the Time#to_f instance method.
###############################################################################
require 'test/unit'

class TC_Time_ToF_InstanceMethod < Test::Unit::TestCase
   def setup
      @time1 = Time.mktime(2007, 6, 29, 7, 3, 27)
      @time2 = Time.mktime(2007, 6, 29, 7, 3, 27, 53000)
   end

   def test_to_f_basic
      assert_respond_to(@time1, :to_f)
      assert_nothing_raised{ @time1.to_f }
      assert_kind_of(Numeric, @time1.to_f)
   end

   def test_to_f
#      assert_equal(1183122207.0, @time1.to_f)
#      assert_equal(1183122207.053, @time2.to_f)
   end

   def test_to_f_expected_errors
      assert_raise(ArgumentError){ @time1.to_f(1) }
      assert_raise(NoMethodError){ @time1.to_f = 1 }
   end

   def teardown
      @time1 = nil
   end
end
