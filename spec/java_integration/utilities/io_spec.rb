require File.dirname(__FILE__) + "/../spec_helper"
require 'jruby'

describe "The JRuby module" do
  it "should give access to a Java reference with the reference method" do
    str_ref = JRuby.reference("foo")
    str_ref.class.should == org.jruby.RubyString
    
    str = str_ref.toString
    str.class.should == String
  end
  
  it "should unwrap Java-wrapped Ruby objects with the dereference method" do
    io_ref = org.jruby.RubyIO.new(JRuby.runtime, java.lang.System.in)
    io_ref.class.should == org.jruby.RubyIO
    
    io = JRuby.dereference(io_ref)
    io.class.should == IO
  end
end
