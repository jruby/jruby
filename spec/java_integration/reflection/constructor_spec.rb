require File.dirname(__FILE__) + "/../spec_helper"

java_import 'java_integration.fixtures.PublicConstructor'

describe "A JavaConstructor" do
  it "should instantiate" do
    ctor = PublicConstructor.java_class.declared_constructor
    expect { ctor.new_instance }.not_to raise_error
  end

  it "should instantiate using specified ctor" do
    ctor = PublicConstructor.java_class.declared_constructor Java::int
    instance = ctor.new_instance 42
    expect( instance.i ).to eq 42
  end

  it "should convert to Java" do
    ctor = PublicConstructor.java_class.constructor java.lang.Object, java.lang.Object
    instance = ctor.new_instance 42, "str"
    expect( instance.i ).to eq 42
    expect( instance.v ).to eq 'str'
    expect( instance.vClass ).to be java.lang.String.java_class
  end
end
