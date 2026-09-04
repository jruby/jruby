require_relative '../../../spec_helper'

describe "IO::Buffer#pread" do
  before :each do
    @path = tmp("io_buffer_pread.txt")
    touch(@path) { |f| f.write "Hello" }

    @file = File.open(@path, "rb")
    @buffer = IO::Buffer.new(5)
  end

  after :each do
    @file.close
    rm_r @path
  end

  it "reads into the whole buffer when no length is given" do
    @buffer.pread(@file, 0).should == 5
    @buffer.get_string.should == "Hello"
  end

  it "reads only the given length, starting at the given offset" do
    @buffer.pread(@file, 0, 4, 1).should == 4
    @buffer.get_string(1, 4).should == "Hell"
  end

  it "treats a length of 0 as the rest of the buffer" do
    @buffer.pread(@file, 0, 0).should == 5
    @buffer.get_string.should == "Hello"
  end

  it "treats a length of 0 as the rest of the buffer after the offset" do
    @buffer.pread(@file, 0, 0, 1).should == 4
    @buffer.get_string(1, 4).should == "Hell"
  end
end
