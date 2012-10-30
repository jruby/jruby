describe :argf_each_char, :shared => true do
  before :each do
    @file1_name = fixture __FILE__, "file1.txt"
    @file2_name = fixture __FILE__, "file2.txt"

    @chars = []
    File.read(@file1_name).each_char { |c| @chars << c }
    File.read(@file2_name).each_char { |c| @chars << c }
  end

  after :each do
    ARGF.close unless ARGF.closed?
  end

  it "yields each char of all streams to the passed block" do
    argv [@file1_name, @file2_name] do
      chars = []
      ARGF.send(@method) { |c| chars << c }
      chars.should == @chars
    end
  end

  it "returns self when passed a block" do
    argv [@file1_name, @file2_name] do
      ARGF.send(@method) {}.should equal(ARGF)
    end
  end

  it "returns an Enumerator when passed no block" do
    argv [@file1_name, @file2_name] do
      enum = ARGF.send(@method)
      enum.should be_an_instance_of(enumerator_class)

      chars = []
      enum.each { |c| chars << c }
      chars.should == @chars
    end
  end
end
