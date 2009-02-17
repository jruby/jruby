########################################################################
# tc_marshal.rb
#
# Because the time.c source file implements a custom version of _dump
# and _load, we test it independently here.
########################################################################
require 'test/unit'

class TC_Time_Marshal_InstanceMethod < Test::Unit::TestCase
   def setup
      @time     = Time.local(2001, 12, 2, 16, 30, 45)
      @marshal  = nil
      @umarshal = nil
   end

   def test_dump
      assert_respond_to(@time, :_dump)
      assert_nothing_raised{ @time._dump }
      assert_kind_of(String, @time._dump)
   end

   def test_load
      assert_respond_to(Time, :_load)
   end

   def test_time_marshal_dump
      assert_nothing_raised{ @marshal = Marshal.dump(@time) }
#      assert_equal("\004\bu:\tTime\rWl\031\200\000\000\320z", @marshal)
   end

   def test_time_marshal_load
      assert_nothing_raised{ @marshal = Marshal.dump(@time) }
      assert_nothing_raised{ @umarshal = Marshal.load(@marshal) }
      assert_equal(true, @umarshal == @time)
   end

   def test_load_expected_errors
      assert_raise(ArgumentError){ Time._load }
      assert_raise(TypeError){ Time._load(1) }
      assert_raise(TypeError){ Time._load(Time.new) }
   end

   def teardown
      @time = nil
      @marshal = nil
      @umarshal = nil
   end
end
