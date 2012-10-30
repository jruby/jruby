require 'date'
require File.expand_path('../../../spec_helper', __FILE__)


describe "Date#new_start" do
  it "converts a date object into another with a new calendar reform" do
    Date.civil(1582, 10, 14, Date::ENGLAND).new_start.should == Date.civil(1582, 10, 24)
    Date.civil(1582, 10,  4, Date::ENGLAND).new_start.should == Date.civil(1582, 10,  4)
    Date.civil(1582, 10, 15).new_start(Date::ENGLAND).should == Date.civil(1582, 10,  5, Date::ENGLAND)
    Date.civil(1752,  9, 14).new_start(Date::ENGLAND).should == Date.civil(1752,  9, 14, Date::ENGLAND)
    Date.civil(1752,  9, 13).new_start(Date::ENGLAND).should == Date.civil(1752,  9,  2, Date::ENGLAND)
  end
end

describe "Date#italy" do
  it "converts a date object into another with the Italian calendar reform" do
    Date.civil(1582, 10, 14, Date::ENGLAND).italy.should == Date.civil(1582, 10, 24)
    Date.civil(1582, 10,  4, Date::ENGLAND).italy.should == Date.civil(1582, 10,  4)
  end
end

describe "Date#england" do
  it "converts a date object into another with the English calendar reform" do
    Date.civil(1582, 10, 15).england.should == Date.civil(1582, 10,  5, Date::ENGLAND)
    Date.civil(1752,  9, 14).england.should == Date.civil(1752,  9, 14, Date::ENGLAND)
    Date.civil(1752,  9, 13).england.should == Date.civil(1752,  9,  2, Date::ENGLAND)
  end
end

describe "Date#julian" do
  it "converts a date object into another with the Julian calendar" do
    Date.civil(1582, 10, 15).julian.should == Date.civil(1582, 10,  5, Date::JULIAN)
    Date.civil(1752,  9, 14).julian.should == Date.civil(1752,  9,  3, Date::JULIAN)
    Date.civil(1752,  9, 13).julian.should == Date.civil(1752,  9,  2, Date::JULIAN)
  end
end

describe "Date#gregorian" do
  it "converts a date object into another with the Gregorian calendar" do
    Date.civil(1582, 10,  4).gregorian.should == Date.civil(1582, 10, 14, Date::GREGORIAN)
    Date.civil(1752,  9, 14).gregorian.should == Date.civil(1752,  9, 14, Date::GREGORIAN)
  end
end

ruby_version_is "" ... "1.9" do
  describe "Date#ordinal_to_jd" do
    it "converts an ordinal date (year-day) to a Julian day number" do
      Date.ordinal_to_jd(2007, 55).should == 2454156
    end
  end

  describe "Date#jd_to_ordinal" do
    it "converts a Julian day number into an ordinal date" do
      Date.jd_to_ordinal(2454156).should == [2007, 55]
    end
  end

  describe "Date#civil_to_jd" do
    it "converts a civil date into a Julian day number" do
      Date.civil_to_jd(2007, 2, 24).should == 2454156
    end
  end

  describe "Date#jd_to_civil" do
    it "converts a Julian day into a civil date" do
      Date.jd_to_civil(2454156).should == [2007, 2, 24]
    end
  end

  describe "Date#commercial_to_jd" do
    it "converts a commercial date (year - week - day of week) into a Julian day number" do
      Date.commercial_to_jd(2007, 45, 1).should == 2454410
    end
  end

  describe "Date#jd_to_commercial" do
    it "converts a Julian day number into a commercial date" do
      Date.jd_to_commercial(2454410).should == [2007, 45, 1]
    end
  end

  describe "Date#ajd_to_jd" do
    it "converts a Astronomical Julian day number into a Julian day number" do
      Date.ajd_to_jd(2454410).should == [2454410, Rational(1,2)]
      Date.ajd_to_jd(2454410, 1.to_r / 2).should == [2454411, 0]
    end
  end

  describe "Date#jd_to_ajd" do
    it "converts a Julian day number into a Astronomical Julian day number" do
      Date.jd_to_ajd(2454410, 0).should == 2454410 - Rational(1, 2)
      Date.jd_to_ajd(2454410, 1.to_r / 2).should == 2454410
    end
  end

  describe "Date#day_fraction_to_time" do
    it "converts a day fraction into time" do
      Date.day_fraction_to_time(2).should == [48, 0, 0, 0]
      Date.day_fraction_to_time(1).should == [24, 0, 0, 0]
      Date.day_fraction_to_time(1.to_r / 2).should == [12, 0, 0, 0]
      Date.day_fraction_to_time(1.to_r / 7).should == [3, 25, 42, 1.to_r / 100800]
    end
  end

  describe "Date#time_to_day_fraction" do
    it "converts a time into a day fraction" do
      Date.time_to_day_fraction(48, 0, 0).should == 2
      Date.time_to_day_fraction(24, 0, 0).should == 1
      Date.time_to_day_fraction(12, 0, 0).should == 1.to_r / 2
      Date.time_to_day_fraction(10, 20, 10).should == 10.to_r / 24 + 20.to_r / (24 * 60) + 10.to_r / (24 * 60 * 60)
    end
  end

  describe "Date#amjd_to_ajd" do
    it "converts Astronomical Modified Julian day numbers into Astronomical Julian day numbers" do
      Date.amjd_to_ajd(10).should == 10 + 2400000 + 1.to_r / 2
    end
  end

  describe "Date#ajd_to_amjd" do
    it "converts Astronomical Julian day numbers into Astronomical Modified Julian day numbers" do
      Date.ajd_to_amjd(10000010).should == 10000010 - 2400000 - 1.to_r / 2
    end
  end

  describe "Date#mjd_to_jd" do
    it "converts Modified Julian day numbers into Julian day numbers" do
      Date.mjd_to_jd(2000).should == 2000 + 2400001
    end
  end

  describe "Date#jd_to_mjd" do
    it "converts Julian day numbers into Modified Julian day numbers" do
      Date.jd_to_mjd(2500000).should == 2500000 - 2400001
    end
  end

  describe "Date#ld_to_jd" do
    it "converts the number of days since the Gregorian calendar in Italy into Julian day numbers" do
      Date.ld_to_jd(450000).should == 450000 + 2299160
    end
  end

  describe "Date#jd_to_ld" do
    it "converts Julian day numbers into the number of days since the Gregorian calendar in Italy" do
      Date.jd_to_ld(2450000).should == 2450000 - 2299160
    end
  end

  describe "Date#jd_to_wday" do
    it "converts a Julian day number into a week day number" do
      Date.jd_to_wday(2454482).should == 3
    end
  end
end
