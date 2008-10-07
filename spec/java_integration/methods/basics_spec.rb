require File.dirname(__FILE__) + "/../spec_helper"

describe "Java instance methods" do
  it "should have Ruby arity -1" do
    lambda do
      java.lang.String.instance_method(:toString).arity.should == -1
    end.should_not raise_error
  end
end

describe "Java static methods" do
  it "should have Ruby arity -1" do
    lambda do
      java.lang.System.method(:getProperty).arity.should == -1
    end.should_not raise_error
  end
end