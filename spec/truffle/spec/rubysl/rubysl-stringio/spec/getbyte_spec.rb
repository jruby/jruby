require 'stringio'
require File.expand_path('../shared/getc', __FILE__)

describe "StringIO#getbyte" do
  it_behaves_like :stringio_getc, :getbyte

  it "returns the 8-bit byte at the current position" do
    io = StringIO.new("example")

    io.send(@method).should == 101
    io.send(@method).should == 120
    io.send(@method).should ==  97
  end

  it "retrieves bytes at a time rather than characters" do
    io = StringIO.new("\xc3\x91")
      
    io.getbyte.should == 0xc3
    io.getbyte.should == 0x91
  end
end

describe "StringIO#getbyte when self is not readable" do
  it_behaves_like :stringio_getc_not_readable, :getbyte
end
