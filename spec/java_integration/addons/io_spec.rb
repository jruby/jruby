require File.dirname(__FILE__) + "/../spec_helper"
require 'tempfile'

describe "Ruby IO" do
  it "should be able to get an IO from a java.io.InputStream" do
    io = java.io.ByteArrayInputStream.new("1234567890".to_java_bytes).to_io
    io.class.should == IO
    io.read(5).should == "12345"
  end
  
  it "should be able to get an IO from a java.io.OutputStream" do
    output = java.io.ByteArrayOutputStream.new
    io = output.to_io
    io.class.should == IO
    io.write("12345")
    io.flush
    String.from_java_bytes(output.to_byte_array).should == "12345"
  end

  it "should be coercible to java.io.InputStream with IO#to_inputstream" do
    file = File.open(__FILE__)
    first_ten = file.read(10)
    file.seek(0)
    stream = file.to_inputstream
    java.io.InputStream.should === stream
    
    bytes = "0000000000".to_java_bytes
    stream.read(bytes).should == 10
    String.from_java_bytes(bytes).should == first_ten
  end

  it "should be coercible to java.io.OutputStream with IO#to_outputstream" do
    file = Tempfile.new("io_spec")
    stream = file.to_outputstream
    java.io.OutputStream.should === stream 
    
    bytes = "1234567890".to_java_bytes
    stream.write(bytes)
    stream.flush
    file.seek(0)
    str = file.read(10)
    str.should == String.from_java_bytes(bytes)
  end

  it "should be able to get an IO from a java.nio.channels.Channel" do
    input = java.io.ByteArrayInputStream.new("1234567890".to_java_bytes)
    channel = java.nio.channels.Channels.newChannel(input)
    io = channel.to_io
    io.class.should == IO
    io.read(5).should == "12345"
    
    output = java.io.ByteArrayOutputStream.new
    channel = java.nio.channels.Channels.newChannel(output)
    io = channel.to_io
    io.class.should == IO
    io.write("12345")
    io.flush
    String.from_java_bytes(output.to_byte_array).should == "12345"
  end

  it "should be coercible to java.nio.channels.Channel with IO#to_channel" do
    file = Tempfile.new("io_spec")
    channel = file.to_channel
    java.nio.channels.Channel.should === channel 
    
    bytes = java.nio.ByteBuffer.wrap("1234567890".to_java_bytes)
    channel.write(bytes)
    file.seek(0)
    str = file.read(10)
    str.should == String.from_java_bytes(bytes.array)
  end
end
