require File.expand_path('../../../spec_helper', __FILE__)
require 'date'

ruby_version_is "1.9.1" do
  describe "DateTime#strftime" do
    it "shows the number of fractional seconds with leading zeroes" do
      DateTime.civil(2000, 4, 6).strftime("%N").should == "000000000"
    end

    it "shows the number of fractional seconds with leading zeroes to two decimal places" do
      DateTime.civil(2000, 4, 6).strftime("%2N").should == "00"
    end

    it "shows the number of fractional milliseconds with leading zeroes" do
      DateTime.civil(2000, 4, 6).strftime("%3N").should == "000"
    end

    it "shows the number of fractional microseconds with leading zeroes" do
      DateTime.civil(2000, 4, 6).strftime("%6N").should == "000000"
    end

    it "shows the number of fractional nanoseconds with leading zeroes" do
      DateTime.civil(2000, 4, 6).strftime("%9N").should == "000000000"
    end
  end
end
