########################################################################
# tc_strftime.rb
#
# Test case for the Time#strftime instance method.
#
# TODO: Add tests for 'c', 'x', 'X', and 'Z'.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Time_Strftime_InstanceMethod < Test::Unit::TestCase
   def setup
      @time = Time.local(2004, 8, 26, 22, 38, 3)
   end

   def test_strftime_basic
      assert_respond_to(@time, :strftime)
      assert_nothing_raised{ @time.strftime('%a') }
      assert_kind_of(String, @time.strftime('%a'))
   end

   def test_strftime_a
      assert_equal('Thu', @time.strftime('%a'))
   end

   def test_strftime_A
      assert_equal('Thursday', @time.strftime('%A'))
   end

   def test_strftime_b
      assert_equal('Aug', @time.strftime('%b'))
   end

   def test_strftime_B
      assert_equal('August', @time.strftime('%B'))
   end

   def test_strftime_d
      assert_equal('26', @time.strftime('%d'))
   end

   def test_strftime_H
      assert_equal('22', @time.strftime('%H'))
   end

   def test_strftime_I
      assert_equal('10', @time.strftime('%I'))
   end

   def test_strftime_j
      assert_equal('239', @time.strftime('%j'))
   end

   def test_strftime_m
      assert_equal('08', @time.strftime('%m'))
   end

   def test_strftime_M
      assert_equal('38', @time.strftime('%M'))
   end

   def test_strftime_p
      assert_equal('PM', @time.strftime('%p'))
   end

   def test_strftime_S
      assert_equal('03', @time.strftime('%S'))
   end

   def test_strftime_U
      assert_equal('34', @time.strftime('%U'))
   end

   # TODO: Add a different time here to differentiate between 'U' and 'W'
   def test_strftime_W
      assert_equal('34', @time.strftime('%W'))
   end

   def test_strftime_w
      assert_equal('4', @time.strftime('%w'))
   end

   def test_strftime_y
      assert_equal('04', @time.strftime('%y'))
   end

   def test_strftime_Y
      assert_equal('2004', @time.strftime('%Y'))
   end

   def test_strftime_percent
      assert_equal('%', @time.strftime('%%'))
   end

   def teardown
      @time = nil
   end
end
