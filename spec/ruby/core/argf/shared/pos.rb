describe :argf_pos, :shared => true do
  before :each do
    @file1 = fixture __FILE__, "file1.txt"
    @file2 = fixture __FILE__, "file2.txt"
  end

  after :each do
    ARGF.close unless ARGF.closed?
  end

  it "gives the correct position for each read operation" do
    argv [@file1, @file2] do
      size1 = File.size(@file1)
      size2 = File.size(@file2)

      ARGF.read(2)
      ARGF.send(@method).should == 2
      ARGF.read(size1-2)
      ARGF.send(@method).should == size1
      ARGF.read(6)
      ARGF.send(@method).should == 6
      ARGF.rewind
      ARGF.send(@method).should == 0
      ARGF.read(size2)
      ARGF.send(@method).should == size2
    end
  end

  it "raises an ArgumentError when called on a closed stream" do
    argv [@file1] do
      ARGF.read
      lambda { ARGF.send(@method) }.should raise_error(ArgumentError)
    end
  end
end
