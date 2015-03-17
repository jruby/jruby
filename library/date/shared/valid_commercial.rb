describe :date_valid_commercial?, :shared => true do
  ruby_version_is "" ... "1.9" do
    it "returns the corresponding Julian Day Number if it is a valid commercial date" do
      # October 1582 (the Gregorian calendar, Commercial Date in 1.8)
      #      M Tu  W Th  F Sa Su
      # 41:  -  -  -  -  5  6  7
      # 42:  1  2  3  4  5  6  7
      # 43:  1  2  3  4  5  6  7
      Date.send(@method, 1582, 41, 5).should == Date.civil(1582, 10, 15).jd
      Date.send(@method, 1752, 37, 4, Date::ENGLAND).should == Date.civil(1752, 9, 14, Date::ENGLAND).jd
    end

    it "returns nil if it is not a valid commercial date" do
      Date.send(@method, 1582, 41, 4).should be_nil
      # valid_commercial? can't handle dates before the Gregorian calendar
      Date.send(@method, 1582, 41, 4, Date::ENGLAND).should be_nil
    end

    it "handles negative week and day numbers" do
      # October 1582 (the Gregorian calendar, Commercial Date in 1.8)
      #       M Tu  W Th  F Sa Su
      # -12:  -  -  -  - -3 -2 -1
      # -11: -7 -6 -5 -4 -3 -2 -1
      # -10: -7 -6 -5 -4 -3 -2 -1
      Date.send(@method, 1582, -12, -4).should be_nil
      Date.send(@method, 1582, -12, -3).should == Date.civil(1582, 10, 15).jd

      Date.send(@method, 2007, -44, -2).should == Date.civil(2007, 3, 3).jd
      Date.send(@method, 2008, -44, -2).should == Date.civil(2008, 3, 1).jd
    end
  end

  ruby_version_is "1.9" do
    it "returns true if it is a valid commercial date" do
      # October 1582 (the Gregorian calendar, Commercial Date in 1.9)
      #      M Tu  W Th  F Sa Su
      # 39:  1  2  3  4  5  6  7
      # 40:  1  2  3  4  5  6  7
      # 41:  1  2  3  4  5  6  7
      Date.send(@method, 1582, 39, 4).should be_true
      Date.send(@method, 1582, 39, 5).should be_true
      Date.send(@method, 1582, 41, 4).should be_true
      Date.send(@method, 1582, 41, 5).should be_true
      Date.send(@method, 1582, 41, 4, Date::ENGLAND).should be_true
      Date.send(@method, 1752, 37, 4, Date::ENGLAND).should be_true
    end

    it "returns false it is not a valid commercial date" do
      Date.send(@method, 1999, 53, 1).should be_false
    end

    it "handles negative week and day numbers" do
      # October 1582 (the Gregorian calendar, Commercial Date in 1.9)
      #       M Tu  W Th  F Sa Su
      # -12: -7 -6 -5 -4 -3 -2 -1
      # -11: -7 -6 -5 -4 -3 -2 -1
      # -10: -7 -6 -5 -4 -3 -2 -1
      Date.send(@method, 1582, -12, -4).should be_true
      Date.send(@method, 1582, -12, -3).should be_true
      Date.send(@method, 2007, -44, -2).should be_true
      Date.send(@method, 2008, -44, -2).should be_true
      Date.send(@method, 1999, -53, -1).should be_false
    end
  end

end
