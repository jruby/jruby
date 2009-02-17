########################################################################
# tc_local.rb
#
# Test case for the Time.local class method and the Time.mktime alias.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Time_Local_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @year   = 2001
      @mon    = 'Feb'
      @dow    = 'Tue'
      @day    = 27
      @hour   = '09'
      @min    = 30
      @sec    = 18
      @usec   = '03'
      @local  = nil
      
      # Starting in 1.8.5 the TZ name was replaced by a numeric offset
      if RELEASE >= 5 || JRUBY
         @offset = get_tz_offset  
         if @offset < 10
            @offset = "-0#{@offset}00"
         else
            @offset = "-#{@offset}00"
         end
      else
         @offset = get_tz_name
      end
   end

   def test_local_basic
      assert_respond_to(Time, :local)
      assert_nothing_raised{ Time.local(@year) }
   end

   def test_mktime_alias_basic
      assert_respond_to(Time, :mktime)
      assert_nothing_raised{ Time.mktime(@year) }
   end

   def test_local_year_only
      assert_nothing_raised{ @local = Time.local(@year) }
      assert_kind_of(Time, @local)
      assert_equal("Mon Jan 01 00:00:00 #{@offset} #{@year}", @local.to_s)
      assert_equal(Time.local(@year), Time.local(@year, nil))
   end

   def test_mktime_alias_year_only
      assert_nothing_raised{ @local = Time.mktime(@year) }
      assert_kind_of(Time, @local)
      assert_equal("Mon Jan 01 00:00:00 #{@offset} #{@year}", @local.to_s)
      assert_equal(Time.mktime(@year), Time.mktime(@year, nil))
   end

   def test_local_year_and_month
      assert_nothing_raised{ @local = Time.local(@year, @mon) }
      assert_kind_of(Time, @local)
      assert_equal("Thu #{@mon} 01 00:00:00 #@offset #{@year}", @local.to_s)
      assert_equal(Time.local(@year, @mon), Time.local(@year, @mon, nil))
   end

   def test_mktime_alias_year_and_month
      assert_nothing_raised{ @local = Time.mktime(@year, @mon) }
      assert_kind_of(Time, @local)
      assert_equal("Thu #{@mon} 01 00:00:00 #@offset #{@year}", @local.to_s)
      assert_equal(Time.mktime(@year, @mon), Time.mktime(@year, @mon, nil))
   end

   def test_local_year_month_and_day
      assert_nothing_raised{ @local = Time.local(@year, @mon, @day) }
      assert_kind_of(Time, @local)
      assert_equal("#{@dow} #{@mon} #{@day} 00:00:00 #@offset #{@year}", @local.to_s)
      assert_equal(Time.local(@year, @mon, @day), Time.local(@year, @mon, @day, nil))
   end

   def test_mktime_alias_year_month_and_day
      assert_nothing_raised{ @local = Time.mktime(@year, @mon, @day) }
      assert_kind_of(Time, @local)
      assert_equal("#{@dow} #{@mon} #{@day} 00:00:00 #@offset #{@year}", @local.to_s)
      assert_equal(Time.mktime(@year, @mon, @day), Time.mktime(@year, @mon, @day, nil))
   end

   def test_local_year_month_day_and_hour
      assert_nothing_raised{ @local = Time.local(@year, @mon, @day, @hour) }
      assert_kind_of(Time, @local)
      assert_equal("#@dow #@mon #@day #@hour:00:00 #@offset #@year", @local.to_s)
      assert_equal(@local, Time.local(@year, @mon, @day, @hour, nil))
   end

   def test_mktime_alias_year_month_day_and_hour
      assert_nothing_raised{ @local = Time.mktime(@year, @mon, @day, @hour) }
      assert_kind_of(Time, @local)
      assert_equal("#@dow #@mon #@day #@hour:00:00 #@offset #@year", @local.to_s)
      assert_equal(@local, Time.mktime(@year, @mon, @day, @hour, nil))
   end

   def test_local_year_month_day_hour_and_minute
      assert_nothing_raised{ @local = Time.local(@year, @mon, @day, @hour, @min) }
      assert_kind_of(Time, @local)

      @min = "0#{@min}" if @min < 10

      assert_equal("#@dow #@mon #@day #@hour:#@min:00 #@offset #@year", @local.to_s)
      assert_equal(@local, Time.local(@year, @mon, @day, @hour, @min, nil))
   end

   def test_mktime_alias_year_month_day_hour_and_minute
      assert_nothing_raised{ @local = Time.mktime(@year, @mon, @day, @hour, @min) }
      assert_kind_of(Time, @local)

      @min = "0#{@min}" if @min < 10

      assert_equal("#@dow #@mon #@day #@hour:#@min:00 #@offset #@year", @local.to_s)
      assert_equal(@local, Time.mktime(@year, @mon, @day, @hour, @min, nil))
   end

   def test_local_year_month_day_hour_minute_and_second
      assert_nothing_raised{ @local = Time.local(@year,@mon,@day,@hour,@min,@sec) }
      assert_kind_of(Time, @local)

      @min = "0#{@min}" if @min < 10
      @sec = "0#{@sec}" if @sec < 10

      assert_equal("#@dow #@mon #@day #@hour:#@min:#@sec #@offset #@year", @local.to_s)
      assert_equal(@local, Time.local(@year, @mon, @day, @hour, @min, @sec, nil))
   end

   def test_mktime_alias_year_month_day_hour_minute_and_second
      assert_nothing_raised{ @local = Time.mktime(@year,@mon,@day,@hour,@min,@sec) }
      assert_kind_of(Time, @local)

      @min = "0#{@min}" if @min < 10
      @sec = "0#{@sec}" if @sec < 10

      assert_equal("#@dow #@mon #@day #@hour:#@min:#@sec #@offset #@year", @local.to_s)
      assert_equal(@local, Time.mktime(@year, @mon, @day, @hour, @min, @sec, nil))
   end

   def test_local_year_month_day_hour_minute_second_and_usec
      assert_nothing_raised{ @local = Time.local(@year,@mon,@day,@hour,@min,@sec,@usec) }
      assert_kind_of(Time, @local)

      @min = "0#{@min}" if @min < 10
      @sec = "0#{@sec}" if @sec < 10

      assert_equal("#@dow #@mon #@day #@hour:#@min:#@sec #@offset #@year", @local.to_s)
      assert_equal(@local, Time.local(@year, @mon, @day, @hour, @min, @sec, @usec))
   end

   def test_mktime_alias_year_month_day_hour_minute_second_and_usec
      assert_nothing_raised{ @local = Time.mktime(@year,@mon,@day,@hour,@min,@sec,@usec) }
      assert_kind_of(Time, @local)

      @min = "0#{@min}" if @min < 10
      @sec = "0#{@sec}" if @sec < 10

      assert_equal("#@dow #@mon #@day #@hour:#@min:#@sec #@offset #@year", @local.to_s)
      assert_equal(@local, Time.mktime(@year, @mon, @day, @hour, @min, @sec, @usec))
   end
   
   def test_local_with_tz
      assert_nothing_raised{ @local = Time.local(1,2,3,27,2,2001,'Tue',287,true,'MST') }
      assert_kind_of(Time, @local)
      assert_equal("Tue Feb 27 02:02:01 #@offset 2001", @local.to_s)
   end

   def test_mktime_alias_with_tz
      assert_nothing_raised{ @local = Time.mktime(1,2,3,27,2,2001,'Tue',287,true,'MST') }
      assert_kind_of(Time, @local)
      assert_equal("Tue Feb 27 02:02:01 #@offset 2001", @local.to_s)
   end
   
   def test_local_with_floats
      assert_nothing_raised{ Time.local(2007.0) }
      assert_nothing_raised{ Time.local(2007.0, 10.1) }
      assert_nothing_raised{ Time.local(2007.0, 10.1, 14.2) }
      assert_nothing_raised{ Time.local(2007.0, 10.1, 14.2, 7.3) }
      assert_nothing_raised{ Time.local(2007.0, 10.1, 14.2, 7.3, 5.4) }
      assert_nothing_raised{ Time.local(2007.0, 10.1, 14.2, 7.3, 5.4, 3.9) }
      assert_nothing_raised{ Time.local(2007.0, 10.1, 14.2, 7.3, 5.4, 3.9, 0.2) }    
   end

   def test_mktime_alias_with_floats
      assert_nothing_raised{ Time.mktime(2007.0) }
      assert_nothing_raised{ Time.mktime(2007.0, 10.1) }
      assert_nothing_raised{ Time.mktime(2007.0, 10.1, 14.2) }
      assert_nothing_raised{ Time.mktime(2007.0, 10.1, 14.2, 7.3) }
      assert_nothing_raised{ Time.mktime(2007.0, 10.1, 14.2, 7.3, 5.4) }
      assert_nothing_raised{ Time.mktime(2007.0, 10.1, 14.2, 7.3, 5.4, 3.9) }
      assert_nothing_raised{ Time.mktime(2007.0, 10.1, 14.2, 7.3, 5.4, 3.9, 0.2) }    
   end

   # The type checking for MRI's Time.local is poor because of the two forms
   # accepted. Also, it seems to allow negative usec's.
   def test_local_expected_errors
      assert_raise(ArgumentError){ Time.local }
      assert_raise(ArgumentError){ Time.local(0,1,2,3,4,5,6,7,8,9,10) }
      assert_raise(ArgumentError){ Time.local(-1) }
      assert_raise(ArgumentError){ Time.local(999999999) }
      assert_raise(ArgumentError){ Time.local(@year, -1) }
      assert_raise(ArgumentError){ Time.local(@year, @mon, -1) }
      assert_raise(ArgumentError){ Time.local(@year, @mon, @day, -1) }
      assert_raise(ArgumentError){ Time.local(@year, @mon, @day, @hour, -1) }
      assert_raise(ArgumentError){ Time.local(@year, @mon, @day, @hour, @min, -1) }
      #assert_raise(ArgumentError){ Time.local(@year, @mon, @day, @hour, @min, @sec, -1) }
   end

   def test_mktime_alias_expected_errors
      assert_raise(ArgumentError){ Time.mktime }
      assert_raise(ArgumentError){ Time.mktime(0,1,2,3,4,5,6,7,8,9,10) }
      assert_raise(ArgumentError){ Time.mktime(-1) }
      assert_raise(ArgumentError){ Time.mktime(999999999) }
      assert_raise(ArgumentError){ Time.mktime(@year, -1) }
      assert_raise(ArgumentError){ Time.mktime(@year, @mon, -1) }
      assert_raise(ArgumentError){ Time.mktime(@year, @mon, @day, -1) }
      assert_raise(ArgumentError){ Time.mktime(@year, @mon, @day, @hour, -1) }
      assert_raise(ArgumentError){ Time.mktime(@year, @mon, @day, @hour, @min, -1) }
      #assert_raise(ArgumentError){ Time.local(@year, @mon, @day, @hour, @min, @sec, -1) }
   end

   def teardown
      @year = nil
      @mon  = nil
      @day  = nil
      @hour = nil
      @min  = nil
      @sec  = nil
      @local  = nil
      @offset = nil
   end
end
