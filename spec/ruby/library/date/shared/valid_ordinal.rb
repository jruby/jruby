describe :date_valid_ordinal?, :shared => true do
  ruby_version_is "" ... "1.9" do
    it "determines if the date is a valid ordinal date" do
      # October 1582 (the Gregorian calendar, Ordinal Date in 1.8)
      #   S   M  Tu   W  Th   F   S
      #     274 275 276 277 278 279
      # 280 281 282 283 284 285 286
      # 287 288 289 290 291 292 293
      # 294
      Date.send(@method, 1582, 277).should == Date.civil(1582, 10,  4).jd
      Date.send(@method, 1582, 278).should == nil
      Date.send(@method, 1582, 287).should == nil
      Date.send(@method, 1582, 288).should == Date.civil(1582, 10, 15).jd
      Date.send(@method, 1582, 287, Date::ENGLAND).should_not == nil
      Date.send(@method, 1582, 287, Date::ENGLAND).should == Date.civil(1582, 10, 14, Date::ENGLAND).jd
    end

    it "handles negative day numbers" do
      # October 1582 (the Gregorian calendar, Ordinal Date in 1.8)
      #   S   M  Tu   W  Th   F   S
      #     -92 -91 -90 -89 -78 -77
      # -76 -75 -74 -73 -72 -71 -70
      # -69 -68 -67 -66 -65 -64 -63
      # -62
      Date.send(@method, 1582, -89).should == Date.civil(1582, 10,  4).jd
      Date.send(@method, 1582, -88).should == nil
      Date.send(@method, 1582, -79).should == nil
      Date.send(@method, 1582, -78).should == Date.civil(1582, 10, 15).jd
      Date.send(@method, 2007, -100).should == Date.send(@method, 2007, 266)
    end
  end

  ruby_version_is "1.9" do
    it "determines if the date is a valid ordinal date" do
      # October 1582 (the Gregorian calendar, Ordinal Date in 1.9)
      #   S   M  Tu   W  Th   F   S
      #     274 275 276 277 278 279
      # 280 281 282 283 284 285 286
      # 287 288 289 290 291 292 293
      # 294
      Date.send(@method, 1582, 277).should == true
      Date.send(@method, 1582, 278).should == true
      Date.send(@method, 1582, 287).should == true
      Date.send(@method, 1582, 288).should == true
    end

    it "handles negative day numbers" do
      # October 1582 (the Gregorian calendar, Ordinal Date in 1.9)
      #   S   M  Tu   W  Th   F   S
      #     -82 -81 -80 -79 -78 -77
      # -76 -75 -74 -73 -72 -71 -70
      # -69 -68 -67 -66 -65 -64 -63
      # -62
      Date.send(@method, 1582, -79).should == true
      Date.send(@method, 1582, -78).should == true
      Date.send(@method, 2007, -100).should == true
    end
  end

end
