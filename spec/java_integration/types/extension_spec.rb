require File.dirname(__FILE__) + "/../spec_helper"

import "java.util.ArrayList"

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
