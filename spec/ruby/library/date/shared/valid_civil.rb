describe :date_valid_civil?, :shared => true do

  # reference:
  # October 1582 (the Gregorian calendar, Civil Date)
  #   S   M  Tu   W  Th   F   S
  #       1   2   3   4  15  16
  #  17  18  19  20  21  22  23
  #  24  25  26  27  28  29  30
  #  31

  ruby_version_is "" ... "1.9" do
    it "returns the corresponding Julian Day Number if it is a valid civil date" do
      Date.send(@method, 1582, 10, 15).should == Date.civil(1582, 10, 15).jd
      Date.send(@method, 1582, 10, 14, Date::ENGLAND).should == Date.civil(1582, 10, 14, Date::ENGLAND).jd
    end

    it "returns nil if it is not a valid civil date" do
      Date.send(@method, 1582, 10, 14).should be_nil
      Date.send(@method, 1582, 10, 14, Date::ENGLAND).should_not be_nil
    end

    it "handles negative months and days" do
      # October 1582 (the Gregorian calendar, Civil Date in 1.8)
      #     S   M  Tu   W  Th   F   S
      #       -31 -30 -29 -28 -17 -16
      #   -15 -14 -13 -12 -11 -10  -9
      #    -8  -7  -6  -5  -4  -3  -2
      #    -1
      Date.send(@method, 1582, -3, -31).should == Date.civil(1582, 10,  1).jd
      Date.send(@method, 1582, -3, -28).should == Date.civil(1582, 10,  4).jd
      Date.send(@method, 1582, -3, -27).should be_nil
      Date.send(@method, 1582, -3, -22).should be_nil
      Date.send(@method, 1582, -3, -21).should be_nil
      Date.send(@method, 1582, -3, -18).should be_nil
      Date.send(@method, 1582, -3, -17).should == Date.civil(1582, 10, 15).jd

      Date.send(@method, 2007, -11, -10).should == Date.civil(2007, 2, 19).jd
      Date.send(@method, 2008, -11, -10).should == Date.civil(2008, 2, 20).jd
    end
  end

  ruby_version_is "1.9" do
    it "returns true if it is a valid civil date" do
      Date.send(@method, 1582, 10, 15).should be_true
      Date.send(@method, 1582, 10, 14, Date::ENGLAND).should be_true
    end

    it "returns false if it is not a valid civil date" do
      Date.send(@method, 1582, 10, 14).should == false
    end

    it "handles negative months and days" do
      # October 1582 (the Gregorian calendar, Civil Date in 1.9)
      #     S   M  Tu   W  Th   F   S
      #       -21 -20 -19 -18 -17 -16
      #   -15 -14 -13 -12 -11 -10  -9
      #    -8  -7  -6  -5  -4  -3  -2
      #    -1
      Date.send(@method, 1582, -3, -22).should be_false
      Date.send(@method, 1582, -3, -21).should be_true
      Date.send(@method, 1582, -3, -18).should be_true
      Date.send(@method, 1582, -3, -17).should be_true

      Date.send(@method, 2007, -11, -10).should be_true
      Date.send(@method, 2008, -11, -10).should be_true
    end
  end

end
