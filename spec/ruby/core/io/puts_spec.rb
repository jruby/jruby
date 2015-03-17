require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

# TODO: need to find a better way to test this. Too fragile to set expectations
# to each write call. Only care that all the characters are sent not the number
# or write calls. Also, these tests do not make sure the ordering of the write calls
# are correct.
describe "IO#puts" do
  before :each do
    @before_separator = $/
    @name = tmp("io_puts.txt")
    @io = new_io @name
  end

  after :each do
    $/ = @before_separator
    @io.close unless @io.closed?
    rm_r @name
  end

  it "writes just a newline when given no args" do
    @io.should_receive(:write).with("\n")
    @io.puts.should == nil
  end

  it "writes just a newline when given just a newline" do
    lambda { $stdout.puts "\n" }.should output_to_fd("\n", STDOUT)
  end

  it "writes empty string with a newline when given nil as an arg" do
    @io.should_receive(:write).with("")
    @io.should_receive(:write).with("\n")
    @io.puts(nil).should == nil
  end

  it "writes empty string with a newline when when given nil as multiple args" do
    @io.should_receive(:write).with("").twice
    @io.should_receive(:write).with("\n").twice
    @io.puts(nil, nil).should == nil
  end

  it "calls to_s before writing non-string objects" do
    object = mock('hola')
    object.should_receive(:to_s).and_return("hola")

    @io.should_receive(:write).with("hola")
    @io.should_receive(:write).with("\n")
    @io.puts(object).should == nil
  end

  it "writes each arg if given several" do
    @io.should_receive(:write).with("1")
    @io.should_receive(:write).with("two")
    @io.should_receive(:write).with("3")
    @io.should_receive(:write).with("\n").exactly(3).times
    @io.puts(1, "two", 3).should == nil
  end

  it "flattens a nested array before writing it" do
    @io.should_receive(:write).with("1")
    @io.should_receive(:write).with("2")
    @io.should_receive(:write).with("3")
    @io.should_receive(:write).with("\n").exactly(3).times
    @io.puts([1, 2, [3]]).should == nil
  end

  it "writes nothing for an empty array" do
    x = []
    @io.should_receive(:write).exactly(0).times
    @io.puts(x).should == nil
  end

  it "writes [...] for a recursive array arg" do
    x = []
    x << 2 << x
    @io.should_receive(:write).with("2")
    @io.should_receive(:write).with("[...]")
    @io.should_receive(:write).with("\n").exactly(2).times
    @io.puts(x).should == nil
  end

  it "writes a newline after objects that do not end in newlines" do
    @io.should_receive(:write).with("5")
    @io.should_receive(:write).with("\n")
    @io.puts(5).should == nil
  end

  it "does not write a newline after objects that end in newlines" do
    @io.should_receive(:write).with("5\n")
    @io.puts("5\n").should == nil
  end

  it "ignores the $/ separator global" do
    $/ = ":"
    @io.should_receive(:write).with("5")
    @io.should_receive(:write).with("\n")
    @io.puts(5).should == nil
  end

  it "raises IOError on closed stream" do
    lambda { IOSpecs.closed_io.puts("stuff") }.should raise_error(IOError)
  end
end
