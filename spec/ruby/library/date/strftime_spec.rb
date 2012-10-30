require 'date'
require File.expand_path('../../../spec_helper', __FILE__)

describe "Date#strftime" do

  it "should be able to print the date" do
    Date.civil(2000, 4, 6).strftime.should == "2000-04-06"
    Date.civil(2000, 4, 6).strftime.should == Date.civil(2000, 4, 6).to_s
  end

  it "should be able to print the full day name" do
    Date.civil(2000, 4, 6).strftime("%A").should == "Thursday"
  end

  it "should be able to print the short day name" do
    Date.civil(2000, 4, 6).strftime("%a").should == "Thu"
  end

  it "should be able to print the full month name" do
    Date.civil(2000, 4, 6).strftime("%B").should == "April"
  end

  it "should be able to print the short month name" do
    Date.civil(2000, 4, 6).strftime("%b").should == "Apr"
    Date.civil(2000, 4, 6).strftime("%h").should == "Apr"
    Date.civil(2000, 4, 6).strftime("%b").should == Date.civil(2000, 4, 6).strftime("%h")
  end

  it "should be able to print the century" do
    Date.civil(2000, 4, 6).strftime("%C").should == "20"
  end

  it "should be able to print the month day with leading zeroes" do
    Date.civil(2000, 4, 6).strftime("%d").should == "06"
  end

  it "should be able to print the month day with leading spaces" do
    Date.civil(2000, 4, 6).strftime("%e").should == " 6"
  end

  it "should be able to print the commercial year with leading zeroes" do
    Date.civil(2000, 4, 6).strftime("%G").should == "2000"
    Date.civil( 200, 4, 6).strftime("%G").should == "0200"
  end

  it "should be able to print the commercial year with only two digits" do
    Date.civil(2000, 4, 6).strftime("%g").should == "00"
    Date.civil( 200, 4, 6).strftime("%g").should == "00"
  end

  it "should be able to print the hour with leading zeroes (hour is always 00)" do
    Date.civil(2000, 4, 6).strftime("%H").should == "00"
  end

  it "should be able to print the hour in 12 hour notation with leading zeroes" do
    Date.civil(2000, 4, 6).strftime("%I").should == "12"
  end

  it "should be able to print the year day with leading zeroes" do
    Date.civil(2000, 4, 6).strftime("%j").should == "097"
  end

  it "should be able to print the hour in 24 hour notation with leading spaces" do
    Date.civil(2000, 4, 6).strftime("%k").should == " 0"
  end

  it "should be able to print the hour in 12 hour notation with leading spaces" do
    Date.civil(2000, 4, 6).strftime("%l").should == "12"
  end

  it "should be able to print the minutes with leading zeroes" do
    Date.civil(2000, 4, 6).strftime("%M").should == "00"
  end

  it "should be able to print the month with leading zeroes" do
    Date.civil(2000, 4, 6).strftime("%m").should == "04"
  end

  it "should be able to add a newline" do
    Date.civil(2000, 4, 6).strftime("%n").should == "\n"
  end

  it "should be able to show AM/PM" do
    Date.civil(2000, 4, 6).strftime("%P").should == "am"
  end

  it "should be able to show am/pm" do
    Date.civil(2000, 4, 6).strftime("%p").should == "AM"
  end

  it "should be able to show the number of seconds with leading zeroes" do
    Date.civil(2000, 4, 6).strftime("%S").should == "00"
  end

  it "should be able to show the number of seconds with leading zeroes" do
    Date.civil(2000, 4, 6).strftime("%S").should == "00"
  end

  it "should be able to show the number of seconds since the unix epoch" do
    Date.civil(2000, 4, 6).strftime("%s").should == "954979200"
  end

  it "should be able to add a tab" do
    Date.civil(2000, 4, 6).strftime("%t").should == "\t"
  end

  it "should be able to show the week number with the week starting on sunday and monday" do
    Date.civil(2000, 4, 6).strftime("%U").should == "14"
    Date.civil(2000, 4, 6).strftime("%W").should == "14"
    Date.civil(2000, 4, 6).strftime("%U").should == Date.civil(2000, 4, 6).strftime("%W")
    Date.civil(2000, 4, 9).strftime("%U").should == "15"
    Date.civil(2000, 4, 9).strftime("%W").should == "14"
    Date.civil(2000, 4, 9).strftime("%U").should_not == Date.civil(2000, 4, 9).strftime("%W")
  end

  it "should be able to show the commercial week day" do
    Date.civil(2000, 4,  9).strftime("%u").should == "7"
    Date.civil(2000, 4, 10).strftime("%u").should == "1"
  end

  it "should be able to show the commercial week" do
    Date.civil(2000, 4,  9).strftime("%V").should == "14"
    Date.civil(2000, 4, 10).strftime("%V").should == "15"
  end

  it "should be able to show the week day" do
    Date.civil(2000, 4,  9).strftime("%w").should == "0"
    Date.civil(2000, 4, 10).strftime("%w").should == "1"
  end

  it "should be able to show the year in YYYY format" do
    Date.civil(2000, 4,  9).strftime("%Y").should == "2000"
  end

  it "should be able to show the year in YY format" do
    Date.civil(2000, 4,  9).strftime("%y").should == "00"
  end

  it "should be able to show the timezone of the date with a : separator" do
    Date.civil(2000, 4,  9).strftime("%Z").should == "+00:00"
  end

  it "should be able to show the timezone of the date with a : separator" do
    Date.civil(2000, 4,  9).strftime("%z").should == "+0000"
  end

  it "should be able to escape the % character" do
    Date.civil(2000, 4,  9).strftime("%%").should == "%"
  end

  ############################
  # Specs that combine stuff #
  ############################

  it "should be able to print the date in full" do
    Date.civil(2000, 4, 6).strftime("%c").should == "Thu Apr  6 00:00:00 2000"
    Date.civil(2000, 4, 6).strftime("%c").should == Date.civil(2000, 4, 6).strftime('%a %b %e %H:%M:%S %Y')
  end

  it "should be able to print the date with slashes" do
    Date.civil(2000, 4, 6).strftime("%D").should == "04/06/00"
    Date.civil(2000, 4, 6).strftime("%D").should == Date.civil(2000, 4, 6).strftime('%m/%d/%y')
  end

  it "should be able to print the date as YYYY-MM-DD" do
    Date.civil(2000, 4, 6).strftime("%F").should == "2000-04-06"
    Date.civil(2000, 4, 6).strftime("%F").should == Date.civil(2000, 4, 6).strftime('%Y-%m-%d')
  end

  it "should be able to show HH:MM" do
    Date.civil(2000, 4, 6).strftime("%R").should == "00:00"
    Date.civil(2000, 4, 6).strftime("%R").should == Date.civil(2000, 4, 6).strftime('%H:%M')
  end

  it "should be able to show HH:MM:SS AM/PM" do
    Date.civil(2000, 4, 6).strftime("%r").should == "12:00:00 AM"
    Date.civil(2000, 4, 6).strftime("%r").should == Date.civil(2000, 4, 6).strftime('%I:%M:%S %p')
  end

  it "should be able to show HH:MM:SS" do
    Date.civil(2000, 4, 6).strftime("%T").should == "00:00:00"
    Date.civil(2000, 4, 6).strftime("%T").should == Date.civil(2000, 4, 6).strftime('%H:%M:%S')
  end

  it "should be able to show the commercial week" do
    Date.civil(2000, 4,  9).strftime("%v").should == " 9-Apr-2000"
    Date.civil(2000, 4,  9).strftime("%v").should == Date.civil(2000, 4,  9).strftime('%e-%b-%Y')
  end

  it "should be able to show HH:MM:SS" do
    Date.civil(2000, 4, 6).strftime("%X").should == "00:00:00"
    Date.civil(2000, 4, 6).strftime("%X").should == Date.civil(2000, 4, 6).strftime('%H:%M:%S')
  end

  it "should be able to show MM/DD/YY" do
    Date.civil(2000, 4, 6).strftime("%x").should == "04/06/00"
    Date.civil(2000, 4, 6).strftime("%x").should == Date.civil(2000, 4, 6).strftime('%m/%d/%y')
  end

  it "should be able to show a full notation" do
    Date.civil(2000, 4,  9).strftime("%+").should == "Sun Apr  9 00:00:00 +00:00 2000"
    Date.civil(2000, 4,  9).strftime("%+").should == Date.civil(2000, 4,  9).strftime('%a %b %e %H:%M:%S %Z %Y')
  end

end
