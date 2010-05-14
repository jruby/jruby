require File.dirname(__FILE__) + "/../spec_helper"

describe "Java instance methods" do
  it "should have Ruby arity -1" do
    lambda do
      java.lang.String.instance_method(:toString).arity.should == -1
    end.should_not raise_error
  end
end

describe "Java static methods" do
  it "should have Ruby arity -1" do
    lambda do
      java.lang.System.method(:getProperty).arity.should == -1
    end.should_not raise_error
  end
end

describe "JavaClass\#==" do
  it "returns true for the same java.lang.Class" do
    str_jclass = java.lang.String.java_class
    str_class = java.lang.Class.forName('java.lang.String')

    str_jclass.should == str_class
  end
end

describe "java.lang.Class\#==" do
  it "returns true for the same JavaClass" do
    str_jclass = java.lang.String.java_class
    str_class = java.lang.Class.forName('java.lang.String')

    str_class.should == str_jclass
  end
end