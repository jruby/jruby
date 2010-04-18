require File.dirname(__FILE__) + "/../spec_helper"

import "java.util.ArrayList"
import "java_integration.fixtures.ProtectedInstanceMethod"
import "java_integration.fixtures.ProtectedStaticMethod"
import "java_integration.fixtures.PackageInstanceMethod"
import "java_integration.fixtures.PackageStaticMethod"
import "java_integration.fixtures.PrivateInstanceMethod"
import "java_integration.fixtures.PrivateStaticMethod"
import "java_integration.fixtures.ConcreteWithVirtualCall"
import "java_integration.fixtures.ComplexPrivateConstructor"
import "java_integration.fixtures.ReceivesArrayList"

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

  # JRUBY-4451
  it "does not bind subclass constructors to match private superclass constructors" do
    subtype = Class.new(ComplexPrivateConstructor)

    obj = subtype.new("foo", 1, 2)
    obj.result.should == "String: foo, int: 1, int: 2"
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

  it "can override virtually-invoked methods from super" do
    my_arraylist = Class.new(ConcreteWithVirtualCall) {
      def virtualMethod
        "derived"
      end
    }
    my_arraylist.new.callVirtualMethod.should == "derived"
  end

  # JRUBY-4571
  it "can also include interfaces and the resulting class both extends and implements" do
    my_arraylist = Class.new(java.util.ArrayList) do
      include java.lang.Runnable

      def run; @foo = 'foo'; end
      attr_accessor :foo;
      def size; 100; end
    end.new

    ReceivesArrayList.new.receive_array_list(my_arraylist).should == 100
    
    thread = java.lang.Thread.new(my_arraylist)
    thread.start
    thread.join
    my_arraylist.foo.should == 'foo'
  end

  # JRUBY-4704
  it "still initializes properly without calling super in initialize" do
    my_arraylist_cls = Class.new(java.util.ArrayList) do
      attr_accessor :foo
      def initialize
        @foo = 'foo'
      end
    end

    my_arraylist = nil
    lambda do
      my_arraylist = my_arraylist_cls.new
    end.should_not raise_error
    my_arraylist.class.superclass.should == java.util.ArrayList
    my_arraylist.to_java.should == my_arraylist
  end
end

describe "A final Java class" do
  it "should not be allowed as a superclass" do
    lambda do
      substring = Class.new(java.lang.String)
    end.should raise_error(TypeError)
  end
end