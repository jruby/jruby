require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.JavaTypeMethods"
java_import "java_integration.fixtures.InterfaceWrapper"

describe "A Java method returning/receiving uncoercible Java types" do
  it "wraps the objects in Ruby object wrappers" do
    # static
    obj = JavaTypeMethods.staticNewObject
    expect(obj.class.to_s).to eq("Java::JavaLang::Object")
    expect(obj.java_object).not_to eq(nil)

    # instance
    obj = JavaTypeMethods.new.newObject
    expect(obj.class.to_s).to eq("Java::JavaLang::Object")
    expect(obj.java_object).not_to eq(nil)
  end

  it "registers the wrapper and reuses it when object returns" do
    # static
    obj = JavaTypeMethods.staticNewObject
    JavaTypeMethods.staticSetObject(obj)
    obj2 = JavaTypeMethods.staticGetObject

    expect(obj).to eq(obj2)
    expect(obj.object_id).to eq(obj2.object_id)

    # instance
    jtm = JavaTypeMethods.new
    obj = jtm.newObject
    jtm.setObject(obj)
    obj2 = jtm.getObject

    expect(obj).to eq(obj2)
    expect(obj.object_id).to eq(obj2.object_id)
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

      expect(rsojo).to eq(rsojo2)
      expect(rsojo.object_id).to eq(rsojo2.object_id)
      expect(rsojo.foo).to eq(true)
      expect(rsojo2.foo).to eq(true)

      # instance
      rsojo = RubySubtypeOfJavaObject.new
      jtm = JavaTypeMethods.new
      jtm.setObject(rsojo)
      rsojo2 = jtm.getObject

      expect(rsojo).to eq(rsojo2)
      expect(rsojo.object_id).to eq(rsojo2.object_id)
      expect(rsojo.foo).to eq(true)
      expect(rsojo2.foo).to eq(true)
    end
  end

  describe "with persistence off" do
    before { java.lang.Object.__persistent__ = false }
    let(:object_proxy_cache) { JRuby.runtime.java_support.object_proxy_cache }

    it "doesn't cache the proxy when directly constructed" do
      object = java.lang.Object.new
      expect(object_proxy_cache.get(object)).to be_nil
    end

    it "doesn't cache the proxy when retrieved from a Java instance method" do
      object = JavaTypeMethods.new.newObject
      expect(object_proxy_cache.get(object)).to be_nil
    end

    it "doesn't cache the proxy when retrieved from a Java static method" do
      object = JavaTypeMethods.staticNewObject
      expect(object_proxy_cache.get(object)).to be_nil
    end
  end

  describe "with persistence on" do
    before { java.lang.Object.__persistent__ = true }
    after { java.lang.Object.__persistent__ = false }
    let(:object_proxy_cache) { JRuby.runtime.java_support.object_proxy_cache }

    it "doesn't cache the proxy when directly constructed" do
      object = java.lang.Object.new
      expect(object_proxy_cache.get(object)).to eq(object)
    end

    it "doesn't cache the proxy when retrieved from a Java instance method" do
      object = JavaTypeMethods.new.newObject
      expect(object_proxy_cache.get(object)).to eq(object)
    end

    it "doesn't cache the proxy when retrieved from a Java static method" do
      object = JavaTypeMethods.staticNewObject
      expect(object_proxy_cache.get(object)).to eq(object)
    end
  end
end

describe "Java::JavaObject.wrap" do
  it "wraps a Java object with an appropriate JavaObject subclass" do
    obj = Java::JavaObject.wrap(java.lang.Object.new)
    str = Java::JavaObject.wrap(java.lang.String.new)
    cls = Java::JavaObject.wrap(java.lang.Class.forName('java.lang.String'))

    expect(obj.class).to eq(Java::JavaObject)
    expect(str.class).to eq(Java::JavaObject)
    expect(cls.class).to eq(Java::JavaClass)
  end
end

describe "Java::newInterfaceImpl" do
  class BugTest
    def run
    end
  end

  it "should use the same generated class for wrapping, on different classloaders" do
    expected1 = InterfaceWrapper.give_me_back(BugTest.new)
    expected2 = InterfaceWrapper.give_me_back(BugTest.new)
    unless java.lang.reflect.Proxy.isProxyClass(expected1.java_class)
      expect(expected1.java_class.class_loader).not_to eq(expected2.java_class.class_loader)
    end
    expect(expected1.java_class.to_s).to eq(expected2.java_class.to_s)
  end

  it "should not mix classes when generating new types for interfaces" do
    a_klass = Class.new { def run; end }
    expected1 = InterfaceWrapper.give_me_back(BugTest.new)
    expected2 = InterfaceWrapper.give_me_back(a_klass.new)
    # in case of proxy based interface implementations this won't hold
    # generated java-class might be the same (instances using different handlers)
    unless java.lang.reflect.Proxy.isProxyClass(expected1.java_class)
      expect(expected1.java_class).not_to eq(expected2.java_class)
    end
    expect(expected1.to_java.equals(expected2.to_java)).to be false
    expect(expected1.to_java.hashCode).not_to eql expected2.to_java.hashCode
  end
end
