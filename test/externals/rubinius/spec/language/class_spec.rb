# Specifications:
#
# Keywords:
#   class
#   def
#
# Behaviour:
#   Constants
#   Class variables
#   Class instance variables
#   Instance variables
#
require File.dirname(__FILE__) + '/../spec_helper'

context "A class definition" do
  specify "should create a new class" do
    class A; end
    A.new.class.should == A
  end

  specify "should allow the declaration of class variables in the body" do
    class B
      @@bar = 'foo'
    end
    B.class_variables.should == ["@@bar"]
  end

  specify "should allow the declaration of class variables in a class method" do
    class C
      def self.setup_cv
        @@bar = 'foo'
      end
    end

    before = C.class_variables
    C.setup_cv
    after = C.class_variables 
    before.should == []
    after.should == ["@@bar"]
  end
  
  specify "should allow the declaration of class variables in an instance method" do
    class D
      def setup_cv
        @@bar = 'foo'
      end
    end

    before = D.class_variables
    D.new.setup_cv
    after = D.class_variables 
    before.should == []
    after.should == ["@@bar"]
  end
end

context "In a class definition" do
  specify "def should create a new method" do
    class E
      def foo
        'foo'
      end
    end
    E.new.foo.should == 'foo'
  end
end

