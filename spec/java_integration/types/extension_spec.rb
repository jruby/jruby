require File.dirname(__FILE__) + "/../spec_helper"

import "java.util.ArrayList"
import "java_integration.fixtures.ProtectedInstanceMethod"
import "java_integration.fixtures.ProtectedStaticMethod"

describe "A Ruby subclass of a Java concrete class" do
  it "should allow access to the proxy object for the class" do
    my_arraylist = Class.new(ArrayList)
    lambda { my_arraylist.java_proxy_class }.should_not raise_error
  end

  it "should allow access to the actual generated class via java_class" do
    my_arraylist = Class.new(ArrayList)
    class_name = my_arraylist.java_proxy_class.to_s
    class_name.index('Proxy').should_not == -1
  end
end

describe "A final Java class" do
  it "should not be allowed as a superclass" do
    lambda do
      substring = Class.new(java.lang.String)
      substring.new
    end.should raise_error(TypeError)
  end
end

describe "A Ruby subclass of a Java class" do
  it "can invoke protected methods of the superclass" do
#    pending "Invoking protected methods from subclasses does not work yet" do
      subtype = Class.new(ProtectedInstanceMethod) do
        def go; theProtectedMethod; end
      end
      subtype.new.go.should == "42"
      
      subtype = Class.new(ProtectedInstanceMethod) do
        def go; ProtectedStaticMethod.theProtectedMethod; end
      end
      subtype.new.go.should == "42"
#    end
  end
end