require File.expand_path('../../../spec_helper', __FILE__)

module ObjectSpecs
  module SomeOtherModule; end
  module AncestorModule; end
  module MyModule; end

  class AncestorClass < String
    include AncestorModule
  end

  class InstanceClass < AncestorClass
    include MyModule
  end
end

describe Object, "#instance_of?" do
  before(:each) do
    @o = ObjectSpecs::InstanceClass.new
  end

  it "returns true if given class is object's class" do
    @o.instance_of?(ObjectSpecs::InstanceClass).should == true
    [].instance_of?(Array).should == true
    ''.instance_of?(String).should == true
  end

  it "returns false if given class is object's ancestor class" do
    @o.instance_of?(ObjectSpecs::AncestorClass).should == false
  end

  it "returns false if given class is not object's class nor object's ancestor class" do
    @o.instance_of?(Array).should == false
  end

  it "returns false if given a Module that is included in object's class" do
    @o.instance_of?(ObjectSpecs::MyModule).should == false
  end

  it "returns false if given a Module that is included one of object's ancestors only" do
    @o.instance_of?(ObjectSpecs::AncestorModule).should == false
  end

  it "returns false if given a Module that is not included in object's class" do
    @o.instance_of?(ObjectSpecs::SomeOtherModule).should == false
  end

  it "raises a TypeError if given an object that is not a Class nor a Module" do
    lambda { @o.instance_of?(Object.new) }.should raise_error(TypeError)
    lambda { @o.instance_of?('ObjectSpecs::InstanceClass') }.should raise_error(TypeError)
    lambda { @o.instance_of?(1) }.should raise_error(TypeError)
  end
end
