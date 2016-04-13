require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.CachedInJava"

describe "A singleton method added to a Java object from Ruby" do
  before(:all) { CachedInJava.__persistent__ = true }

  it "caches the proxy when directly constructed" do
    object = CachedInJava.new
    def object.answer; 42; end
    expect(object.answer).to eq(42)
    expect(CachedInJava.last_instance).to eq(object)
    expect(CachedInJava.last_instance.answer).to eq(42)
  end

  it "caches the proxy when retrieved from a Java instance method" do
    object = CachedInJava.new.new_instance_from_instance
    def object.answer; 42; end
    expect(object.answer).to eq(42)
    expect(CachedInJava.last_instance).to eq(object)
    expect(CachedInJava.last_instance.answer).to eq(42)
  end

  it "caches the proxy when retrieved from a Java static method" do
    object = CachedInJava.new_instance_from_static
    def object.answer; 42; end
    expect(object.answer).to eq(42)
    expect(CachedInJava.last_instance).to eq(object)
    expect(CachedInJava.last_instance.answer).to eq(42)
  end
end
