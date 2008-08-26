require File.dirname(__FILE__) + "/../spec_helper"

import "java_integration.fixtures.CoreTypeMethods"

describe "Non-overloaded static Java methods" do
  it "should raise ArgumentError when called with incorrect arity" do
    lambda do
      java.util.Collections.empty_list('foo')
    end.should raise_error(ArgumentError)
  end
end

describe "Overloaded static Java methods" do
  obj = java.lang.Integer.new(1)

  it "call more long instead of double" do
    pending do
      CoreTypeMethods.getType(1, obj).should == "long,object"
    end
  end

  it "call more double instead of long" do
    pending do
      CoreTypeMethods.getType(1.0, obj).should == "double,object"
    end
  end

  obj = "heh"

  it "call more long w/ string instead of long w/ object" do
    CoreTypeMethods.getType(1, obj).should == "long,string"
  end

  it "call more double w/ string instead of long w/ string" do
    CoreTypeMethods.getType(1.0, obj).should == "double,string"
  end

end
