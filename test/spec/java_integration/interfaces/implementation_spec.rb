require File.dirname(__FILE__) + "/../spec_helper"

import "spec.java_integration.fixtures.SingleMethodInterface"
import "spec.java_integration.fixtures.UsesSingleMethodInterface"

describe "Single-method Java interfaces implemented in Ruby" do
  class ValueHolder
    include SingleMethodInterface
    def initialize(val)
      @value = val
    end
    def callIt
      @value
    end
  end
  
  it "should be kind_of? the interface" do
    ValueHolder.new(1).should be_kind_of(SingleMethodInterface)
    SingleMethodInterface.should === ValueHolder.new(1)
  end

  it "should be implemented with 'include InterfaceClass'" do
    UsesSingleMethodInterface.callIt(ValueHolder.new(1)).should == 1
  end

  it "should be cast-able to the interface on the Java side" do
    UsesSingleMethodInterface.castAndCallIt(ValueHolder.new(2)).should == 2
  end
end

describe "Single-method Java interfaces" do
  # Fails, why?
  #it "should be coerced from a passed block" do
  #  UsesSingleMethodInterface.callIt { 1 }.should == 1
  #end
  
  it "should be implementable with .impl" do
    impl = SingleMethodInterface.impl {|name| name}
    impl.should be_kind_of(SingleMethodInterface)
    SingleMethodInterface.should === impl
    
    UsesSingleMethodInterface.callIt(impl).should == :callIt
  end
end