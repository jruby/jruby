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

  it "should be implemented with 'include InterfaceClass'" do
    UsesSingleMethodInterface.callIt(ValueHolder.new(1)).should == 1
  end

  it "should be cast-able to the interface on the Java side" do
    UsesSingleMethodInterface.castAndCallIt(ValueHolder.new(2)).should == 2
  end
end