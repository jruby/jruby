require File.dirname(__FILE__) + "/../spec_helper"
require 'jruby'
require 'stringio'
require 'tmpdir'

describe "The JRuby module" do
  it "should give access to a Java reference with the reference method" do
    str_ref = JRuby.reference("foo")
    expect(str_ref.class).to eq(org.jruby.RubyString)
    
    str = str_ref.toString
    expect(str.class).to eq(String)
  end
  
  it "should unwrap Java-wrapped Ruby objects with the dereference method" do
    io_ref = org.jruby.RubyIO.new(JRuby.runtime, java.lang.System.in)
    expect(io_ref.class).to eq(org.jruby.RubyIO)
    
    io = JRuby.dereference(io_ref)
    expect(io.class).to eq(IO)
  end
end

describe "IOOutputStream" do
  # https://github.com/jruby/jruby/issues/8686
  it "allows source and destination encoding to be the same" do
    filename = File.join(Dir.tmpdir, "iooutputstream_write")
    io = File.open(filename, "w+:UTF-8")
    ioos = org.jruby.util.IOOutputStream.new(io, org.jcodings.specific.UTF8Encoding::INSTANCE)
    bytes = "…".to_java_bytes
    ioos.write(bytes, 0, bytes.length)
    ioos.flush
    io.flush

    File.read(filename).should == "…"
  ensure
    io.close rescue nil
  end
end
