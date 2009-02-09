require File.dirname(__FILE__) + "/../spec_helper"

import java.lang.NullPointerException

describe "Non-wrapped Java exceptions" do
  it "can be rescued" do
    exception = NullPointerException.new
    begin
      raise exception
    rescue NullPointerException => npe
    end
    
    npe.should_not == nil
    npe.should == exception
  end
end

describe "A Ruby-level exception" do
  it "carries its message along to the Java exception" do
    java_ex = JRuby.runtime.new_runtime_error("error message");
    java_ex.message.should == "error message"

    java_ex = JRuby.runtime.new_name_error("error message", "name");
    java_ex.message.should == "error message"
  end
end