describe "StringScanner#[name]" do
  before :each do
    @s = StringScanner.new("Fri Jun 13 2008 22:43")
  end

  ruby_version_is "1.8" do
    it "raises a TypeError when a String is as argument" do
      @s.scan(/(\w+) (\w+) (\d+) /)
      lambda { @s["wday"]}.should raise_error(TypeError)
    end

    it "returns nil when a Symbol is as argument" do
      @s.scan(/(\w+) (\w+) (\d+) /)
      @s[:wday].should be_nil
    end
  end
end
