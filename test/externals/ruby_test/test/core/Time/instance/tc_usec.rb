###############################################################################
# tc_usec.rb
#
# Test case for the Time#usec instance method and the Time#tv_usec alias.
###############################################################################
require 'test/unit'

class TC_Time_Usec_InstanceMethod < Test::Unit::TestCase
   def setup
      @time = Time.mktime(2007, 6, 29, 7, 22, 39, 53000)
   end

   def test_usec_basic
      assert_respond_to(@time, :usec)
      assert_nothing_raised{ @time.usec }
      assert_kind_of(Integer, @time.usec)
   end

   def test_tv_usec_alias_basic
      assert_respond_to(@time, :tv_usec)
      assert_nothing_raised{ @time.tv_usec }
      assert_kind_of(Integer, @time.tv_usec)
   end

   def test_usec
      assert_equal(53000, @time.usec)
      assert_equal(0, Time.mktime(2007).usec)
      assert_equal(0, Time.mktime(2004, 2, 29).usec)
      assert_equal(0, Time.mktime(2007, 2, 29).usec)
      assert_equal(0, Time.mktime(2007, 2, 29, 1).usec)
      assert_equal(0, Time.mktime(2007, 2, 29, 1, 2).usec)
      assert_equal(0, Time.mktime(2007, 2, 29, 1, 2, 3).usec)
      assert_equal(4, Time.mktime(2007, 2, 29, 1, 2, 3, 4).usec)
   end

   def test_tv_usec_alias
      assert_equal(53000, @time.tv_usec)
      assert_equal(0, Time.mktime(2007).tv_usec)
      assert_equal(0, Time.mktime(2004, 2, 29).tv_usec)
      assert_equal(0, Time.mktime(2007, 2, 29).tv_usec)
      assert_equal(0, Time.mktime(2007, 2, 29, 1).tv_usec)
      assert_equal(0, Time.mktime(2007, 2, 29, 1, 2).tv_usec)
      assert_equal(0, Time.mktime(2007, 2, 29, 1, 2, 3).tv_usec)
      assert_equal(4, Time.mktime(2007, 2, 29, 1, 2, 3, 4).tv_usec)
   end

   def test_usec_expected_errors
      assert_raise(ArgumentError){ @time.usec(1) }
      assert_raise(NoMethodError){ @time.usec = 1 }
   end

   def test_tv_usec_alias_expected_errors
      assert_raise(ArgumentError){ @time.tv_usec(1) }
      assert_raise(NoMethodError){ @time.tv_usec = 1 }
   end

   def teardown
      @time = nil
   end
end
