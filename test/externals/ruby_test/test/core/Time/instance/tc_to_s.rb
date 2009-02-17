########################################################################
# tc_to_s.rb
#
# Test case for the Time#to_s instance method.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Time_ToS_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @time   = Time.local(2007, 2, 3, 4, 5, 6)
      @offset = get_tz_offset

      if @offset < 10
         @offset = "-0#{@offset}00"
      else
         @offset = "-#{@offset}00"
      end
   end

   def test_to_s_basic
      assert_respond_to(:@time, :to_s)
      assert_nothing_raised{ @time.to_s }
      assert_kind_of(String, @time.to_s)
   end

   # MS Windows provides the Standard Name for '%z' instead of the offset
   def test_to_s
#      assert_equal("Sat Feb 03 04:05:06 #{@offset} 2007", @time.to_s)
      assert_equal("Thu Jan 01 00:00:00 UTC 1970", Time.gm(1970).to_s)
      unless WINDOWS
         assert_equal(@time.to_s, @time.strftime("%a %b %d %H:%M:%S %z %Y"))
      end
   end

   def test_to_s_expected_errors
      assert_raise(ArgumentError){ @time.to_s(1) }
      assert_raise(NoMethodError){ @time.to_s = 1 }
   end

   def teardown
      @time   = nil
      @offset = nil
   end
end
