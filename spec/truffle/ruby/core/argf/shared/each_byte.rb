describe :argf_each_byte, :shared => true do
  before :each do
    @file1_name = fixture __FILE__, "file1.txt"
    @file2_name = fixture __FILE__, "file2.txt"

    @bytes = []
    File.read(@file1_name).each_byte { |b| @bytes << b }
    File.read(@file2_name).each_byte { |b| @bytes << b }
  end

  after :each do
    ARGF.close unless ARGF.closed?
  end

  it "yields each byte of all streams to the passed block" do
    argv [@file1_name, @file2_name] do
      bytes = []
      ARGF.send(@method) { |b| bytes << b }
      bytes.should == @bytes
    end
  end

  it "returns self when passed a block" do
    argv [@file1_name, @file2_name] do
      ARGF.send(@method) {}.should equal(ARGF)
    end
  end

  ruby_version_is "" ... "1.8.7" do
    it "raises a LocalJumpError when passed no block" do
      argv [@file1_name, @file2_name] do
        lambda { ARGF.send(@method) }.should raise_error(LocalJumpError)
      end
    end
  end

  ruby_version_is "1.8.7" do
    it "returns an Enumerator when passed no block" do
      argv [@file1_name, @file2_name] do
        enum = ARGF.send(@method)
        enum.should be_an_instance_of(enumerator_class)

        bytes = []
        enum.each { |b| bytes << b }
        bytes.should == @bytes
      end
    end
  end
end
