require File.dirname(__FILE__) + "/../spec_helper"
require 'tempfile'

describe "Ruby IO" do

  let(:input_number) { "1234567890" }

  it "gets an IO from a java.io.InputStream" do
    io = java.io.ByteArrayInputStream.new(input_number.to_java_bytes).to_io
    expect(io.class).to eq(IO)
    expect(io.read(5)).to eq("12345")
  end

  it "rewinds a java.io.ByteArrayInputStream" do
    io = java.io.ByteArrayInputStream.new(input_number.to_java_bytes).to_io
    expect(io.class).to eq(IO)
    expect(io.read(5)).to eq("12345")
    expect(io.read(5)).to eq("67890")
    io.rewind
    expect(io.read(5)).to eq("12345")
  end

  it "seeks a java.io.ByteArrayInputStream" do
    bytes = input_number.to_java_bytes
    io = java.io.ByteArrayInputStream.new(input_number.to_java_bytes, 3, 6).to_io
    expect(io.class).to eq(IO)
    expect(io.read(4)).to eq("4567")
    expect(io.read(4)).to eq("89")
    io.seek 0
    expect(io.read(5)).to eq("45678")
    io.seek 4
    expect(io.read(4)).to eq("89")
    io.seek 9
    expect(io.read(2)).to eq(nil)
  end

  it "gets an IO from a java.io.OutputStream" do
    output = java.io.ByteArrayOutputStream.new
    io = output.to_io
    expect(io.class).to eq(IO)
    io.write("12345")
    io.flush
    expect(String.from_java_bytes(output.to_byte_array)).to eq("12345")
  end

  it "is coercible to java.io.InputStream with IO#to_input_stream" do
    file = File.open(__FILE__)
    first_ten = file.read(10)
    file.seek(0)
    stream = file.to_input_stream
    expect(java.io.InputStream).to be === stream

    bytes = "0000000000".to_java_bytes
    expect(stream.read(bytes)).to eq(10)
    expect(String.from_java_bytes(bytes)).to eq(first_ten)

    expect(java.io.InputStream).to be === file.to_inputstream # old-naming
  end

  it "is coercible using to_java to java.io.InputStream" do
    file = File.open(__FILE__)
    first_ten = file.read(10)
    file.seek(0)
    stream = file.to_java java.io.InputStream
    expect(java.io.InputStream).to be === stream

    bytes = "0000000000".to_java_bytes
    expect(stream.read(bytes)).to eq(10)
    expect(String.from_java_bytes(bytes)).to eq(first_ten)
  end

  it "is coercible to java.io.OutputStream with IO#to_output_stream" do
    file = Tempfile.new("io_spec")
    stream = file.to_output_stream
    expect(java.io.OutputStream).to be === stream

    bytes = input_number.to_java_bytes
    stream.write(bytes)
    stream.flush
    file.seek(0)
    str = file.read(10)
    expect(str).to eq(String.from_java_bytes(bytes))

    expect(java.io.OutputStream).to be === file.to_outputstream # old-naming
  end


  it "is coercible using to_java to java.io.OutputStream" do
    file = Tempfile.new("io_spec")
    stream = file.to_java 'java.io.OutputStream'
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

  it "is coercible to java.io.Files" do
    file = Tempfile.new("io_spec").to_java 'java.io.File'
    expect(java.io.File).to be === file
    file = File.open(__FILE__).to_java java.io.File
    expect(java.io.File).to be === file
    expect(file.getPath).to eql __FILE__
  end

end
