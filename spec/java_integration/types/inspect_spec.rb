require File.dirname(__FILE__) + "/../spec_helper"

describe "A Java object's builtin inspect method" do
  it "produces the \"hashy\" inspect output" do
    o = java.lang.Object.new
    expect(o.inspect).to match(/\#<Java::JavaLang::Object:0x[0-9a-f]+>/)
  end
end

describe "java.lang.Class" do
  it "produces Ruby-style inspect" do
    klass = java.lang.String.java_class.to_java
    expect(klass.inspect).to eq '#<Java::JavaLang::Class: java.lang.String>'

    klass = Java::short[0].new.java_class.to_java
    expect(klass.inspect).to eq '#<Java::JavaLang::Class: short[]>'
  end
end

describe "java.lang.String" do
  it "inspects like a Ruby string" do
    str = java.lang.String.new 'foo'
    expect(str.inspect).to eq '"foo"'
  end
end

describe "java.lang.StringBuilder" do
  it "inspects with Ruby type" do
    str = java.lang.StringBuilder.new 'bar'
    expect(str.inspect).to eq '#<Java::JavaLang::StringBuilder: "bar">'
  end
end
