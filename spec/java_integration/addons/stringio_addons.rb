require File.dirname(__FILE__) + "/../spec_helper"
require 'stringio'

describe "Ruby StringIO" do
  it "should be coercible to java.io.InputStream with StringIO#to_input_stream" do
    file = StringIO.new("\xC3\x80abcdefghij")
    stream = file.to_input_stream
    expect(java.io.InputStream).to be === stream

    expect(stream.read).to eq(0xc3)
    expect(stream.read).to eq(0x80)

    bytes = "0000000000".to_java_bytes
    expect(stream.read(bytes)).to eq(10)
    expect(String.from_java_bytes(bytes)).to eq('abcdefghij')

    # compatiblity with old-naming
    expect(java.io.InputStream).to be === file.to_inputstream
  end

  it "should be coercible to java.io.OutputStream with StringIO#to_output_stream" do
    file = StringIO.new
    stream = file.to_output_stream
    expect(java.io.OutputStream).to be === stream 
    
    bytes = "1234567890".to_java_bytes
    stream.write(bytes)
    stream.flush
    file.seek(0)
    str = file.read(10)
    expect(str).to eq(String.from_java_bytes(bytes))

    # compatiblity with old-naming
    expect(java.io.OutputStream).to be === file.to_outputstream
  end

  it "should be coercible to java.nio.channels.Channel with StringIO#to_channel" do
    file = StringIO.new
    channel = file.to_channel
    expect(java.nio.channels.Channel).to be === channel 
    
    bytes = java.nio.ByteBuffer.wrap("1234567890".to_java_bytes)
    channel.write(bytes)
    file.seek(0)
    str = file.read(10)
    expect(str).to eq(String.from_java_bytes(bytes.array))
  end
end
