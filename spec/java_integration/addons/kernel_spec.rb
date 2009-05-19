require File.dirname(__FILE__) + "/../spec_helper"

describe "Object extensions" do
  shared_examples_for "Object that can include classes" do
    before(:each) do 
      Object.class_eval("remove_const 'Foo'") if Object.constants.include? 'Foo'
      Object.class_eval("remove_const 'Properties'") if Object.constants.include? 'Properties'
    end

    after(:each) do
      Object.class_eval("remove_const 'Foo'") if Object.constants.include? 'Foo'
      Object.class_eval("remove_const 'Properties'") if Object.constants.include? 'Properties'
    end

    it "should allow inclusion of Java class constants in class context" do
      class Foo
        include_class java.util.Properties
      end
      Foo.constants.should include 'Properties'
    end

    it "should allow inclusion of Java class constants in module context" do 
      module Foo
        include_class java.util.Properties
      end
      Foo.constants.should include 'Properties'
    end

    it "should allow inclusion of Java class constants in instance context" do
      class Foo
        def initialize
          include_class java.util.Properties
          Properties.should == java.util.Properties
        end
      end
      Foo.new
    end
  end

  describe "(without Object#class_eval defined)" do
    it_should_behave_like "Object that can include classes"
  end
    
  describe "(with Object#class_eval defined)" do
    before(:each) do 
      class Object
        def metaclass
          class << self
            self
          end
        end

        def class_eval(*args, &block)
          metaclass.class_eval(*args, &block)
        end
      end
    end

    it_should_behave_like "Object that can include classes"

    after(:each) do
      class Object
        undef metaclass
        undef class_eval
      end
    end
  end
end
