###############################################################################
# tc_year.rb
#
# Test case for the Time#year instance method.
###############################################################################
require 'test/unit'

class TC_Time_Year_InstanceMethod < Test::Unit::TestCase
   def setup
      @time = Time.mktime(2007, 6, 29)
   end

   def test_year_basic
      assert_respond_to(@time, :year)
      assert_nothing_raised{ @time.year }
      assert_kind_of(Integer, @time.year)
   end

   def test_year
      assert_equal(2007, @time.year)
      assert_equal(2007, Time.mktime(2007).year)
      assert_equal(1970, Time.mktime(1970).year)
      assert_equal(2004, Time.mktime(2004, 2, 29).year)
      assert_equal(2007, Time.mktime(2007, 2, 29).year)
   end

   def test_year_expected_errors
      assert_raise(ArgumentError){ @time.year(1) }
      assert_raise(NoMethodError){ @time.year = 1 }
   end

   def teardown
      @time = nil
   end
end
