require File.dirname(__FILE__) + "/../spec_helper"
require 'jruby'

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
