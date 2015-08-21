require File.dirname(__FILE__) + "/../spec_helper"
require 'tempfile'

describe "Ruby IO" do
  let(:input_number){"1234567890"}
  it "gets an IO from a java.io.InputStream" do
    io = java.io.ByteArrayInputStream.new(input_number.to_java_bytes).to_io
    expect(io.class).to eq(IO)
    expect(io.read(5)).to eq("12345")
  end
  
  it "gets an IO from a java.io.OutputStream" do
    output = java.io.ByteArrayOutputStream.new
    io = output.to_io
    expect(io.class).to eq(IO)
    io.write("12345")
    io.flush
    expect(String.from_java_bytes(output.to_byte_array)).to eq("12345")
  end

  it "is coercible to java.io.InputStream with IO#to_inputstream" do
    file = File.open(__FILE__)
    first_ten = file.read(10)
    file.seek(0)
    stream = file.to_inputstream
    expect(java.io.InputStream).to be === stream
    
    bytes = "0000000000".to_java_bytes
    expect(stream.read(bytes)).to eq(10)
    expect(String.from_java_bytes(bytes)).to eq(first_ten)
  end

  it "is coercible to java.io.OutputStream with IO#to_outputstream" do
    file = Tempfile.new("io_spec")
    stream = file.to_outputstream
    expect(java.io.OutputStream).to be === stream 
    
    bytes = input_number.to_java_bytes
    stream.write(bytes)
    stream.flush
    file.seek(0)
    str = file.read(10)
    expect(str).to eq(String.from_java_bytes(bytes))
  end

  it "gets an IO from a java.nio.channels.Channel" do
    input = java.io.ByteArrayInputStream.new(input_number.to_java_bytes)
    channel = java.nio.channels.Channels.newChannel(input)
    io = channel.to_io
    expect(io.class).to eq(IO)
    expect(io.read(5)).to eq("12345")
    
    output = java.io.ByteArrayOutputStream.new
    channel = java.nio.channels.Channels.newChannel(output)
    io = channel.to_io
    expect(io.class).to eq(IO)
    io.write("12345")
    io.flush
    expect(String.from_java_bytes(output.to_byte_array)).to eq("12345")
  end

  it "is coercible to java.nio.channels.Channel with IO#to_channel" do
    file = Tempfile.new("io_spec")
    channel = file.to_channel
    expect(java.nio.channels.Channel).to be === channel 
    
    bytes = java.nio.ByteBuffer.wrap(input_number.to_java_bytes)
    channel.write(bytes)
    file.seek(0)
    str = file.read(10)
    expect(str).to eq(String.from_java_bytes(bytes.array))
  end
end
