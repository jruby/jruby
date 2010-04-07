require File.dirname(__FILE__) + "/../spec_helper"
require 'java'

describe "A Java Throwable" do
  it "implements backtrace" do
    ex = java.lang.Exception.new
    trace = nil
    lambda {trace = ex.backtrace}.should_not raise_error
    trace.should == ex.stack_trace.map(&:to_s)
  end
  
  it "implements backtrace=" do
    ex = java.lang.Exception.new
    ex.backtrace = ['blah']
    ex.backtrace.should == ['blah']
  end
  
  it "implements to_s as message" do
    ex = java.lang.Exception.new
    ex.to_s.should == nil
    ex.to_s.should == ex.message
    
    ex = java.lang.Exception.new('hello')
    ex.to_s.should == 'hello'
    ex.to_s.should == ex.message
  end
  
  it "implements to_str to call to_s" do
    ex = java.lang.Exception.new
    def ex.to_s
      'hello'
    end
    
    ex.to_str.should == 'hello'
  end
  
  it "implements inspect as toString" do
    ex = java.lang.Exception.new('hello')
    ex.inspect.should == "java.lang.Exception: hello"
  end
end