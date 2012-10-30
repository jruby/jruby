require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "IO#read_nonblock" do
  before(:each) do
    @read, @write = IO.pipe
  end

  after(:each) do
    @read.close rescue nil
    @write.close rescue nil
  end

  it "raises EAGAIN when there is no data" do
    lambda { @read.read_nonblock(5) }.should raise_error(Errno::EAGAIN)
  end

  ruby_version_is "1.9" do
    it "raises IO::WaitReadable when there is no data" do
      lambda { @read.read_nonblock(5) }.should raise_error(IO::WaitReadable)
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

  not_compliant_on :rubinius, :jruby do
    ruby_version_is ""..."1.9" do
      it "changes the behavior of #read to nonblocking" do
        @write << "hello"
        @read.read_nonblock(5)

        # Yes, use normal IO#read here. #read_nonblock has changed the internal
        # flags of @read to be nonblocking, so now any normal read calls raise
        # EAGAIN if there is no data.
        lambda { @read.read(5) }.should raise_error(Errno::EAGAIN)
      end
    end

    # This feature was changed in 1.9
    # see also: [ruby-dev:25101] http://blade.nagaokaut.ac.jp/cgi-bin/scat.rb/ruby/ruby-dev/25101
    #   and #2469 http://redmine.ruby-lang.org/issues/show/2469
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
