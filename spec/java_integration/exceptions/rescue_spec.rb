require File.dirname(__FILE__) + "/../spec_helper"

java_import java.lang.OutOfMemoryError
java_import "java_integration.fixtures.ThrowExceptionInInitializer"

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
    java_ex.message.should == "(RuntimeError) error message"

    java_ex = JRuby.runtime.new_name_error("error message", "name");
    java_ex.message.should == "(NameError) error message"
  end
end

describe "A native exception wrapped by another" do  
  it "gets the first available message from the causes' chain" do
    begin
      ThrowExceptionInInitializer.new.test
    rescue NativeException => e
      e.message.should =~ /lets cause an init exception$/
    end
  end
end
