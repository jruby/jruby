require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Module#ancestors" do
  it "returns a list of modules included in self (including self)" do
    ModuleSpecs.ancestors.should         include(ModuleSpecs)
    ModuleSpecs::Basic.ancestors.should  include(ModuleSpecs::Basic)
    ModuleSpecs::Super.ancestors.should  include(ModuleSpecs::Super, ModuleSpecs::Basic)
    ModuleSpecs::Parent.ancestors.should include(ModuleSpecs::Parent, Object, Kernel)
    ModuleSpecs::Child.ancestors.should  include(ModuleSpecs::Child, ModuleSpecs::Super, ModuleSpecs::Basic, ModuleSpecs::Parent, Object, Kernel)
  end

  it "returns only modules and classes" do
    class << ModuleSpecs::Child; self; end.ancestors.should include(ModuleSpecs::Internal, Class, Module, Object, Kernel)
  end

  it "has 1 entry per module or class" do
    ModuleSpecs::Parent.ancestors.should == ModuleSpecs::Parent.ancestors.uniq
  end

  describe "when called on a singleton class" do
    ruby_version_is "2.1" do
      it "includes the singleton classes of ancestors" do
        Parent  = Class.new
        Child   = Class.new(Parent)
        SChild  = Child.singleton_class

        SChild.ancestors.should include(SChild,
                                        Parent.singleton_class,
                                        Object.singleton_class,
                                        BasicObject.singleton_class,
                                        Class,
                                        Module,
                                        Object,
                                        Kernel,
                                        BasicObject)

      end
    end
  end
end
