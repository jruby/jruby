require File.dirname(__FILE__) + "/../spec_helper"

import "java_integration.fixtures.SingleMethodInterface"
import "java_integration.fixtures.UsesSingleMethodInterface"
import "java_integration.fixtures.DescendantOfSingleMethodInterface"
import "java_integration.fixtures.UsesDescendantOfSingleMethodInterface"

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
  it "can be coerced from a block passed to a static method" do
    UsesSingleMethodInterface.callIt { 1 }.should == 1
  end
  
  it "can be coerced from a block passed to a instance method" do
    UsesSingleMethodInterface.new.callIt2 { 1 }.should == 1
  end
  
  it "should be implementable with .impl" do
    impl = SingleMethodInterface.impl {|name| name}
    impl.should be_kind_of(SingleMethodInterface)
    SingleMethodInterface.should === impl
    
    UsesSingleMethodInterface.callIt(impl).should == :callIt
  end
end

describe "A Ruby class including a descendant interface" do
  it "implements all methods from that interface and parents" do
    impl = Class.new do
      include DescendantOfSingleMethodInterface
      
      def callIt; "foo"; end
      def callThat; "bar"; end
    end
    
    dosmi = impl.new
    
    UsesSingleMethodInterface.callIt(dosmi).should == "foo"
    UsesDescendantOfSingleMethodInterface.callThat(dosmi).should == "bar"
  end
end