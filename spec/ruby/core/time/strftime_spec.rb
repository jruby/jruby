# encoding: utf-8

require File.expand_path('../../../spec_helper', __FILE__)

describe "Time#strftime" do
  it "formats time according to the directives in the given format string" do
    with_timezone("GMT", 0) do
      Time.at(0).strftime("There is %M minutes in epoch").should == "There is 00 minutes in epoch"
    end
  end

  it "supports week of year format with %U and %W" do
    # start of the yer
    saturday_first = Time.local(2000,1,1,14,58,42)
    saturday_first.strftime("%U").should == "00"
    saturday_first.strftime("%W").should == "00"

    sunday_second = Time.local(2000,1,2,14,58,42)
    sunday_second.strftime("%U").should == "01"
    sunday_second.strftime("%W").should == "00"

    monday_third = Time.local(2000,1,3,14,58,42)
    monday_third.strftime("%U").should == "01"
    monday_third.strftime("%W").should == "01"

    sunday_9th = Time.local(2000,1,9,14,58,42)
    sunday_9th.strftime("%U").should == "02"
    sunday_9th.strftime("%W").should == "01"

    monday_10th = Time.local(2000,1,10,14,58,42)
    monday_10th.strftime("%U").should == "02"
    monday_10th.strftime("%W").should == "02"

    # middle of the year
    some_sunday = Time.local(2000,8,6,4,20,00)
    some_sunday.strftime("%U").should == "32"
    some_sunday.strftime("%W").should == "31"
    some_monday = Time.local(2000,8,7,4,20,00)
    some_monday.strftime("%U").should == "32"
    some_monday.strftime("%W").should == "32"

    # end of year, and start of next one
    saturday_30th = Time.local(2000,12,30,14,58,42)
    saturday_30th.strftime("%U").should == "52"
    saturday_30th.strftime("%W").should == "52"

    sunday_last = Time.local(2000,12,31,14,58,42)
    sunday_last.strftime("%U").should == "53"
    sunday_last.strftime("%W").should == "52"

    monday_first = Time.local(2001,1,1,14,58,42)
    monday_first.strftime("%U").should == "00"
    monday_first.strftime("%W").should == "01"
  end

  it "supports mm/dd/yy formatting with %D" do
    now = Time.now
    mmddyy = now.strftime('%m/%d/%y')
    now.strftime('%D').should == mmddyy
  end

  it "supports HH:MM:SS formatting with %T" do
    now = Time.now
    hhmmss = now.strftime('%H:%M:%S')
    now.strftime('%T').should == hhmmss
  end

  it "supports 12-hr formatting with %l" do
    time = Time.local(2004, 8, 26, 22, 38, 3)
    time.strftime('%l').should == '10'
    morning_time = Time.local(2004, 8, 26, 6, 38, 3)
    morning_time.strftime('%l').should == ' 6'
  end

  it "supports AM/PM formatting with %p" do
    time = Time.local(2004, 8, 26, 22, 38, 3)
    time.strftime('%p').should == 'PM'
    time = Time.local(2004, 8, 26, 11, 38, 3)
    time.strftime('%p').should == 'AM'
  end

  it "returns the abbreviated weekday with %a" do
    time = Time.local(2009, 9, 18, 12, 0, 0)
    time.strftime('%a').should == 'Fri'
  end

  it "returns the full weekday with %A" do
    time = Time.local(2009, 9, 18, 12, 0, 0)
    time.strftime('%A').should == 'Friday'
  end

  it "returns the abbreviated month with %b" do
    time = Time.local(2009, 9, 18, 12, 0, 0)
    time.strftime('%b').should == 'Sep'
  end

  it "returns the full month with %B" do
    time = Time.local(2009, 9, 18, 12, 0, 0)
    time.strftime('%B').should == 'September'
  end

  it "returns the day of the month with %d" do
    time = Time.local(2009, 9, 18, 12, 0, 0)
    time.strftime('%d').should == '18'
  end

  it "returns the 24-based hour with %H" do
    time = Time.local(2009, 9, 18, 18, 0, 0)
    time.strftime('%H').should == '18'
  end

  it "returns the 12-based hour with %I" do
    time = Time.local(2009, 9, 18, 18, 0, 0)
    time.strftime('%I').should == '06'
  end

  it "returns the Julian date with %j" do
    time = Time.local(2009, 9, 18, 18, 0, 0)
    time.strftime('%j').should == '261'
  end

  describe "with %L" do
    it "formats the milliseconds of a second" do
      Time.local(2009, 1, 1, 0, 0, Rational(100, 1000)).strftime("%L").should == "100"
      Time.local(2009, 1, 1, 0, 0, Rational(10, 1000)).strftime("%L").should == "010"
      Time.local(2009, 1, 1, 0, 0, Rational(1, 1000)).strftime("%L").should == "001"
      Time.local(2009, 1, 1, 0, 0, Rational(1, 10000)).strftime("%L").should == "000"
    end
  end

  it "returns the month with %m" do
    time = Time.local(2009, 9, 18, 12, 0, 0)
    time.strftime('%m').should == '09'
  end

  it "returns the minute with %M" do
    time = Time.local(2009, 9, 18, 12, 6, 0)
    time.strftime('%M').should == '06'
  end

  describe "with %N" do
    it "formats the nanoseconds of of the second with %N" do
      Time.local(2009, 1, 1, 0, 0, Rational(999999999, 1000000000)).strftime("%N").should == "999999999"
    end

    it "formats the milliseconds of of the second with %3N" do
      Time.local(2009, 1, 1, 0, 0, Rational(999, 1000)).strftime("%3N").should == "999"
    end

    it "formats the microseconds of of the second with %6N" do
      Time.local(2009, 1, 1, 0, 0, Rational(999999, 1000000)).strftime("%6N").should == "999999"
    end

    it "formats the nanoseconds of of the second with %9N" do
      Time.local(2009, 1, 1, 0, 0, Rational(999999999, 1000000000)).strftime("%9N").should == "999999999"
    end

    it "formats the picoseconds of of the second with %12N" do
      Time.local(2009, 1, 1, 0, 0, Rational(999999999999, 1000000000000)).strftime("%12N").should == "999999999999"
    end
  end

  it "returns the second with %S" do
    time = Time.local(2009, 9, 18, 12, 0, 6)
    time.strftime('%S').should == '06'
  end

  it "returns the enumerated day of the week with %w" do
    time = Time.local(2009, 9, 18, 12, 0, 0)
    time.strftime('%w').should == '5'
  end

  it "returns the date alone with %x" do
    time = Time.local(2009, 9, 18, 12, 0, 6)
    time.strftime('%x').should == '09/18/09'
  end

  it "returns the time alone with %X" do
    time = Time.local(2009, 9, 18, 12, 0, 6)
    time.strftime('%X').should == '12:00:06'
  end

  it "returns the year wihout a century with %y" do
    time = Time.local(2009, 9, 18, 12, 0, 0)
    time.strftime('%y').should == '09'
  end

  it "returns the year with %Y" do
    time = Time.local(2009, 9, 18, 12, 0, 0)
    time.strftime('%Y').should == '2009'
  end

  describe "with %z" do
    it "formats a UTC time offset as '+0000'" do
      Time.utc(2005).strftime("%z").should == "+0000"
    end

    it "formats a local time with positive UTC offset as '+HHMM'" do
      with_timezone("CET", 1) do
        Time.local(2005).strftime("%z").should == "+0100"
      end
    end

    it "formats a local time with negative UTC offset as '-HHMM'" do
      with_timezone("PST", -8) do
        Time.local(2005).strftime("%z").should == "-0800"
      end
    end

    it "formats a time with fixed positive offset as '+HHMM'" do
      Time.new(2012, 1, 1, 0, 0, 0, 3660).strftime("%z").should == "+0101"
    end

    it "formats a time with fixed negative offset as '-HHMM'" do
      Time.new(2012, 1, 1, 0, 0, 0, -3660).strftime("%z").should == "-0101"
    end

    it "formats a time with fixed offset as '+/-HH:MM' with ':' specifier" do
      Time.new(2012, 1, 1, 0, 0, 0, 3660).strftime("%:z").should == "+01:01"
    end

    it "formats a time with fixed offset as '+/-HH:MM:SS' with '::' specifier" do
      Time.new(2012, 1, 1, 0, 0, 0, 3665).strftime("%::z").should == "+01:01:05"
    end

    it "rounds fixed offset to the nearest second" do
      Time.new(2012, 1, 1, 0, 0, 0, Rational(36645, 10)).strftime("%::z").should == "+01:01:05"
    end
  end

  it "returns the timezone with %Z" do
    time = Time.local(2009, 9, 18, 12, 0, 0)
    zone = time.zone
    time.strftime("%Z").should == zone
  end

  it "supports am/pm formatting with %P" do
    time = Time.local(2004, 8, 26, 22, 38, 3)
    time.strftime('%P').should == 'pm'
    time = Time.local(2004, 8, 26, 11, 38, 3)
    time.strftime('%P').should == 'am'
  end

  it "returns the fractional seconds digits, default is 9 digits (nanosecond) with %N" do
    time = Time.local(2009, 9, 18, 12, 0, 6, 123456)
    time.strftime('%N').should == '123456000'
  end

  it "supports GNU modificators" do
    time = Time.local(2001, 2, 3, 4, 5, 6)

    time.strftime('%^h').should == 'FEB'
    time.strftime('%^_5h').should == '  FEB'
    time.strftime('%0^5h').should == '00FEB'
    time.strftime('%04H').should == '0004'
    time.strftime('%0-^5h').should == 'FEB'
    time.strftime('%_-^5h').should == 'FEB'
    time.strftime('%^ha').should == 'FEBa'

    expected = {
      "%10h" => '       Feb',
      "%^10h" => '       FEB',
      "%_10h" => '       Feb',
      "%_010h" => '0000000Feb',
      "%0_10h" => '       Feb',
      "%0_-10h" => 'Feb',
      "%0-_10h" => 'Feb'
    }

    ["%10h","%^10h","%_10h","%_010h","%0_10h","%0_-10h","%0-_10h"].each do |format|
      time.strftime(format).should == expected[format]
    end
  end

  it "supports the '-' modifier to drop leading zeros" do
    time = Time.local(2001,1,1,14,01,42)
    time.strftime("%-m/%-d/%-y %-I:%-M %p").should == "1/1/1 2:1 PM"

    time = Time.local(2010,10,10,12,10,42)
    time.strftime("%-m/%-d/%-y %-I:%-M %p").should == "10/10/10 12:10 PM"
  end

  it "supports the '-' modifier for padded format directives" do
    time = Time.local(2010, 8, 8, 8, 10, 42)
    time.strftime("%-e").should == "8"
    time.strftime("%-k%p").should == "8AM"
    time.strftime("%-l%p").should == "8AM"
  end

  with_feature :encoding do
    it "passes the format string's encoding to the result string" do
      time = Time.local(2010, 8, 8, 8, 10, 42)
      result = time.strftime("%d. März %Y %H:%M")

      result.encoding.should == Encoding::UTF_8
      result.should == "08. März 2010 08:10"
    end
  end
end
