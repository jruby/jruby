require File.expand_path('../../../spec_helper', __FILE__)
require 'date'

describe "Date.civil_to_jd" do

  ruby_version_is "" ... "1.9" do
    it "converts a Civil Date to a Julian Day Number" do
      Date.civil_to_jd(-4713, 11, 24).should == 0
      Date.civil_to_jd(2011, 6, 8).should == 2455721
    end

    it "converts a Civil Date to a Julian Day Number using Julian calendar" do
      Date.civil_to_jd(2011, 6, 8, Date::JULIAN).should == 2455734
    end
  end

end
