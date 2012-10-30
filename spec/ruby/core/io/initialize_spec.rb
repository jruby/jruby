require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "IO#initialize" do
  before :each do
    @name = tmp("io_initialize.txt")
    @io = new_io @name
  end

  after :each do
    @io.close unless @io.closed?
    rm_r @name
  end

  it "reassociates the IO instance with the new descriptor when passed a Fixnum" do
    # This leaks one file descriptor. Do NOT write this spec to
    # call IO.new with the fd of an existing IO instance.
    fd = new_fd @name, "r:utf-8"
    @io.send :initialize, fd, 'r'
    @io.fileno.should == fd
  end

  it "calls #to_int to coerce the object passed as an fd" do
    # This leaks one file descriptor. Do NOT write this spec to
    # call IO.new with the fd of an existing IO instance.
    obj = mock('fileno')
    fd = new_fd @name, "r:utf-8"
    obj.should_receive(:to_int).and_return(fd)
    @io.send :initialize, obj, 'r'
    @io.fileno.should == fd
  end

  it "raises a TypeError when passed an IO" do
    lambda { @io.send :initialize, STDOUT, 'w' }.should raise_error(TypeError)
  end

  it "raises a TypeError when passed nil" do
    lambda { @io.send :initialize, nil, 'w' }.should raise_error(TypeError)
  end

  it "raises a TypeError when passed a String" do
    lambda { @io.send :initialize, "4", 'w' }.should raise_error(TypeError)
  end

  it "raises IOError on closed stream" do
    lambda { @io.send :initialize, IOSpecs.closed_io.fileno }.should raise_error(IOError)
  end

  it "raises an Errno::EBADF when given an invalid file descriptor" do
    lambda { @io.send :initialize, -1, 'w' }.should raise_error(Errno::EBADF)
  end
end
