require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.JavaTypeMethods"
java_import "java_integration.fixtures.InterfaceWrapper"

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

describe "Java::JavaObject.wrap" do
  it "wraps a Java object with an appropriate JavaObject subclass" do
    obj = Java::JavaObject.wrap(java.lang.Object.new)
    str = Java::JavaObject.wrap(java.lang.String.new)
    cls = Java::JavaObject.wrap(java.lang.Class.forName('java.lang.String'))

    obj.class.should == Java::JavaObject
    str.class.should == Java::JavaObject
    cls.class.should == Java::JavaClass
  end
end

describe "Java::newInterfaceImpl" do
  class BugTest
    def run
    end
  end
  class Bolt
    def run
    end
  end
  it "should use the same generated class for wrapping, on different classloaders" do
    expected1 = InterfaceWrapper.give_me_back(BugTest.new)
    expected2 = InterfaceWrapper.give_me_back(BugTest.new)
    expected1.java_class.class_loader.should_not == expected2.java_class.class_loader
    expected1.java_class.to_s.should == expected2.java_class.to_s
  end

  it "should not mix classes when generating new types for interfaces" do
    expected1 = InterfaceWrapper.give_me_back(BugTest.new)
    expected2 = InterfaceWrapper.give_me_back(Bolt.new)
    expected1.java_class.should_not == expected2.java_class
  end
end
