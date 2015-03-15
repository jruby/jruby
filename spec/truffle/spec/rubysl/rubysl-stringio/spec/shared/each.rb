describe :stringio_each_separator, :shared => true do
  before(:each) do
    @io = StringIO.new("a b c d e\n1 2 3 4 5")
    ScratchPad.record []
  end

  it "uses the passed argument as the line separator" do
    seen = []
    @io.send(@method, " ") {|s| seen << s}
    seen.should == ["a ", "b ", "c ", "d ", "e\n1 ", "2 ", "3 ", "4 ", "5"]
  end

  it "does not change $_" do
    $_ = "test"
    @io.send(@method, " ") { |s| s}
    $_.should == "test"
  end

  it "returns self" do
    @io.send(@method) {|l| l }.should equal(@io)
  end

  ruby_version_is "1.9" do
    it "returns at most limit characters when limit is positive" do
      @io.send(@method, $/, 5) { |x| ScratchPad << x }
      ScratchPad.recorded.should == ["a b c", " d e\n", "1 2 3", " 4 5"]
    end

    it "calls #to_str to convert a single argument to a String" do
      sep = mock("stringio each sep")
      sep.should_receive(:to_str).at_least(1).times.and_return($/)
      sep.should_not_receive(:to_int)

      @io.send(@method, sep) { |x| }
    end

    it "calls #to_int to convert a single argument if #to_str does not return a String" do
      limit = mock("stringio each limit")
      limit.should_receive(:to_str).at_least(1).times.and_return(nil)
      limit.should_receive(:to_int).at_least(1).times.and_return(5)

      @io.send(@method, limit) { |x| }
    end

    it "calls #to_int to convert the limit" do
      limit = mock("stringio each limit")
      limit.should_receive(:to_int).at_least(1).times.and_return(5)

      @io.send(@method, limit) { |x| }
    end

    it "calls #to_int to convert the limit when passed separator and limit" do
      limit = mock("stringio each limit")
      limit.should_receive(:to_int).at_least(1).times.and_return(6)

      @io.send(@method, $/, limit) { |x| }
    end

    it "raises an ArgumentError if length is 0 and #each is called on the Enumerator" do
      enum = @io.send(@method, 0)
      lambda { enum.each { } }.should raise_error(ArgumentError)
    end

    it "raises an ArgumentError if length is 0" do
      lambda { @io.send(@method, 0) { } }.should raise_error(ArgumentError)
    end
  end

  it "tries to convert the passed separator to a String using #to_str" do
    obj = mock("to_str")
    obj.stub!(:to_str).and_return(" ")

    seen = []
    @io.send(@method, obj) { |l| seen << l }
    seen.should == ["a ", "b ", "c ", "d ", "e\n1 ", "2 ", "3 ", "4 ", "5"]
  end

  it "yields self's content starting from the current position when the passed separator is nil" do
    seen = []
    io = StringIO.new("1 2 1 2 1 2")
    io.pos = 2
    io.send(@method, nil) {|s| seen << s}
    seen.should == ["2 1 2 1 2"]
  end

  ruby_bug "", "1.8.8" do
    it "yields each paragraph when passed an empty String as separator" do
      seen = []
      io = StringIO.new("para1\n\npara2\n\n\npara3")
      io.send(@method, "") {|s| seen << s}
      seen.should == ["para1\n\n", "para2\n\n", "para3"]
    end
  end
end

describe :stringio_each_no_arguments, :shared => true do
  before(:each) do
    @io = StringIO.new("a b c d e\n1 2 3 4 5")
  end

  it "yields each line to the passed block" do
    seen = []
    @io.send(@method) {|s| seen << s }
    seen.should == ["a b c d e\n", "1 2 3 4 5"]
  end

  it "yields each line starting from the current position" do
    seen = []
    @io.pos = 4
    @io.send(@method) {|s| seen << s }
    seen.should == ["c d e\n", "1 2 3 4 5"]
  end

  it "does not change $_" do
    $_ = "test"
    @io.send(@method) { |s| s}
    $_.should == "test"
  end

  it "uses $/ as the default line separator" do
    seen = []
    begin
      old_rs, $/ = $/, " "
      @io.send(@method) {|s| seen << s }
      seen.should eql(["a ", "b ", "c ", "d ", "e\n1 ", "2 ", "3 ", "4 ", "5"])
    ensure
      $/ = old_rs
    end
  end

  it "returns self" do
    @io.send(@method) {|l| l }.should equal(@io)
  end

  ruby_version_is "" ... "1.8.7" do
    it "yields a LocalJumpError when passed no block" do
      lambda { @io.send(@method) }.should raise_error(LocalJumpError)
    end
  end

  ruby_version_is "1.8.7" do
    it "returns an Enumerator when passed no block" do
      enum = @io.send(@method)
      enum.instance_of?(enumerator_class).should be_true

      seen = []
      enum.each { |b| seen << b }
      seen.should == ["a b c d e\n", "1 2 3 4 5"]
    end
  end
end

describe :stringio_each_not_readable, :shared => true do
  it "raises an IOError" do
    io = StringIO.new("a b c d e", "w")
    lambda { io.send(@method) { |b| b } }.should raise_error(IOError)

    io = StringIO.new("a b c d e")
    io.close_read
    lambda { io.send(@method) { |b| b } }.should raise_error(IOError)
  end
end
