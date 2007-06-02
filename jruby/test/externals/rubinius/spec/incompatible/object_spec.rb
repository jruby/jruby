require File.dirname(__FILE__) + '/../spec_helper'

describe "Object instance method coerce_string" do
  specify "raises TypeError if the instance does not respond to to_str" do
    class A; end
    should_raise(TypeError) { A.new.coerce_string }
  end
  
  specify "raises TypeError for nil" do
    should_raise(TypeError) { nil.coerce_string }
  end
  
  specify "raises TypeError for a Fixnum" do
    should_raise(TypeError) { 1.coerce_string }
  end
  
  specify "raises TypeError if the instance responds to to_str but doesn't return a String" do
    class B
      def to_str
        1
      end
    end
    should_raise(TypeError) { B.new.coerce_string }
  end
  
  specify "calls to_str if the instance responds to it" do
    class C
      def to_str
        "zam"
      end
    end
    C.new.coerce_string.should == "zam"
  end
end
