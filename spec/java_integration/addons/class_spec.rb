require File.dirname(__FILE__) + "/../spec_helper"
require 'java'

describe "A Java class" do
  it "has a name" do
    klass = java.lang.String.java_class.to_java
    expect(klass.name).to eq('java.lang.String')
  end

  it "improves to_s with canonical name" do
    klass = Java::boolean[1].new.java_class.to_java
    expect(klass.name).to eq('[Z')
    expect(klass.to_s).to eq('boolean[]')
  end

  it "returns name for anonymous class with to_s" do
    obj = java.util.function.Function.identity
    klass = obj.java_class.to_java
    expect(klass.name).to start_with('java.util.function.Function$')
    expect(klass.to_s).to start_with('java.util.function.Function$')
  end

  context 'java_class' do
    it "is the proxy wrapper for Java type" do
      obj = java.lang.Integer.new(0)
      expect( obj.java_class ).to equal java.lang.Class.forName('java.lang.Integer')
      expect( obj.java_class ).to equal java.lang.Integer.java_class
    end

    it "is the target for Java class proxy" do
      expect( java.lang.Integer.java_class ).to be java.lang.Class.forName('java.lang.Integer')
      expect( java.lang.Runnable.java_class ).to eql java.lang.Class.forName('java.lang.Runnable')
    end

    it "is infered for Ruby interface impl" do
      obj = Proc.new { Thread.pass }.to_java java.lang.Runnable
      expect( obj.java_class ).not_to eql java.lang.Runnable.java_class
      expect( obj.java_class.interfaces ).to include(java.lang.Runnable.java_class)
    end

    it "is the Java proxy class for Ruby sub-classed Java" do
      klass = Class.new(java.util.ArrayList)
      list = klass.new([1, 2])
      java.util.Collections.swap(list, 0, 1)
      # NOTE: this isn't accurate/consistent but it's compatible with JRuby 9.2
      expect( list.java_class ).to eql java.lang.Class.forName('java.util.ArrayList')
    end
  end

end
