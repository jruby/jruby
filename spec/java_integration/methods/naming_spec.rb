require File.dirname(__FILE__) + "/../spec_helper"

import "java_integration.fixtures.MethodNames"

describe "Java static method names" do
  it "should present as both camel-case and ruby-case" do
    methods = MethodNames.methods
    
    methods.should include("lowercase1")
    methods.should include("camelCase1")
    methods.should include("camel_case1")
    methods.should include("camelWithUPPER1")
    methods.should include("camel_with_upper1")
    methods.should include("camelWITHUpper1")
    methods.should include("CAMELWithUpper1")
    
    pending("broken") do
      methods.should_not include("camel_withupper1")
      methods.should_not include("camelwith_upper1")
    end
  end
  
  it "should present javabean properties as attribute readers and writers" do
    methods = MethodNames.methods
    
    methods.should include("getValue1")
    methods.should include("get_value1")
    methods.should include("value1")
    
    methods.should include("setValue1")
    methods.should include("set_value1")
    methods.should include("value1=")
    
    methods.should include("getValues1")
    methods.should include("get_values1")
    methods.should_not include("values1")
    
    methods.should include("setValues1")
    methods.should include("set_values1")
    methods.should_not include("values1=")
  end

  it "should present boolean javabean property accessors as '?' method" do
    methods = MethodNames.methods
    
    methods.should include("isFirst1")
    methods.should include("first1")
    methods.should include("first1?")

    methods.should include("isSecond1")
    methods.should include("second1")
    methods.should include("second1?")
    
    methods.should include("hasThird1")
    methods.should include("has_third1")
    methods.should include("has_third1?")

    methods.should include("hasFourth1")
    methods.should include("has_fourth1");
    methods.should include("has_fourth1?");

    pending("not implemented") do
      methods.should include("third1?")
    end
    methods.should_not include("fourth1?")
  end
  
  it "should not overwrite critical core Ruby methods" do
    pending("need a better way to separate class and instance methods in the java code")
  end
end

describe "Java instance method names" do
  it "should present as both camel-case and ruby-case" do
    methods = MethodNames.instance_methods
    
    methods.should include("lowercase2")
    methods.should include("camelCase2")
    methods.should include("camel_case2")
    methods.should include("camelWithUPPER2")
    methods.should include("camel_with_upper2")
    methods.should include("camelWITHUpper2")
    methods.should include("CAMELWithUpper2")
    
    pending("broken") do
      methods.should_not include("camel_withupper2")
      methods.should_not include("camelwith_upper2")
    end
  end
  
  it "should present javabean properties as attribute readers and writers" do
    methods = MethodNames.instance_methods
    
    methods.should include("getValue2")
    methods.should include("get_value2")
    methods.should include("value2")
    
    methods.should include("setValue2")
    methods.should include("set_value2")
    methods.should include("value2=")
    
    methods.should include("getValues2")
    methods.should include("get_values2")
    methods.should_not include("values2")
    
    methods.should include("setValues2")
    methods.should include("set_values2")
    methods.should_not include("values2=")
  end
  
  it "should treat consecutive caps as part of one property name" do
    methods = MethodNames.instance_methods

    methods.should include("jconsecutive_caps")
    methods.should include("jconsecutive_caps=")
  end
  
  it "should present boolean javabean property accessors as '?' method" do
    methods = MethodNames.instance_methods
    
    methods.should include("isFirst2")
    methods.should include("first2")
    methods.should include("first2?")

    methods.should include("isSecond2")
    methods.should include("second2")
    methods.should include("second2?")
    
    methods.should include("hasThird2")
    methods.should include("has_third2")
    methods.should include("has_third2?")

    methods.should include("hasFourth2")
    methods.should include("has_fourth2")
    methods.should include("has_fourth2?")

    pending("not implemented") do
      methods.should include("third2?")
    end
    methods.should_not include("fourth2?")
  end
  
  it "should not overwrite critical core Ruby methods" do
    obj = MethodNames.new
    
    obj.send(:initialize).should_not == "foo"
    obj.object_id.should_not == "foo"
    obj.__id__.should_not == "foo"
    lambda {obj.__send__}.should raise_error(ArgumentError)
  end

  it "should be able to access Java methods of core Ruby Methods via $method" do
    MethodNames.new.initialize__method.should == "foo"
    MethodNames.inspect__method.should == "foo"
  end
end

describe "Needed implementation methods for concrete classes" do 
  it "should have __id__ method" do 
    ArrayReceiver.new.methods.should include("__id__")
  end
  it "should have __send__ method" do 
    ArrayReceiver.new.methods.should include("__send__")
  end
  it "should have == method" do 
    ArrayReceiver.new.methods.should include("==")
  end
  it "should have inspect method" do 
    ArrayReceiver.new.methods.should include("inspect")
  end
  it "should have respond_to? method" do 
    ArrayReceiver.new.methods.should include("respond_to?")
  end
  it "should have class method" do 
    ArrayReceiver.new.methods.should include("class")
  end
  it "should have methods method" do 
    ArrayReceiver.new.methods.should include("methods")
  end
  it "should have send method" do 
    ArrayReceiver.new.methods.should include("send")
  end
  it "should have equal? method" do 
    ArrayReceiver.new.methods.should include("equal?")
  end
  it "should have eql? method" do 
    ArrayReceiver.new.methods.should include("eql?")
  end
  it "should have to_s method" do 
    ArrayReceiver.new.methods.should include("to_s")
  end
end

describe "Needed implementation methods for interfaces" do 
  it "should have __id__ method" do 
    BeanLikeInterface.new.methods.should include("__id__")
  end
  it "should have __send__ method" do 
    BeanLikeInterface.new.methods.should include("__send__")
  end
  it "should have == method" do 
    BeanLikeInterface.new.methods.should include("==")
  end
  it "should have inspect method" do 
    BeanLikeInterface.new.methods.should include("inspect")
  end
  it "should have respond_to? method" do 
    BeanLikeInterface.new.methods.should include("respond_to?")
  end
  it "should have class method" do 
    BeanLikeInterface.new.methods.should include("class")
  end
  it "should have methods method" do 
    BeanLikeInterface.new.methods.should include("methods")
  end
  it "should have send method" do 
    BeanLikeInterface.new.methods.should include("send")
  end
  it "should have equal? method" do 
    BeanLikeInterface.new.methods.should include("equal?")
  end
  it "should have eql? method" do 
    BeanLikeInterface.new.methods.should include("eql?")
  end
  it "should have to_s method" do 
    BeanLikeInterface.new.methods.should include("to_s")
  end
end
