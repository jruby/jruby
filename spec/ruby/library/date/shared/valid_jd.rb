describe :date_valid_jd?, :shared => true do
  ruby_version_is "" ... "1.9" do
    it "returns passed argument" do
      Date.send(@method, -100).should == -100
      Date.send(@method, :number).should == :number
      Date.send(@method, nil).should  == nil
    end

  end

  ruby_version_is "1.9" do
    it "returns true if passed any value other than nil" do
      Date.send(@method, -100).should be_true
      Date.send(@method, :number).should    be_true
      Date.send(@method, Rational(1,2)).should  be_true
    end
  end

  ruby_version_is "1.9" do
    it "returns false if passed nil" do
      Date.send(@method, nil).should be_false
    end
  end

  ruby_version_is "1.9" ... "1.9.3" do
    it "returns false if passed false" do
      Date.send(@method, false).should be_false
    end
  end

  ruby_version_is "1.9.3" do
    it "returns true if passed false" do
      Date.send(@method, false).should be_true
    end
  end
end
