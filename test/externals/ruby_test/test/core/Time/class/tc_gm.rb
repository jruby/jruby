########################################################################
# tc_gm.rb
#
# Test case for the Time.gm class method and the Time.utc alias.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Time_GM_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @year, @mon, @dow, @day, @hour, @min, @sec, @usec = *get_datetime

      @gmt = nil

      @day  = "0#{@day}" if @day.to_i < 10
      @hour = "0#{@hour}" if @hour.to_i < 10
      @min  = "0#{@min}" if @min.to_i < 10
      @sec  = "0#{@sec}" if @sec.to_i < 10
      @usec = "0#{@usec}" if @usec.to_i < 10
   end

   def test_gm_basic
      assert_respond_to(Time, :gm)
      assert_nothing_raised{ Time.gm(@year) }
   end

   def test_utc_alias_basic
      assert_respond_to(Time, :gm)
      assert_nothing_raised{ Time.gm(@year) }
   end

   def test_gm_year_only
      dow = Time.local(Time.now.year).strftime('%a')
      assert_nothing_raised{ @gmt = Time.gm(@year) }
      assert_kind_of(Time, @gmt)
      assert_equal("#{dow} Jan 01 00:00:00 UTC #{@year}", @gmt.to_s)
      assert_equal(Time.gm(@year), Time.gm(@year, nil))
   end

   def test_utc_alias_year_only
      dow = Time.local(Time.now.year).strftime('%a')
      assert_nothing_raised{ @gmt = Time.utc(@year) }
      assert_kind_of(Time, @gmt)
      assert_equal("#{dow} Jan 01 00:00:00 UTC #{@year}", @gmt.to_s)
      assert_equal(Time.utc(@year), Time.utc(@year, nil))
   end

   def test_gm_year_and_month
      assert_nothing_raised{ @dow = Time.local(@year, @mon, 1).strftime('%a') }
      assert_nothing_raised{ @gmt = Time.gm(@year, @mon) }
      assert_kind_of(Time, @gmt)
      assert_equal("#{@dow} #{@mon} 01 00:00:00 UTC #{@year}", @gmt.to_s)
      assert_equal(Time.gm(@year, @mon), Time.gm(@year, @mon, nil))
   end

   def test_utc_alias_year_and_month
      assert_nothing_raised{ @dow = Time.local(@year, @mon, 1).strftime('%a') }
      assert_nothing_raised{ @gmt = Time.utc(@year, @mon) }
      assert_kind_of(Time, @gmt)
      assert_equal("#{@dow} #{@mon} 01 00:00:00 UTC #{@year}", @gmt.to_s)
      assert_equal(Time.utc(@year, @mon), Time.utc(@year, @mon, nil))
   end

   def test_gm_year_month_and_day
      assert_nothing_raised{ @gmt = Time.gm(@year, @mon, @day) }
      assert_kind_of(Time, @gmt)
      assert_equal("#{@dow} #{@mon} #{@day} 00:00:00 UTC #{@year}", @gmt.to_s)
      assert_equal(Time.gm(@year, @mon, @day), Time.gm(@year, @mon, @day, nil))
   end

   def test_utc_alias_year_month_and_day
      assert_nothing_raised{ @gmt = Time.utc(@year, @mon, @day) }
      assert_kind_of(Time, @gmt)
      assert_equal("#{@dow} #{@mon} #{@day} 00:00:00 UTC #{@year}", @gmt.to_s)
      assert_equal(Time.utc(@year, @mon, @day), Time.utc(@year, @mon, @day, nil))
   end

   def test_gm_year_month_day_and_hour
      assert_nothing_raised{ @gmt = Time.gm(@year, @mon, @day, @hour) }
      assert_kind_of(Time, @gmt)
      assert_equal("#@dow #@mon #@day #@hour:00:00 UTC #@year", @gmt.to_s)
      assert_equal(@gmt, Time.gm(@year, @mon, @day, @hour, nil))
   end

   def test_utc_alias_year_month_day_and_hour
      assert_nothing_raised{ @gmt = Time.utc(@year, @mon, @day, @hour) }
      assert_kind_of(Time, @gmt)
      assert_equal("#@dow #@mon #@day #@hour:00:00 UTC #@year", @gmt.to_s)
      assert_equal(@gmt, Time.utc(@year, @mon, @day, @hour, nil))
   end

   def test_gm_year_month_day_hour_and_minute
      assert_nothing_raised{ @gmt = Time.gm(@year, @mon, @day, @hour, @min) }
      assert_kind_of(Time, @gmt)

      assert_equal("#@dow #@mon #@day #@hour:#@min:00 UTC #@year", @gmt.to_s)
      assert_equal(@gmt, Time.gm(@year, @mon, @day, @hour, @min, nil))
   end

   def test_utc_alias_year_month_day_hour_and_minute
      assert_nothing_raised{ @gmt = Time.utc(@year, @mon, @day, @hour, @min) }
      assert_kind_of(Time, @gmt)

      assert_equal("#@dow #@mon #@day #@hour:#@min:00 UTC #@year", @gmt.to_s)
      assert_equal(@gmt, Time.utc(@year, @mon, @day, @hour, @min, nil))
   end

   def test_gm_year_month_day_hour_minute_and_second
      assert_nothing_raised{ @gmt = Time.gm(@year,@mon,@day,@hour,@min,@sec) }
      assert_kind_of(Time, @gmt)

      assert_equal("#@dow #@mon #@day #@hour:#@min:#@sec UTC #@year", @gmt.to_s)
      assert_equal(@gmt, Time.gm(@year, @mon, @day, @hour, @min, @sec, nil))
   end

   def test_utc_alias_year_month_day_hour_minute_and_second
      assert_nothing_raised{ @gmt = Time.utc(@year,@mon,@day,@hour,@min,@sec) }
      assert_kind_of(Time, @gmt)

      assert_equal("#@dow #@mon #@day #@hour:#@min:#@sec UTC #@year", @gmt.to_s)
      assert_equal(@gmt, Time.utc(@year, @mon, @day, @hour, @min, @sec, nil))
   end

   def test_gm_year_month_day_hour_minute_second_and_usec
      assert_nothing_raised{ @gmt = Time.gm(@year,@mon,@day,@hour,@min,@sec,@usec) }
      assert_kind_of(Time, @gmt)

      assert_equal("#@dow #@mon #@day #@hour:#@min:#@sec UTC #@year", @gmt.to_s)
      assert_equal(@gmt, Time.gm(@year, @mon, @day, @hour, @min, @sec, @usec))
   end

   def test_utc_alias_year_month_day_hour_minute_second_and_usec
      assert_nothing_raised{ @gmt = Time.utc(@year,@mon,@day,@hour,@min,@sec,@usec) }
      assert_kind_of(Time, @gmt)

      assert_equal("#@dow #@mon #@day #@hour:#@min:#@sec UTC #@year", @gmt.to_s)
      assert_equal(@gmt, Time.utc(@year, @mon, @day, @hour, @min, @sec, @usec))
   end
   
   def test_gm_with_tz
      assert_nothing_raised{ @gmt = Time.gm(1,2,3,14,10,2007,'Sun',287,true,'MDT') }
      assert_kind_of(Time, @gmt)
      assert_equal('Sun Oct 14 03:02:01 UTC 2007', @gmt.to_s)
   end

   def test_utc_alias_with_tz
      assert_nothing_raised{ @gmt = Time.utc(1,2,3,14,10,2007,'Sun',287,true,'MDT') }
      assert_kind_of(Time, @gmt)
      assert_equal('Sun Oct 14 03:02:01 UTC 2007', @gmt.to_s)
   end
   
   def test_gm_with_floats
      assert_nothing_raised{ Time.gm(2007.0) }
      assert_nothing_raised{ Time.gm(2007.0, 10.1) }
      assert_nothing_raised{ Time.gm(2007.0, 10.1, 14.2) }
      assert_nothing_raised{ Time.gm(2007.0, 10.1, 14.2, 7.3) }
      assert_nothing_raised{ Time.gm(2007.0, 10.1, 14.2, 7.3, 5.4) }
      assert_nothing_raised{ Time.gm(2007.0, 10.1, 14.2, 7.3, 5.4, 3.9) }
      assert_nothing_raised{ Time.gm(2007.0, 10.1, 14.2, 7.3, 5.4, 3.9, 0.2) }    
   end

   def test_utc_alias_with_floats
      assert_nothing_raised{ Time.utc(2007.0) }
      assert_nothing_raised{ Time.utc(2007.0, 10.1) }
      assert_nothing_raised{ Time.utc(2007.0, 10.1, 14.2) }
      assert_nothing_raised{ Time.utc(2007.0, 10.1, 14.2, 7.3) }
      assert_nothing_raised{ Time.utc(2007.0, 10.1, 14.2, 7.3, 5.4) }
      assert_nothing_raised{ Time.utc(2007.0, 10.1, 14.2, 7.3, 5.4, 3.9) }
      assert_nothing_raised{ Time.utc(2007.0, 10.1, 14.2, 7.3, 5.4, 3.9, 0.2) }    
   end

   # The type checking for MRI's Time.gm is poor because of the two forms
   # accepted. Also, it seems to allow negative usec's.
   def test_gm_expected_errors
      assert_raise(ArgumentError){ Time.gm }
      assert_raise(ArgumentError){ Time.gm(0,1,2,3,4,5,6,7,8,9,10) }
#      assert_raise(ArgumentError){ Time.gm(-1) }
      assert_raise(ArgumentError){ Time.gm(999999999) }
      assert_raise(ArgumentError){ Time.gm(@year, -1) }
      assert_raise(ArgumentError){ Time.gm(@year, @mon, -1) }
      assert_raise(ArgumentError){ Time.gm(@year, @mon, @day, -1) }
      assert_raise(ArgumentError){ Time.gm(@year, @mon, @day, @hour, -1) }
      assert_raise(ArgumentError){ Time.gm(@year, @mon, @day, @hour, @min, -1) }
      #assert_raise(ArgumentError){ Time.gm(@year, @mon, @day, @hour, @min, @sec, -1) }
   end

   def test_utc_alias_expected_errors
      assert_raise(ArgumentError){ Time.utc }
      assert_raise(ArgumentError){ Time.utc(0,1,2,3,4,5,6,7,8,9,10) }
#      assert_raise(ArgumentError){ Time.utc(-1) }
      assert_raise(ArgumentError){ Time.utc(999999999) }
      assert_raise(ArgumentError){ Time.utc(@year, -1) }
      assert_raise(ArgumentError){ Time.utc(@year, @mon, -1) }
      assert_raise(ArgumentError){ Time.utc(@year, @mon, @day, -1) }
      assert_raise(ArgumentError){ Time.utc(@year, @mon, @day, @hour, -1) }
      assert_raise(ArgumentError){ Time.utc(@year, @mon, @day, @hour, @min, -1) }
      #assert_raise(ArgumentError){ Time.utc(@year, @mon, @day, @hour, @min, @sec, -1) }
   end

   def teardown
      @year = nil
      @mon  = nil
      @day  = nil
      @hour = nil
      @min  = nil
      @sec  = nil
      @gmt  = nil
   end
end
