require File.dirname(__FILE__) + "/../spec_helper"

import "java_integration.fixtures.CoreTypeMethods"
import "java_integration.fixtures.StaticMethodSelection"

describe "Non-overloaded static Java methods" do
  it "should raise ArgumentError when called with incorrect arity" do
    lambda do
      java.util.Collections.empty_list('foo')
    end.should raise_error(ArgumentError)
  end
end

describe "An overloaded Java static method" do
  it "should be called with the most exact overload" do
    obj = java.lang.Integer.new(1)
    CoreTypeMethods.getType(1).should == "long"
    CoreTypeMethods.getType(1, obj).should == "long,object"
    CoreTypeMethods.getType(1, obj, obj).should == "long,object,object"
    CoreTypeMethods.getType(1, obj, obj, obj).should == "long,object,object,object"
    CoreTypeMethods.getType(1.0).should == "double"
    CoreTypeMethods.getType(1.0, obj).should == "double,object"
    CoreTypeMethods.getType(1.0, obj, obj).should == "double,object,object"
    CoreTypeMethods.getType(1.0, obj, obj, obj).should == "double,object,object,object"
    
    obj = "foo"
    CoreTypeMethods.getType(1).should == "long"
    CoreTypeMethods.getType(1, obj).should == "long,string"
    CoreTypeMethods.getType(1, obj, obj).should == "long,string,string"
    CoreTypeMethods.getType(1, obj, obj, obj).should == "long,string,string,string"
    CoreTypeMethods.getType(1.0).should == "double"
    CoreTypeMethods.getType(1.0, obj).should == "double,string"
    CoreTypeMethods.getType(1.0, obj, obj).should == "double,string,string"
    CoreTypeMethods.getType(1.0, obj, obj, obj).should == "double,string,string,string"
  end

  it "should raise error when called with too many args" do
    lambda do
      obj = java.lang.Integer.new(1)
      CoreTypeMethods.getType(1, obj, obj, obj, obj)
    end.should raise_error(ArgumentError)
      
    lambda do
      obj = "foo"
      CoreTypeMethods.getType(1, obj, obj, obj, obj)
    end.should raise_error(ArgumentError)
  end

  it "should raise error when called with too few args" do
    pending do
      lambda do
        CoreTypeMethods.getType()
      end.should raise_error(ArgumentError)

      lambda do
        CoreTypeMethods.getType()
      end.should raise_error(ArgumentError)
    end
  end
end

describe "The return value of an overridden Java static method" do
  before(:each) do
    @return_value = StaticMethodSelection.produce
  end
  it "should not be nil" do
    @return_value.should_not be_nil
  end
  it "should be of the correct type" do
    @return_value.should be_an_instance_of(StaticMethodSelection)
  end 
end

describe "An overloaded Java instance method" do
  it "should be called with the most exact overload" do
    obj = java.lang.Integer.new(1)
    ctm = CoreTypeMethods.new
    ctm.getTypeInstance(1).should == "long"
    ctm.getTypeInstance(1, obj).should == "long,object"
    ctm.getTypeInstance(1, obj, obj).should == "long,object,object"
    ctm.getTypeInstance(1, obj, obj, obj).should == "long,object,object,object"
    ctm.getTypeInstance(1.0).should == "double"
    ctm.getTypeInstance(1.0, obj).should == "double,object"
    ctm.getTypeInstance(1.0, obj, obj).should == "double,object,object"
    ctm.getTypeInstance(1.0, obj, obj, obj).should == "double,object,object,object"
    
    obj = "foo"
    ctm = CoreTypeMethods.new
    ctm.getTypeInstance(1).should == "long"
    ctm.getTypeInstance(1, obj).should == "long,string"
    ctm.getTypeInstance(1, obj, obj).should == "long,string,string"
    ctm.getTypeInstance(1, obj, obj, obj).should == "long,string,string,string"
    ctm.getTypeInstance(1.0).should == "double"
    ctm.getTypeInstance(1.0, obj).should == "double,string"
    ctm.getTypeInstance(1.0, obj, obj).should == "double,string,string"
    ctm.getTypeInstance(1.0, obj, obj, obj).should == "double,string,string,string"
  end

  it "should raise error when called with too many args" do
    lambda do
      obj = java.lang.Integer.new(1)
      CoreTypeMethods.new.getTypeInstance(1, obj, obj, obj, obj)
    end.should raise_error(ArgumentError)
      
    lambda do
      obj = "foo"
      CoreTypeMethods.new.getTypeInstance(1, obj, obj, obj, obj)
    end.should raise_error(ArgumentError)
  end

  it "should raise error when called with too few args" do
    pending "not sure why these are failing" do
      lambda do
        CoreTypeMethods.new.getTypeInstance()
      end.should raise_error(ArgumentError)

      lambda do
        CoreTypeMethods.new.getTypeInstance()
      end.should raise_error(ArgumentError)
    end
  end
end