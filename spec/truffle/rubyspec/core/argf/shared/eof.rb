describe :argf_eof, :shared => true do
  before :each do
    @file1 = fixture __FILE__, "file1.txt"
    @file2 = fixture __FILE__, "file2.txt"
  end

  after :each do
    ARGF.close unless ARGF.closed?
  end

  # NOTE: this test assumes that fixtures files have two lines each
  it "returns true when reaching the end of a file" do
    argv [@file1, @file2, @file1] do
      result = []
      while ARGF.gets
        result << ARGF.send(@method)
      end
      result.should == [false, true, false, true, false, true]
    end
  end

  it "raises IOError when called on a closed stream" do
    argv [@file1] do
      ARGF.read
      lambda { ARGF.send(@method) }.should raise_error(IOError)
    end
  end
end
