require_relative '../../../spec_helper'

describe "IO::Buffer#pwrite" do
  before :each do
    @path = tmp("io_buffer_pwrite.txt")

    @file = File.open(@path, "wb+")
    @buffer = IO::Buffer.new(5)
    @buffer.set_string("Hello")
  end

  after :each do
    @file.close
    rm_r @path
  end

  it "writes the whole buffer when no length is given" do
    @buffer.pwrite(@file, 0).should == 5
    File.binread(@path).should == "Hello"
  end

  it "writes only the given length, starting at the given offset" do
    @buffer.pwrite(@file, 0, 4, 1).should == 4
    File.binread(@path).should == "ello"
  end

  it "treats a length of 0 as the rest of the buffer" do
    @buffer.pwrite(@file, 0, 0).should == 5
    File.binread(@path).should == "Hello"
  end

  it "treats a length of 0 as the rest of the buffer after the offset" do
    @buffer.pwrite(@file, 0, 0, 1).should == 4
    File.binread(@path).should == "ello"
  end
end
