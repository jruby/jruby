require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "IO#read_nonblock" do
  before :each do
    @read, @write = IO.pipe
  end

  after :each do
    @read.close if @read && !@read.closed?
    @write.close if @write && !@write.closed?
  end

  it "raises EAGAIN or a subclass when there is no data" do
    lambda { @read.read_nonblock(5) }.should raise_error(Errno::EAGAIN)
  end

  it "raises an exception extending IO::WaitReadable when there is no data" do
    lambda { @read.read_nonblock(5) }.should raise_error(IO::WaitReadable)
  end

  ruby_version_is "2.1" do
    it "raises IO::EAGAINWaitReadable when there is no data" do
      lambda { @read.read_nonblock(5) }.should raise_error(IO::EAGAINWaitReadable)
    end
  end

  it "returns at most the number of bytes requested" do
    @write << "hello"
    @read.read_nonblock(4).should == "hell"
  end

  it "returns less data if that is all that is available" do
    @write << "hello"
    @read.read_nonblock(10).should == "hello"
  end

  it "allows for reading 0 bytes before any write" do
    @read.read_nonblock(0).should == ""
  end

  it "allows for reading 0 bytes after a write" do
    @write.write "1"
    @read.read_nonblock(0).should == ""
    @read.read_nonblock(1).should == "1"
  end

  it "raises IOError on closed stream" do
    lambda { IOSpecs.closed_io.read_nonblock(5) }.should raise_error(IOError)
  end

  it "raises EOFError when the end is reached" do
    @write << "hello"
    @write.close

    @read.read_nonblock(5)

    lambda { @read.read_nonblock(5) }.should raise_error(EOFError)
  end
end
