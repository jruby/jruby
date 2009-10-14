require File.dirname(__FILE__) + "/../spec_helper"

import "java.util.ArrayList"
import "java_integration.fixtures.ProtectedInstanceMethod"
import "java_integration.fixtures.ProtectedStaticMethod"
import "java_integration.fixtures.PackageInstanceMethod"
import "java_integration.fixtures.PackageStaticMethod"
import "java_integration.fixtures.PrivateInstanceMethod"
import "java_integration.fixtures.PrivateStaticMethod"
import "java_integration.fixtures.ConcreteWithVirtualCall"

describe "A Ruby subclass of a Java concrete class" do
  it "should allow access to the proxy object for the class" do
    my_arraylist = Class.new(ArrayList)
    lambda { my_arraylist.java_proxy_class }.should_not raise_error
  end

  it "should allow access to the actual generated class via java_class" do
    my_arraylist = Class.new(ArrayList)
    class_name = my_arraylist.java_proxy_class.to_s
    class_name.index('Proxy').should_not == -1
  end
end

describe "A final Java class" do
  it "should not be allowed as a superclass" do
    lambda do
      substring = Class.new(java.lang.String)
    end.should raise_error(TypeError)
  end
end

describe "A Ruby subclass of a Java class" do
  it "can invoke protected methods of the superclass" do
    subtype = Class.new(ProtectedInstanceMethod) do
      def go; theProtectedMethod; end
    end
    subtype.new.go.should == "42"

    subtype = Class.new(ProtectedInstanceMethod) do
      def go; ProtectedStaticMethod.theProtectedMethod; end
    end
    subtype.new.go.should == "42"
  end
  it "can not invoke package-visible methods of the superclass" do
    subtype = Class.new(PackageInstanceMethod) do
      def go; thePackageMethod; end
    end
    pending "Why doesn't this raise NoMethodError?" do
      lambda {subtype.new.go}.should raise_error(NoMethodError)
    end
    lambda {subtype.new.go}.should raise_error

    subtype = Class.new(PackageInstanceMethod) do
      def go; PackageStaticMethod.thePackageMethod; end
    end
    pending "Why doesn't this raise NoMethodError?" do
      lambda {subtype.new.go}.should raise_error(NoMethodError)
    end
    lambda {subtype.new.go}.should raise_error
  end
  it "can override methods that return void and return non-void value" do
    subtype = Class.new(PackageInstanceMethod) do
      def voidMethod; 123; end
    end
    subtype.new.invokeVoidMethod.should == nil
  end
  it "can not invoke private methods of the superclass" do
    subtype = Class.new(PrivateInstanceMethod) do
      def go; thePrivateMethod; end
    end
    pending "Why doesn't this raise NoMethodError?" do
      lambda {subtype.new.go}.should raise_error(NoMethodError)
    end
    lambda {subtype.new.go}.should raise_error

    subtype = Class.new(PrivateInstanceMethod) do
      def go; PrivateStaticMethod.thePrivateMethod; end
    end
    pending "Why doesn't this raise NoMethodError?" do
      lambda {subtype.new.go}.should raise_error(NoMethodError)
    end
    lambda {subtype.new.go}.should raise_error
  end
end

describe "A Ruby subclass of a Java concrete class" do
  it "can override virtually-invoked methods from super" do
    my_arraylist = Class.new(ConcreteWithVirtualCall) {
      def virtualMethod
        "derived"
      end
    }
    my_arraylist.new.callVirtualMethod.should == "derived"
  end
end