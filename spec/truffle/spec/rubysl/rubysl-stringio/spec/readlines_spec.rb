require File.expand_path('../fixtures/classes', __FILE__)

describe "StringIO#readlines when passed [separator]" do
  before(:each) do
    @io = StringIO.new("this>is>an>example")
  end

  it "returns an Array containing lines based on the passed separator" do
    @io.readlines(">").should == ["this>", "is>", "an>", "example"]
  end

  it "updates self's position based on the number of read bytes" do
    @io.readlines(">")
    @io.pos.should eql(18)
  end

  it "updates self's lineno based on the number of read lines" do
    @io.readlines(">")
    @io.lineno.should eql(4)
  end

  it "does not change $_" do
    $_ = "test"
    @io.readlines(">")
    $_.should == "test"
  end

  ruby_bug "", "1.8.8" do
    it "returns an Array containing all paragraphs when the passed separator is an empty String" do
      io = StringIO.new("this is\n\nan example")
      io.readlines("").should == ["this is\n\n", "an example"]
    end
  end

  it "returns the remaining content as one line starting at the current position when passed nil" do
    io = StringIO.new("this is\n\nan example")
    io.pos = 5
    io.readlines(nil).should == ["is\n\nan example"]
  end

  it "tries to convert the passed separator to a String using #to_str" do
    obj = mock('to_str')
    obj.stub!(:to_str).and_return(">")
    @io.readlines(obj).should == ["this>", "is>", "an>", "example"]
  end
end

ruby_version_is "1.9" do
  describe "StringIO#readlines" do
    before :each do
      @io = StringIO.new("ab\ncd")
      ScratchPad.record []
    end

    it "returns at most limit characters when limit is positive" do
      @io.readlines.should == ["ab\n", "cd"]
    end

    it "calls #to_int to convert the limit" do
      limit = mock("stringio each limit")
      limit.should_receive(:to_int).at_least(1).times.and_return(5)

      @io.readlines(limit)
    end

    it "calls #to_int to convert the limit when passed separator and limit" do
      limit = mock("stringio each limit")
      limit.should_receive(:to_int).at_least(1).times.and_return(6)

      @io.readlines($/, limit)
    end

    it "raises an ArgumentError when limit is 0" do
      lambda { @io.readlines(0) }.should raise_error(ArgumentError)
    end
  end
end

describe "StringIO#readlines when passed no argument" do
  before(:each) do
    @io = StringIO.new("this is\nan example\nfor StringIO#readlines")
  end

  it "returns an Array containing lines based on $/" do
    begin
      old_sep, $/ = $/, " "
      @io.readlines.should == ["this ", "is\nan ", "example\nfor ", "StringIO#readlines"]
    ensure
      $/ = old_sep
    end
  end

  it "updates self's position based on the number of read bytes" do
    @io.readlines
    @io.pos.should eql(41)
  end

  it "updates self's lineno based on the number of read lines" do
    @io.readlines
    @io.lineno.should eql(3)
  end

  it "does not change $_" do
    $_ = "test"
    @io.readlines(">")
    $_.should == "test"
  end

  it "returns an empty Array when self is at the end" do
    @io.pos = 41
    @io.readlines.should == []
  end
end

describe "StringIO#readlines when in write-only mode" do
  it "raises an IOError" do
    io = StringIO.new("xyz", "w")
    lambda { io.readlines }.should raise_error(IOError)

    io = StringIO.new("xyz")
    io.close_read
    lambda { io.readlines }.should raise_error(IOError)
  end
end
