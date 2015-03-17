describe :tempfile_length, :shared => true do
  before :each do
    @tempfile = Tempfile.new("specs")
  end

  after :each do
    TempfileSpecs.cleanup @tempfile
  end

  it "returns the size of self" do
    @tempfile.send(@method).should eql(0)
    @tempfile.print("Test!")
    @tempfile.send(@method).should eql(5)
  end

  ruby_version_is ''...'1.9.2' do
    it "returns 0 when self is closed" do
      @tempfile.print("Test!")
      @tempfile.close
      @tempfile.send(@method).should eql(0)
    end
  end

  ruby_version_is '1.9.2' do
    it "returns the size of self even if self is closed" do
      @tempfile.print("Test!")
      @tempfile.close
      @tempfile.send(@method).should eql(5)
    end
  end
end
