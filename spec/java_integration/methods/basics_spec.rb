require File.dirname(__FILE__) + "/../spec_helper"

describe "Java instance methods" do
  it "should have Ruby arity -1" do
    expect do
      expect(java.lang.String.instance_method(:toString).arity).to eq(-1)
    end.not_to raise_error
  end
end

describe "Java static methods" do
  it "should have Ruby arity -1" do
    expect do
      expect(java.lang.System.method(:getProperty).arity).to eq(-1)
    end.not_to raise_error
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