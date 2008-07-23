require File.dirname(__FILE__) + "/../spec_helper"

import "java_integration.fixtures.JavaTypeMethods"

describe "A Java method returning/receiving uncoercible Java types" do
  it "wraps the objects in Ruby object wrappers" do
    # static
    obj = JavaTypeMethods.staticNewObject
    obj.class.to_s.should == "Java::JavaLang::Object"
    obj.java_object.should_not == nil

    # instance
    obj = JavaTypeMethods.new.newObject
    obj.class.to_s.should == "Java::JavaLang::Object"
    obj.java_object.should_not == nil
  end

  it "registers the wrapper and reuses it when object returns" do
    # static
    obj = JavaTypeMethods.staticNewObject
    JavaTypeMethods.staticSetObject(obj)
    obj2 = JavaTypeMethods.staticGetObject

    obj.should == obj2
    obj.object_id.should == obj2.object_id

    # instance
    jtm = JavaTypeMethods.new
    obj = jtm.newObject
    jtm.setObject(obj)
    obj2 = jtm.getObject

    obj.should == obj2
    obj.object_id.should == obj2.object_id
  end

  describe "when receiving Ruby subtypes of Java types" do
    it "registers the Ruby part of the object so it is not lost when object returns" do
      class RubySubtypeOfJavaObject < Java::java.lang.Object
        def foo; true; end
      end

      rsojo = RubySubtypeOfJavaObject.new

      # static
      JavaTypeMethods.staticSetObject(rsojo)
      rsojo2 = JavaTypeMethods.staticGetObject

      rsojo.should == rsojo2
      rsojo.object_id.should == rsojo2.object_id
      rsojo.foo.should == true
      rsojo2.foo.should == true

      # instance
      rsojo = RubySubtypeOfJavaObject.new
      jtm = JavaTypeMethods.new
      jtm.setObject(rsojo)
      rsojo2 = jtm.getObject

      rsojo.should == rsojo2
      rsojo.object_id.should == rsojo2.object_id
      rsojo.foo.should == true
      rsojo2.foo.should == true
    end
  end
end
