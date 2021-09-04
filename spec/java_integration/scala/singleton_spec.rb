require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.ScalaSingleton"
java_import "java_integration.fixtures.ScalaSingletonTrait"

describe "A Scala singleton" do
  describe "shadowed by a Scala class" do
    it "defines class methods from the singleton", pending: true do
      expect(ScalaSingleton.hello).to eq("Hello")
    end

    it "defines instance methods from the class" do
      expect(ScalaSingleton.new.hello).to eq("Goodbye")
    end
  end
  
  describe "shadowed by a Scala trait" do
    it "defines class methods from the singleton", pending: true do
      expect(ScalaSingletonTrait.hello).to eq("Hello")
    end
  end
end
