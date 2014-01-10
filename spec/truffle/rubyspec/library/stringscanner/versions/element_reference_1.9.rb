describe "StringScanner#[name]" do
  before :each do
    @s = StringScanner.new("Fri Jun 13 2008 22:43")
  end

  ruby_version_is "1.9"..."2.1" do
    it "raises a TypeError when a String is as argument" do
      @s.scan(/(\w+) (\w+) (\d+) /)
      lambda { @s["wday"]}.should raise_error(TypeError)
      @s.scan(/(?<wday>\w+) (?<month>\w+) (?<day>\d+) /)
      @s["wday"].should be_nil
    end

    it "raises a TypeError when a Symbol is as argument" do
      @s.scan(/(\w+) (\w+) (\d+) /)
      lambda { @s[:wday]}.should raise_error(TypeError)
      @s.scan(/(?<wday>\w+) (?<month>\w+) (?<day>\d+) /)
      @s[:wday].should be_nil
    end
  end

  ruby_version_is "2.1" do
    it "raises a IndexError when there's no named capture" do
      @s.scan(/(\w+) (\w+) (\d+) /)
      lambda { @s["wday"]}.should raise_error(IndexError)
      lambda { @s[:wday]}.should raise_error(IndexError)
    end

    it "returns named capture" do
      @s.scan(/(?<wday>\w+) (?<month>\w+) (?<day>\d+) /)
      @s["wday"].should == "Fri"
      @s["month"].should == "Jun"
      @s["day"].should == "13"
      @s[:wday].should == "Fri"
      @s[:month].should == "Jun"
      @s[:day].should == "13"
    end
  end
end
