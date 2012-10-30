describe :argf_filename, :shared => true do
  before :each do
    @file1 = fixture __FILE__, "file1.txt"
    @file2 = fixture __FILE__, "file2.txt"
  end

  after :each do
    ARGF.close unless ARGF.closed?
  end

  # NOTE: this test assumes that fixtures files have two lines each
  it "returns the current file name on each file" do
    argv [@file1, @file2] do
      result = []
      # returns first current file even when not yet open
      result << ARGF.send(@method)
      result << ARGF.send(@method) while ARGF.gets
      # returns last current file even when closed
      result << ARGF.send(@method)

      result.map! { |f| File.expand_path(f) }
      result.should == [@file1, @file1, @file1, @file2, @file2, @file2]
    end
  end

  # NOTE: this test assumes that fixtures files have two lines each
  it "it sets the $FILENAME global variable with the current file name on each file" do
    argv [@file1, @file2] do
      result = []
      result << $FILENAME while ARGF.gets
      # returns last current file even when closed
      result << $FILENAME
      result.map! { |f| File.expand_path(f) }
      result.should == [@file1, @file1, @file2, @file2, @file2]
    end
  end
end
