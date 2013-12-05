require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.ScalaSingleton"
java_import "java_integration.fixtures.ScalaSingletonTrait"

describe "A Scala singleton" do
  describe "shadowed by a Scala class" do
    it "defines class methods from the singleton" do
      ScalaSingleton.hello.should == "Hello"
    end
    
    it "defines instance methods from the class" do
      ScalaSingleton.new.hello.should == "Goodbye"
    end
  end
  
  describe "shadowed by a Scala trait" do
    it "defines class methods from the singleton" do
      ScalaSingletonTrait.hello.should == "Hello"
    end
  end
end
