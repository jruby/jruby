require File.dirname(__FILE__) + '/../spec_helper'

context "Module" do

  specify "const_defined? should return true if the name is defined" do
    class Blah
      class Whee
      end
    end

    Object.const_defined?(:Object).should == true
    Blah.const_defined?("Whee").should == true
    
    # MRI doesn't allow Blah::Whee
    Object.const_defined?("Blah::Whee").should == true
    Object.const_defined?("Blah::Zargle").should == false
  end

  specify "instance_methods with false should exclude super class instance methods" do
    class A
      def foo
      end
    end
    A.instance_methods(false).should == [:foo]
  end

  specify "instance_methods should return all instance methods of a module" do
    module B
      def foo
      end
    end
    B.instance_methods.should == [:foo]
  end

end
