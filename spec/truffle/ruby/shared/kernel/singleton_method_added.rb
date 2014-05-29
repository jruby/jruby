require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../fixtures/kernel/singleton_method', __FILE__)

describe :singleton_method_added, :shared => true do

  before :each do
    ScratchPad.clear
  end

  it "is a private method" do
    @object.should have_private_instance_method(@method)
  end

  it "is called when a method is defined on self" do
    class KernelSpecs::SingletonMethod
      def self.new_method_on_self
      end
    end
    ScratchPad.recorded.should == [:method_added, :new_method_on_self]
  end

  it "is called when a method is defined in the singleton class" do
    class KernelSpecs::SingletonMethod
      class << self
        def new_method_on_singleton
        end
      end
    end
    ScratchPad.recorded.should == [:method_added, :new_method_on_singleton]
  end

  it "is called when define_method is used in the singleton class" do
    class KernelSpecs::SingletonMethod
      class << self
        define_method :new_method_with_define_method do
        end
      end
    end
    ScratchPad.recorded.should == [:method_added, :new_method_with_define_method]
  end

end
