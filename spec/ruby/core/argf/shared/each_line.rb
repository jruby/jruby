describe :argf_each_line, :shared => true do
  before :each do
    @file1_name = fixture __FILE__, "file1.txt"
    @file2_name = fixture __FILE__, "file2.txt"

    @lines  = File.readlines @file1_name
    @lines += File.readlines @file2_name
  end

  after :each do
    ARGF.close unless ARGF.closed?
  end

  it "is a public method" do
    argv [@file1_name, @file2_name] do
      ARGF.public_methods(false).should include(stasy(@method))
    end
  end

  it "requires multiple arguments" do
    argv [@file1_name, @file2_name] do
      ARGF.method(@method).arity.should < 0
    end
  end

  it "reads each line of files" do
    argv [@file1_name, @file2_name] do
      lines = []
      ARGF.send(@method) { |b| lines << b }
      lines.should == @lines
    end
  end

  it "returns self when passed a block" do
    argv [@file1_name, @file2_name] do
      ARGF.send(@method) {}.should equal(ARGF)
    end
  end

  it "returns an Enumerator when passed no block" do
    argv [@file1_name, @file2_name] do
      ARGF.send(@method).should be_an_instance_of(enumerator_class)
    end
  end

  describe "with a separator" do
    it "yields each separated section of all streams" do
      argv [@file1_name, @file2_name] do
        ARGF.send(@method, '.').to_a.should ==
          (File.readlines(@file1_name, '.') + File.readlines(@file2_name, '.'))
      end
    end
  end
end
