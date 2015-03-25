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

  it "returns the size of self even if self is closed" do
    @tempfile.print("Test!")
    @tempfile.close
    @tempfile.send(@method).should eql(5)
  end
end
