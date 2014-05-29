require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do
  require "parsedate"

  describe "ParseDate.parsedate" do
    it "returns Array of 8 nils given an empty String" do
      ParseDate.parsedate("").should == [nil] * 8
    end

    it "raises TypeError given nil" do
      lambda { ParseDate.parsedate(nil) }.should raise_error(TypeError)
    end

    it "raises NoMethodError given Time" do
      lambda { ParseDate.parsedate(Time.now) }.should raise_error(NoMethodError)
    end

    it "returns Array with weekday number, given a full day name" do
      ParseDate.parsedate("Sunday").should == [nil] * 7 + [0]
    end

    it "returns Array with weekday number, given a 3 letter day name" do
      ParseDate.parsedate("mon").should == [nil] * 7 + [1]
    end

    it "returns Array with weekday number, given a String containing a word starting with day" do
      ParseDate.parsedate("ignore friday monday ignore").should == [nil] * 7 + [5]
      ParseDate.parsedate("ignorefriday").should == [nil] * 8
      ParseDate.parsedate("fridayignore").should == [nil] * 7 + [5]
      # friday, not monday!
      ParseDate.parsedate("friends on monday").should == [nil] * 7 + [5]
    end

    it "returns Array of 8 nils, given a single digit String" do
      ParseDate.parsedate("8").should == [nil] * 8
    end

    it "returns Array with day set, given a String of 2 digits" do
      ParseDate.parsedate("08").should == [nil, nil] + [8] + [nil] * 5
      ParseDate.parsedate("99").should == [nil, nil] + [99] + [nil] * 5
    end

    it "returns Array of 8 nils, given a String of 3 digits" do
      ParseDate.parsedate("100").should == [nil] * 8
    end

    it "returns Array with month and day set, given a String of 4 digits" do
      ParseDate.parsedate("1050").should == [nil] + [10,50] + [nil] * 5
    end

    it "returns Array with year set, given a String of 5 digits" do
      ParseDate.parsedate("10500").should == [10] + [nil] * 7
    end

    it "returns Array with date fields set, given a String of 6 digits" do
      ParseDate.parsedate("105070").should == [10, 50, 70] + [nil] * 5
    end

    it "returns Array with 4-digit year set, given a String of 7 digits" do
      ParseDate.parsedate("1050701").should == [1050] + [nil] * 7
    end

    it "returns Array with date fields set, given a String of 8 digits" do
      ParseDate.parsedate("10507011").should == [1050, 70, 11] + [nil] * 5
    end

    it "returns Array of 8 nils, given a odd-sized String of 9 or more digits" do
      ParseDate.parsedate("123456789").should == [nil] * 8
      ParseDate.parsedate("12345678901").should == [nil] * 8
    end

    it "returns Array with date & hour fields set, given a String of 10 digits" do
      ParseDate.parsedate("1234567890").should == [1234, 56, 78, 90] + [nil] * 4
    end

    it "returns Array with date, hour & minute fields set, given a String of 12 digits" do
      ParseDate.parsedate("123456789012").should == [1234, 56, 78, 90, 12] + [nil] * 3
    end

    it "returns Array with date & time fields set, given a String of 14 digits" do
      ParseDate.parsedate("12345678901234").should == [1234, 56, 78, 90, 12, 34, nil, nil]
    end

    ruby_version_is ""..."1.8.7" do
      it "returns Array with year and month set, given a String like nn-nn" do
        ParseDate.parsedate("08-09").should == [8,9] + [nil] * 6
        ParseDate.parsedate("08-09",true).should == [2008,9] + [nil] * 6
      end
    end

    ruby_version_is "1.8.7" do
      it "returns Array with year and month set, given a String like nn-nn" do
        ParseDate.parsedate("08-09").should == [nil, nil, 8, nil, nil, nil, "-09", nil]
        ParseDate.parsedate("08-09",true).should == [nil, nil, 8, nil, nil, nil, "-09", nil]
      end
    end

    it "returns Array with day and hour set, given a String like n-nn" do
      ParseDate.parsedate("8-09").should == [nil,nil] + [9,8] + [nil] * 4
    end

    it "returns Array with day and timezone set, given a String like nn-n" do
      ParseDate.parsedate("08-9").should == [nil,nil,8,nil,nil,nil,"-9",nil]
    end
  end
end
