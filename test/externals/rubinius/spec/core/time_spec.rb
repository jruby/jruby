require File.dirname(__FILE__) + '/../spec_helper'

# class methods
# _load, apply_offset, at, gm, httpdate, local, make_time, mktime,
# month_days, new, now, parse, rfc2822, times, utc, xmlschema,
# zone_offset, zone_utc?

# +, -, <=>, _dump, asctime, ctime, day, dst?, eql?, getgm, getlocal,
# getutc, gmt?, gmt_offset, gmtime, gmtoff, hash, hour, httpdate,
# inspect, isdst, iso8601, localtime, marshal_dump, marshal_load,
# mday, min, mon, month, rfc2822, rfc822, sec, strftime, succ, to_a,
# to_f, to_i, to_s, tv_sec, tv_usec, usec, utc, utc?, utc_offset,
# wday, xmlschema, yday, year, zone

context "Time class method" do
  setup do
    @origtz = ENV['TZ']
    ENV['TZ'] = 'GMT'
  end

  specify "at should convert to time object" do
    Time.at( 1168475924 ).inspect.should == "Thu Jan 11 00:38:44 +0000 2007"
  end
  
  specify "at should create a new time object with the value given by time" do
    Time.at(Time.now).inspect.should == Time.now.inspect
  end
  
  specify "gm should create a time based on given values, interpreted as UTC (GMT)" do
    Time.gm(2000,"jan",1,20,15,1).inspect.should == "Sat Jan 01 20:15:01 UTC 2000"
  end
  
  specify "utc is a synonym for Time.gm" do
    Time.utc(2000,"jan",1,20,15,1).inspect.should == "Sat Jan 01 20:15:01 UTC 2000"
  end
  
  specify "local should create a time based on given values, interpreted in the local time zone" do
    Time.local(2000,"jan",1,20,15,1).inspect.should == "Sat Jan 01 20:15:01 +0000 2000"
  end
  
  specify "mktime is a synonym for Time.local" do
    Time.mktime(2000,"jan",1,20,15,1).inspect.should == "Sat Jan 01 20:15:01 +0000 2000"
  end
end

context "Time instance method" do
  specify "asctime should return a canonical string representation of time" do
    Time.now.asctime.should == Time.now.strftime("%a %b %d %H:%M:%S %Y")
  end
  
  specify "ctime is a synonym for Time#acstime " do
    Time.now.ctime.should == Time.now.asctime
  end
  
  specify "sec should return the second of the minute(0..60) for time" do
    Time.at(0).sec.should == 0
  end
  
  specify "to_i should return the value of time as an integer number of seconds since epoch" do
    Time.at(0).to_i.should == 0
  end
  
  specify "tv_sec is a synonym for Time#to_i" do
    Time.at(0).tv_sec.should == Time.at(0).to_i
  end
  
  specify "hour should return the hour of the day (0..23) for time" do
    Time.at(0).hour.should == 0
  end
  
  specify "min should return the minute of the hour (0..59) for time" do
    Time.at(0).min.should == 0
  end
  
  specify "day should return the day of the month (1..n) for time" do
    Time.at(0).day.should == 1
  end
  
  specify "mday is a synonym for Time#day" do
    Time.new.mday.should == Time.new.day
  end
  
  specify "<=> should return 1 if the first argument is a point in time after the second argument" do
    (Time.now <=> Time.at(0)).should == 1
  end
  
  specify "<=> should return -1 if the first argument is a point in time before the second argument" do
    (Time.at(0) <=> Time.now).should == -1
  end
  
  specify "utc? should return true if time represents a time in UTC (GMT)" do
    Time.now.utc?.should == false
  end
  
  specify "year should return the name year for time" do
    Time.at(0).year.should == 1970
  end
  
  specify "strftime should format time according to the directives in the given format string" do
    Time.at(0).strftime("There is %M minutes in epoch").should == "There is 00 minutes in epoch"
  end
  
  specify "wday should return an integer representing the day of the week, 0..6, with Sunday being 0" do
    Time.at(0).wday.should == 4
  end
  
  specify "yday should return an integer representing the day of the year, 1..366" do
    Time.at(9999999).yday.should == 116
  end
  
  specify "zone should return the time zone used for time" do
    Time.now.zone.should == 'GMT'
  end
  
  specify "mon should return the month of the year" do
    Time.at(99999).mon.should == 1
  end
  
  specify "month is a synonym for Time#mon" do
    Time.at(99999).month.should == Time.at(99999).mon
  end
  
  specify "gmt? should return true is time represents a time in UTC (GMT)" do
    Time.now.gmt?.should == false
  end
  
  specify "usec should return the microseconds for time" do
    Time.at(0).usec.should == 0
  end
  
  specify "gmt_offset should return the offset in seconds between the timezone of time and UTC" do
    Time.new.gmt_offset.should == 0
  end
end

