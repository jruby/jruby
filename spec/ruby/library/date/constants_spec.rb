require 'date'
require File.expand_path('../../../spec_helper', __FILE__)

describe "Date constants" do

  # Fixes in 1.8.7
  ruby_bug "#", "1.8.6" do
    it "defines JULIAN" do
      (Date::JULIAN <=> Date::Infinity.new).should == 0
    end
  end

  # Fixed in 1.8.7
  ruby_bug "#", "1.8.6" do
    it "defines GREGORIAN" do
      (Date::GREGORIAN <=> -Date::Infinity.new).should == 0
    end
  end

  it "defines ITALY" do
    Date::ITALY.should == 2299161 # 1582-10-15
  end

  it "defines ENGLAND" do
    Date::ENGLAND.should == 2361222 # 1752-09-14
  end

  it "defines MONTHNAMES" do
    Date::MONTHNAMES.should == [nil] + %w(January February March April May June July
                                          August September October November December)
  end

  it "defines DAYNAMES" do
    Date::DAYNAMES.should == %w(Sunday Monday Tuesday Wednesday Thursday Friday Saturday)
  end

  it "defines ABBR_MONTHNAMES" do
    Date::ABBR_DAYNAMES.should == %w(Sun Mon Tue Wed Thu Fri Sat)
  end

  it "freezes MONTHNAMES, DAYNAMES, ABBR_MONTHNAMES, ABBR_DAYSNAMES" do
    [Date::MONTHNAMES, Date::DAYNAMES, Date::ABBR_MONTHNAMES, Date::ABBR_DAYNAMES].each do |ary|
      lambda { ary << "Unknown" }.should raise_error
      ary.compact.each do |name|
        lambda { name << "modified" }.should raise_error
      end
    end
  end

  ruby_version_is "" ... "1.8.7" do
    it "defines UNIXEPOCH" do
      Date::UNIXEPOCH.should == 2440588
    end
  end

  ruby_version_is "1.8.7" ... "1.9.3"do

    it "defines HALF_DAYS_IN_DAY" do
      Date::HALF_DAYS_IN_DAY.should == Rational(1, 2)
    end

    it "defines HOURS_IN_DAY" do
      Date::HOURS_IN_DAY.should == Rational(1, 24)
    end

    it "defines MINUTES_IN_DAY" do
      Date::MINUTES_IN_DAY.should == Rational(1, 1440)
    end

    it "defines SECONDS_IN_DAY" do
      Date::SECONDS_IN_DAY.should == Rational(1, 86400)
    end

    it "defines MILLISECONDS_IN_DAY" do
      Date::MILLISECONDS_IN_DAY.should == Rational(1, 86400*10**3)
    end

    it "defines NANOSECONDS_IN_DAY" do
      Date::NANOSECONDS_IN_DAY.should == Rational(1, 86400*10**9)
    end

    it "defines MILLISECONDS_IN_SECOND" do
      Date::MILLISECONDS_IN_SECOND.should == Rational(1, 10**3)
    end

    it "defines NANOSECONDS_IN_SECOND" do
      Date::NANOSECONDS_IN_SECOND.should == Rational(1, 10**9)
    end

    it "defines MJD_EPOCH_IN_AJD" do
      Date::MJD_EPOCH_IN_AJD.should == Rational(4800001, 2) # 1858-11-17
    end

    it "defines UNIX_EPOCH_IN_AJD" do
      Date::UNIX_EPOCH_IN_AJD.should == Rational(4881175, 2) # 1970-01-01
    end

    it "defines MJD_EPOCH_IN_CJD" do
      Date::MJD_EPOCH_IN_CJD.should == 2400001
    end

    it "defines UNIX_EPOCH_IN_CJD" do
      Date::UNIX_EPOCH_IN_CJD.should == 2440588
    end

    it "defines LD_EPOCH_IN_CJD" do
      Date::LD_EPOCH_IN_CJD.should == 2299160
    end
  end

end
