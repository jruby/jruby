require File.dirname(__FILE__) + "/../spec_helper"

java_import "java.util.ArrayList"
java_import "java_integration.fixtures.ProtectedInstanceMethod"
java_import "java_integration.fixtures.ProtectedStaticMethod"
java_import "java_integration.fixtures.PackageInstanceMethod"
java_import "java_integration.fixtures.PackageStaticMethod"
java_import "java_integration.fixtures.PrivateInstanceMethod"
java_import "java_integration.fixtures.PrivateStaticMethod"
java_import "java_integration.fixtures.ConcreteWithVirtualCall"
java_import "java_integration.fixtures.ComplexPrivateConstructor"
java_import "java_integration.fixtures.ReceivesArrayList"
java_import "java_integration.fixtures.ClassWithAbstractMethods"

describe "A Ruby subclass of a Java concrete class" do
  it "should allow access to the proxy object for the class" do
    my_arraylist = Class.new(ArrayList)
    expect { my_arraylist.java_proxy_class }.not_to raise_error
  end

  it "should allow access to the actual generated class via java_class" do
    my_arraylist = Class.new(ArrayList)
    class_name = my_arraylist.java_proxy_class.to_s
    expect(class_name.index('Proxy')).not_to eq(-1)
  end

  it "can invoke protected methods of the superclass" do
    subtype = Class.new(ProtectedInstanceMethod) do
      def go; theProtectedMethod; end
    end
    expect(subtype.new.go).to eq("42")

    subtype = Class.new(ProtectedInstanceMethod) do
      def go; ProtectedStaticMethod.theProtectedMethod; end
    end
    expect(subtype.new.go).to eq("42")
  end

  it "can not invoke package-visible methods of the superclass" do
    subtype = Class.new(PackageInstanceMethod) do
      def go; thePackageMethod; end
    end
    expect {subtype.new.go}.to raise_error(NameError)

    subtype = Class.new(PackageInstanceMethod) do
      def go; PackageStaticMethod.thePackageMethod; end
    end
    expect {subtype.new.go}.to raise_error(NameError)

    skip "these should raise NoMethodError"
  end

  # JRUBY-4451
  it "does not bind subclass constructors to match private superclass constructors" do
    subtype = Class.new(ComplexPrivateConstructor)

    obj = subtype.new("foo", 1, 2)
    expect(obj.result).to eq("String: foo, int: 1, int: 2")
  end

  it "can override methods that return void and return non-void value" do
    subtype = Class.new(PackageInstanceMethod) do
      def voidMethod; 123; end
    end
    expect(subtype.new.invokeVoidMethod).to eq(nil)
  end

  it "can not invoke private methods of the superclass" do
    subtype = Class.new(PrivateInstanceMethod) do
      def go; thePrivateMethod; end
    end
    expect {subtype.new.go}.to raise_error(NameError)

    pending "this should raise NoMethodError"
    subtype = Class.new(PrivateInstanceMethod) do
      def go; PrivateStaticMethod.thePrivateMethod; end
    end
    expect {subtype.new.go}.to raise_error(NoMethodError)
  end

  it "can override virtually-invoked methods from super" do
    my_arraylist = Class.new(ConcreteWithVirtualCall) {
      def virtualMethod
        "derived"
      end
    }
    expect(my_arraylist.new.callVirtualMethod).to eq("derived")
  end

  # JRUBY-4571
  it "can also include interfaces and the resulting class both extends and implements" do
    my_arraylist = Class.new(java.util.ArrayList) do
      include java.lang.Runnable

      def run; @foo = 'foo'; end
      attr_accessor :foo;
      def size; 100; end
    end.new

    expect(ReceivesArrayList.new.receive_array_list(my_arraylist)).to eq(100)

    thread = java.lang.Thread.new(my_arraylist)
    thread.start
    thread.join
    expect(my_arraylist.foo).to eq('foo')
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
    expect do
      my_arraylist = my_arraylist_cls.new
    end.not_to raise_error
    expect(my_arraylist.class.superclass).to eq(java.util.ArrayList)
    expect(my_arraylist.to_java).to eq(my_arraylist)
  end

  it "raises argument error when super does not match superclass constructor arity" do
    my_arraylist_cls = Class.new(java.util.ArrayList) do
      def initialize
        super('foo', 'foo', 'foo')
      end
    end

    expect do
      my_arraylist_cls.new
    end.to raise_error(ArgumentError)
  end

  # JRUBY-4788
  it "raises argument error if no matching arity method has been implemented on class or superclass" do
    my_cwam_cls = Class.new(ClassWithAbstractMethods) do
      # arity should be 1, mismatch is intentional
      def foo1
        "ok"
      end
    end
    my_cwam = my_cwam_cls.new

    expect do
      ClassWithAbstractMethods.callFoo1(my_cwam, "ok")
    end.to raise_error(ArgumentError)
  end

  it "dispatches to other-arity superclass methods if arities mismatch" do
    my_cwam_cls = Class.new(ClassWithAbstractMethods) do
      # arity should be 2, mismatch is intentional
      def foo2(arg)
        "bad"
      end
    end
    my_cwam = my_cwam_cls.new

    expect(ClassWithAbstractMethods.callFoo2(my_cwam, "x", "y")).to eq("ok")
  end
end

describe "A final Java class" do
  it "should not be allowed as a superclass" do
    expect do
      Class.new(java.lang.String)
    end.to raise_error(TypeError)
  end
end
