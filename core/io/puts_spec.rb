require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "IO#puts" do
  before :each do
    @before_separator = $/
    @name = tmp("io_puts.txt")
    @io = new_io @name
    ScratchPad.record ""
    def @io.write(str)
      ScratchPad << str
    end
  end

  after :each do
    ScratchPad.clear
    @io.close if @io
    rm_r @name
    $/ = @before_separator
  end

  it "writes just a newline when given no args" do
    @io.puts.should == nil
    ScratchPad.recorded.should == "\n"
  end

  it "writes just a newline when given just a newline" do
    lambda { $stdout.puts "\n" }.should output_to_fd("\n", STDOUT)
  end

  it "writes empty string with a newline when given nil as an arg" do
    @io.puts(nil).should == nil
    ScratchPad.recorded.should == "\n"
  end

  it "writes empty string with a newline when when given nil as multiple args" do
    @io.puts(nil, nil).should == nil
    ScratchPad.recorded.should == "\n\n"
  end

  it "calls to_s before writing non-string objects" do
    object = mock('hola')
    object.should_receive(:to_s).and_return("hola")

    @io.puts(object).should == nil
    ScratchPad.recorded.should == "hola\n"
  end

  it "writes each arg if given several" do
    @io.puts(1, "two", 3).should == nil
    ScratchPad.recorded.should == "1\ntwo\n3\n"
  end

  it "flattens a nested array before writing it" do
    @io.puts([1, 2, [3]]).should == nil
    ScratchPad.recorded.should == "1\n2\n3\n"
  end

  it "writes nothing for an empty array" do
    x = []
    @io.should_not_receive(:write)
    @io.puts(x).should == nil
  end

  it "writes [...] for a recursive array arg" do
    x = []
    x << 2 << x
    @io.puts(x).should == nil
    ScratchPad.recorded.should == "2\n[...]\n"
  end

  it "writes a newline after objects that do not end in newlines" do
    @io.puts(5).should == nil
    ScratchPad.recorded.should == "5\n"
  end

  it "does not write a newline after objects that end in newlines" do
    @io.puts("5\n").should == nil
    ScratchPad.recorded.should == "5\n"
  end

  it "ignores the $/ separator global" do
    $/ = ":"
    @io.puts(5).should == nil
    ScratchPad.recorded.should == "5\n"
  end

  it "raises IOError on closed stream" do
    lambda { IOSpecs.closed_io.puts("stuff") }.should raise_error(IOError)
  end
end
