describe :stringio_read, :shared => true do
  before(:each) do
    @io = StringIO.new("example")
  end

  it "returns the passed buffer String" do
    ret = @io.send(@method, 7, buffer = "")
    ret.should equal(buffer)
  end

  it "returns the remaining data when limit is nil" do
    @io.send(@method, nil, buffer = "").should == "example"
    buffer.should == "example"
  end

  ruby_version_is "1.9" do
    it "truncates buffer when limit is nil and no data reamins" do
      @io.send(@method, nil)
      @io.send(@method, nil, buffer = "abc").should == ""
      buffer.should == ""
    end
  end

  it "reads length bytes and writes them to the buffer String" do
    @io.send(@method, 7, buffer = "")
    buffer.should == "example"
  end

  it "calls #to_str to convert the object to a String" do
    obj = mock("to_str")
    obj.should_receive(:to_str).and_return(buffer = "")

    @io.send(@method, 7, obj)
    buffer.should == "example"
  end

  it "raises a TypeError if the object does not respond to #to_str" do
    lambda { @io.send(@method, 7, Object.new) }.should raise_error(TypeError)
  end

  ruby_version_is ""..."1.9" do
    it "raises a TypeError when passed a frozen String as buffer" do
      lambda { @io.send(@method, 7, "".freeze) }.should raise_error(TypeError)
    end
  end

  ruby_version_is "1.9" do
    it "raises a RuntimeError when passed a frozen String as buffer" do
      lambda { @io.send(@method, 7, "".freeze) }.should raise_error(RuntimeError)
    end
  end

  with_feature :encoding do
    it "returns a String in ASCII-8BIT ignoring the encoding of the source String and buffer" do
      io = StringIO.new("abc".force_encoding(Encoding::EUC_JP))
      buffer = "".force_encoding(Encoding::ISO_8859_1)
      io.send(@method, 3, buffer).encoding.should == Encoding::ASCII_8BIT
    end
  end
end

describe :stringio_read_length, :shared => true do
  before(:each) do
    @io = StringIO.new("example")
  end

  it "reads length bytes from the current position and returns them" do
    @io.pos = 3
    @io.send(@method, 4).should == "mple"
  end

  it "reads at most the whole content" do
    @io.send(@method, 999).should == "example"
  end
  it "updates the position" do
    @io.send(@method, 3)
    @io.pos.should eql(3)

    @io.send(@method, 999)
    @io.pos.should eql(7)
  end

  it "calls #to_int to convert the length" do
    obj = mock("to_int")
    obj.should_receive(:to_int).and_return(7)
    @io.send(@method, obj).should == "example"
  end

  it "raises a TypeError when the length does not respond to #to_int" do
    lambda { @io.send(@method, Object.new) }.should raise_error(TypeError)
  end

  it "raises a TypeError when length is negative" do
    lambda { @io.send(@method, -2) }.should raise_error(ArgumentError)
  end

  with_feature :encoding do
    it "returns a String in ASCII-8BIT encoding when passed a length > 0" do
      @io.send(@method, 4).encoding.should == Encoding::ASCII_8BIT
    end

    it "returns an empty String in ASCII-8BIT encoding when passed length == 0" do
      @io.send(@method, 0).encoding.should == Encoding::ASCII_8BIT
    end
  end
end

describe :stringio_read_no_arguments, :shared => true do
  before(:each) do
    @io = StringIO.new("example")
  end

  it "reads the whole content starting from the current position" do
    @io.send(@method, 10).should == "example"

    @io.pos = 3
    @io.send(@method, 10).should == "mple"
  end

  it "updates the current position" do
    @io.send(@method, 10)
    @io.pos.should eql(7)
  end

  ruby_bug "readmine#156", "1.8.7" do
    it "returns an empty String when no data remains" do
      @io.send(@method, 7).should == "example"
      @io.send(@method, nil).should == ""
    end
  end

  with_feature :encoding do
    it "returns a String in the same encoding as the source String" do
      io = StringIO.new("abc".force_encoding(Encoding::EUC_JP))
      io.send(@method).encoding.should == Encoding::EUC_JP
    end

    it "returns an empty String in ASCII-8BIT encoding" do
      @io.send(@method).should == "example"
      @io.send(@method).encoding.should == Encoding::ASCII_8BIT
    end
  end
end

describe :stringio_read_nil, :shared => true do
  before :each do
    @io = StringIO.new("example")
  end

  it "returns the remaining content from the current position" do
    @io.send(@method, nil).should == "example"

    @io.pos = 4
    @io.send(@method, nil).should == "ple"
  end

  it "updates the current position" do
    @io.send(@method, nil)
    @io.pos.should eql(7)
  end

  ruby_bug "redmine#156", "1.8.7" do
    it "returns an empty String when no data remains" do
      @io.send(@method, nil).should == "example"
      @io.send(@method, nil).should == ""
    end
  end

  with_feature :encoding do
    it "returns an empty String in ASCII-8BIT encoding" do
      @io.send(@method).should == "example"
      @io.send(@method).encoding.should == Encoding::ASCII_8BIT
    end
  end
end

describe :stringio_read_not_readable, :shared => true do
  it "raises an IOError" do
    io = StringIO.new("test", "w")
    lambda { io.send(@method, 2) }.should raise_error(IOError)

    io = StringIO.new("test")
    io.close_read
    lambda { io.send(@method, 2) }.should raise_error(IOError)
  end
end
