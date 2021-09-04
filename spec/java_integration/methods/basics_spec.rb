require File.dirname(__FILE__) + "/../spec_helper"

describe "Java instance methods" do
  it "have correct (fixed) arity" do
    expect(java.lang.String.instance_method(:length).arity).to eq(0)
    expect(java.lang.String.instance_method(:toString).arity).to eq(0)
    expect(java.lang.String.instance_method(:charAt).arity).to eq(1)

    expect(java.lang.String.instance_method(:replace).arity).to eq(2)
  end
  it "(overloads) have correct arity" do
    expect(java.lang.String.instance_method(:lastIndexOf).arity).to eq(-2)
    expect(java.lang.String.instance_method(:toLowerCase).arity).to eq(-1)

    expect(java.lang.StringBuilder.instance_method(:append).arity).to eq(-2)
    expect(java.lang.StringBuilder.instance_method(:insert).arity).to eq(-3)

    expect(java.util.ArrayList.instance_method(:toArray).arity).to eq(-1)
  end
end

describe "Java static methods" do
  it "have correct arity" do
    expect(java.lang.System.method(:exit).arity).to eq(1)
    expect(java.lang.System.method(:gc).arity).to eq(0)
    expect(java.lang.System.method(:arraycopy).arity).to eq(5)

    expect(java.util.Arrays.method(:asList).arity).to eq(-1) # (T... a)
  end
  it "(overloads) have correct arity" do
    expect(java.lang.System.method(:getProperty).arity).to eq(-2)
    # format(String format, Object... args)
    # format(Locale l, String format, Object... args)
    expect(java.lang.String.method(:format).arity).to eq(-2)
    # not fixed due valueOf(char[] data, int offset, int count)
    expect(java.lang.String.method(:valueOf).arity).to eq(-2)
  end
end

describe "JavaClass\#==" do
  it "returns true for the same java.lang.Class" do
    str_jclass = java.lang.String.java_class
    str_class = java.lang.Class.forName('java.lang.String')

    expect(str_jclass).to eq(str_class)
  end
end

describe "java.lang.Class\#==" do
  it "returns true for the same JavaClass" do
    str_jclass = java.lang.String.java_class
    str_class = java.lang.Class.forName('java.lang.String')

    expect(str_class).to eq(str_jclass)
  end
end