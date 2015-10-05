# specs for JRUBY-4680
require File.dirname(__FILE__) + "/../spec_helper"

java_import 'java_integration.fixtures.ClassWithMultipleSignaturesWithPrimitiveArgs'
java_import 'java_integration.fixtures.GenericComparable'

describe "JRuby" do
  before :each do
    @int = 0x135
    @float =3.125
  end

  C = ClassWithMultipleSignaturesWithPrimitiveArgs # for brevity
  it "chooses most appropriate method for a given primitive argument" do
    expect(C.foo1(@int)).to eq("int")
    expect(C.foo1(@float)).to eq("float")
    expect(C.foo2(Object.new, @int)).to eq("int")
    expect(C.foo2(Object.new, @float)).to eq("float")
  end
end

describe "A class with two inexact overloads" do
  it "will have the most specific overload called" do
    expect(GenericComparable.new.compareTo(0)).to eq(0)
    expect(GenericComparable.new.compareTo('foo')).to eq(1)
  end
end