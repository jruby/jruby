###############################################################################
# tc_to_i.rb
#
# Test case for the Time#to_i instance method and the Time#tv_sec alias.
###############################################################################
require 'test/unit'

class TC_Time_ToI_InstanceMethod < Test::Unit::TestCase
   def setup
      @time = Time.mktime(2007, 6, 29, 7, 3, 27)
   end

   def test_to_i_basic
      assert_respond_to(@time, :to_i)
      assert_nothing_raised{ @time.to_i }
      assert_kind_of(Numeric, @time.to_i)
   end

   def test_tv_sec_alias_basic
      assert_respond_to(@time, :tv_sec)
      assert_nothing_raised{ @time.tv_sec }
      assert_kind_of(Numeric, @time.tv_sec)
   end

   def test_to_i
#      assert_equal(1183122207, @time.to_i)
      assert_equal(0, Time.gm(1970).to_i)
      assert_equal(1078012800, Time.gm(2004, 2, 29).to_i)
   end

   def test_tv_sec_alias
#      assert_equal(1183122207, @time.tv_sec)
      assert_equal(0, Time.gm(1970).tv_sec)
      assert_equal(1078012800, Time.gm(2004, 2, 29).tv_sec)
   end

   def test_to_i_expected_errors
      assert_raise(ArgumentError){ @time.to_i(1) }
      assert_raise(NoMethodError){ @time.to_i = 1 }
   end

   def test_tv_sec_alias_expected_errors
      assert_raise(ArgumentError){ @time.tv_sec(1) }
      assert_raise(NoMethodError){ @time.tv_sec = 1 }
   end

   def teardown
      @time = nil
   end
end
