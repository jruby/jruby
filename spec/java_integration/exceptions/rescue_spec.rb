require File.dirname(__FILE__) + "/../spec_helper"

import java.lang.OutOfMemoryError

describe "Non-wrapped Java throwables" do
  it "can be rescued" do
    exception = OutOfMemoryError.new
    begin
      raise exception
    rescue OutOfMemoryError => oome
    end
    
    oome.should_not == nil
    oome.should == exception
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