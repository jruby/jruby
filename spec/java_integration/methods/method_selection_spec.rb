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
    C.foo1(@int).should == "int"
    C.foo1(@float).should == "float"
    C.foo2(Object.new, @int).should == "int"
    C.foo2(Object.new, @float).should == "float"
  end
end

describe "A class with two inexact overloads" do
  it "will have the most specific overload called" do
    GenericComparable.new.compareTo(0).should == 0
    GenericComparable.new.compareTo('foo').should == 1
  end
end