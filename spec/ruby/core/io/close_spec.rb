require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "IO#close" do
  before :each do
    @name = tmp('io_close.txt')
    @io = new_io @name
  end

  after :each do
    @io.close unless @io.closed?
    rm_r @name
  end

  it "closes the stream" do
    @io.close
    @io.closed?.should == true
  end

  it "returns nil" do
    @io.close.should == nil
  end

  it "raises an IOError reading from a closed IO" do
    @io.close
    lambda { @io.read }.should raise_error(IOError)
  end

  it "raises an IOError writing to a closed IO" do
    @io.close
    lambda { @io.write "data" }.should raise_error(IOError)
  end

  ruby_version_is ''...'2.3' do
    it "raises an IOError if closed" do
      @io.close
      lambda { @io.close }.should raise_error(IOError)
    end
  end

  ruby_version_is "2.3" do
    it "does not raise anything when self was already closed" do
      @io.close
      lambda { @io.close }.should_not raise_error
    end
  end
end

describe "IO#close on an IO.popen stream" do

  it "clears #pid" do
    io = IO.popen 'yes', 'r'

    io.pid.should_not == 0

    io.close

    lambda { io.pid }.should raise_error(IOError)
  end

  it "sets $?" do
    io = IO.popen 'true', 'r'
    io.close

    $?.exitstatus.should == 0

    io = IO.popen 'false', 'r'
    io.close

    $?.exitstatus.should == 1
  end

  it "waits for the child to exit" do
    io = IO.popen 'yes', 'r'
    io.close

    $?.exitstatus.should_not == 0 # SIGPIPE/EPIPE
  end

end

