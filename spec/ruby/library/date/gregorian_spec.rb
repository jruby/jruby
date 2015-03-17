require File.expand_path('../../../spec_helper', __FILE__)
require 'date'

describe "Date#gregorian?" do

  it "marks a day before the calendar reform as Julian" do
    Date.civil(1007, 2, 27).gregorian?.should be_false
    Date.civil(1907, 2, 27, Date.civil(1930, 1, 1).jd).gregorian?.should be_false
  end

  it "marks a day after the calendar reform as Julian" do
    Date.civil(2007, 2, 27).gregorian?.should == true
    Date.civil(1607, 2, 27, Date.civil(1582, 1, 1).jd).gregorian?.should be_true
  end

end

ruby_version_is "" ... "1.9" do
  describe "Date.gregorian?" do

    it "marks the date as Gregorian if using the Gregorian calendar" do
      Date.gregorian?(Date.civil(1007, 2, 27).jd, Date::GREGORIAN).should be_true
    end

    it "marks the date as not Gregorian if using the Julian calendar" do
      Date.gregorian?(Date.civil(1007, 2, 27).jd, Date::JULIAN).should be_false
    end

    it "marks the date before the English Day of Calendar Reform as not Gregorian" do
      Date.gregorian?(Date.civil(1752, 9, 13).jd, Date::ENGLAND).should be_false
    end

    it "marks the date after the English Day of Calendar Reform as Gregorian" do
      Date.gregorian?(Date.civil(1752, 9, 14).jd, Date::ENGLAND).should be_true
    end

    it "marks the date before the Italian Day of Calendar Reform as not Gregorian" do
      Date.gregorian?(Date.civil(1582, 10, 4).jd, Date::ITALY).should be_false
    end

    it "marks the date after the Italian Day of Calendar Reform as Gregorian" do
      Date.gregorian?(Date.civil(1582, 10, 15).jd, Date::ITALY).should be_true
    end

  end

end
