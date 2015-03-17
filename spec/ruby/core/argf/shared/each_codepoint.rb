describe :argf_each_codepoint, :shared => true do
  before :each do
    file1_name = fixture __FILE__, "file1.txt"
    file2_name = fixture __FILE__, "file2.txt"
    @filenames = [file1_name, file2_name]

    @codepoints = File.read(file1_name).codepoints
    @codepoints.concat File.read(file2_name).codepoints
  end

  after :each do
    ARGF.close unless ARGF.closed?
  end

  it "is a public method" do
    argv @filenames do
      ARGF.public_methods(false).should include(@method)
    end
  end

  it "does not require arguments" do
    argv @filenames do
      ARGF.method(@method).arity.should == 0
    end
  end

  it "returns self when passed a block" do
    argv @filenames do
      ARGF.send(@method) {}.should equal(ARGF)
    end
  end

  it "returns an Enumerator when passed no block" do
    argv @filenames do
      ARGF.send(@method).should be_an_instance_of(enumerator_class)
    end
  end

  it "yields each codepoint of all streams" do
    argv @filenames do
      ARGF.send(@method).to_a.should == @codepoints
    end
  end
end
