require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Module#protected_method_defined?" do
  it "returns true if the named protected method is defined by module or its ancestors" do
    ModuleSpecs::CountsMixin.protected_method_defined?("protected_3").should == true

    ModuleSpecs::CountsParent.protected_method_defined?("protected_3").should == true
    ModuleSpecs::CountsParent.protected_method_defined?("protected_2").should == true

    ModuleSpecs::CountsChild.protected_method_defined?("protected_3").should == true
    ModuleSpecs::CountsChild.protected_method_defined?("protected_2").should == true
    ModuleSpecs::CountsChild.protected_method_defined?("protected_1").should == true
  end

  it "returns false if method is not a protected method" do
    ModuleSpecs::CountsChild.protected_method_defined?("public_3").should == false
    ModuleSpecs::CountsChild.protected_method_defined?("public_2").should == false
    ModuleSpecs::CountsChild.protected_method_defined?("public_1").should == false

    ModuleSpecs::CountsChild.protected_method_defined?("private_3").should == false
    ModuleSpecs::CountsChild.protected_method_defined?("private_2").should == false
    ModuleSpecs::CountsChild.protected_method_defined?("private_1").should == false
  end

  it "returns false if the named method is not defined by the module or its ancestors" do
    ModuleSpecs::CountsMixin.protected_method_defined?(:protected_10).should == false
  end

  it "accepts symbols for the method name" do
    ModuleSpecs::CountsMixin.protected_method_defined?(:protected_3).should == true
  end

  not_compliant_on :rubinius do
    ruby_version_is ""..."1.9" do
      it "raises an ArgumentError if passed a Fixnum" do
        lambda {
          ModuleSpecs::CountsMixin.protected_method_defined?(1)
        }.should raise_error(ArgumentError)
      end

      it "raises a TypeError if not passed a Symbol" do
        lambda {
          ModuleSpecs::CountsMixin.protected_method_defined?(nil)
        }.should raise_error(TypeError)
        lambda {
          ModuleSpecs::CountsMixin.protected_method_defined?(false)
        }.should raise_error(TypeError)

        sym = mock('symbol')
        def sym.to_sym() :protected_3 end
        lambda {
          ModuleSpecs::CountsMixin.protected_method_defined?(sym)
        }.should raise_error(TypeError)
      end
    end

    ruby_version_is "1.9" do
      it "raises a TypeError if not passed a Symbol" do
        lambda {
          ModuleSpecs::CountsMixin.protected_method_defined?(1)
        }.should raise_error(TypeError)
        lambda {
          ModuleSpecs::CountsMixin.protected_method_defined?(nil)
        }.should raise_error(TypeError)
        lambda {
          ModuleSpecs::CountsMixin.protected_method_defined?(false)
        }.should raise_error(TypeError)

        sym = mock('symbol')
        def sym.to_sym() :protected_3 end
        lambda {
          ModuleSpecs::CountsMixin.protected_method_defined?(sym)
        }.should raise_error(TypeError)
      end
    end
  end

  it "accepts any object that is a String type" do
    str = mock('protected_3')
    def str.to_str() 'protected_3' end
    ModuleSpecs::CountsMixin.protected_method_defined?(str).should == true
  end

  deviates_on :rubinius do
    it "raises a TypeError if not passed a String type" do
      lambda {
        ModuleSpecs::CountsMixin.protected_method_defined?(mock('x'))
      }.should raise_error(TypeError)
    end
  end
end
