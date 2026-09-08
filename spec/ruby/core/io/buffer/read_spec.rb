require_relative '../../../spec_helper'
require 'socket'
require 'io/nonblock'

describe "IO::Buffer#read" do
  before :each do
    @path = tmp("io_buffer_read.txt")
    touch(@path) { |f| f.write "Hello" }

    @file = File.open(@path, "rb")
    @buffer = IO::Buffer.new(5)
  end

  after :each do
    @file.close
    rm_r @path
  end

  it "reads into the whole buffer when no length is given" do
    @buffer.read(@file).should == 5
    @buffer.get_string.should == "Hello"
  end

  it "reads only the given length, starting at the given offset" do
    @buffer.read(@file, 4, 1).should == 4
    @buffer.get_string(1, 4).should == "Hell"
  end

  it "treats a length of 0 as the rest of the buffer" do
    @buffer.read(@file, 0).should == 5
    @buffer.get_string.should == "Hello"
  end

  it "treats a length of 0 as the rest of the buffer after the offset" do
    @buffer.read(@file, 0, 1).should == 4
    @buffer.get_string(1, 4).should == "Hell"
  end

  platform_is_not :windows do
    it "returns the negated errno when the read would block" do
      r, w = UNIXSocket.pair
      begin
        r.nonblock = true
        @buffer.read(r, 5, 0).should == -Errno::EAGAIN::Errno
      ensure
        r.close
        w.close
      end
    end
  end
end
