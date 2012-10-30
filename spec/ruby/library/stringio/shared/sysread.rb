describe :stringio_sysread_length, :shared => true do
  before(:each) do
    @io = StringIO.new("example")
  end

  # This was filed as a bug in redmine#156 but since MRI refused to change the
  # 1.8 behavior, it's now considered a version difference by RubySpec since
  # it could have a significant impact on user code.
  ruby_version_is ""..."1.9" do
    it "raises an EOFError when passed 0 and no data remains" do
      @io.send(@method, 8).should == "example"
      lambda { @io.send(@method, 0) }.should raise_error(EOFError)
    end
  end

  ruby_version_is "1.9" do
    it "returns an empty String when passed 0 and no data remains" do
      @io.send(@method, 8).should == "example"
      @io.send(@method, 0).should == ""
    end
  end

  it "raises an EOFError when passed length > 0 and no data remains" do
    @io.read.should == "example"
    lambda { @io.sysread(1) }.should raise_error(EOFError)
  end
end
