require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../fixtures/constants', __FILE__)

describe "Module#const_defined?" do
  it "returns true if the given Symbol names a constant defined in the receiver" do
    ConstantSpecs.const_defined?(:CS_CONST2).should == true
    ConstantSpecs.const_defined?(:ModuleA).should == true
    ConstantSpecs.const_defined?(:ClassA).should == true
    ConstantSpecs::ContainerA.const_defined?(:ChildA).should == true
  end

  ruby_version_is "1.9" do
    it "returns true if the constant is defined in the reciever's superclass" do
      # CS_CONST4 is defined in the superclass of ChildA
      ConstantSpecs::ContainerA::ChildA.const_defined?(:CS_CONST4).should be_true
    end

    it "returns true if the constant is defined in a mixed-in module of the reciever" do
      # CS_CONST10 is defined in a module included by ChildA
      ConstantSpecs::ContainerA::ChildA.const_defined?(:CS_CONST10).should be_true
    end

    it "returns true if the constant is defined in Object and the receiver is a module" do
      # CS_CONST1 is defined in Object
      ConstantSpecs::ModuleA.const_defined?(:CS_CONST1).should be_true
    end

    it "returns true if the constant is defined in Object and the receiver is a class that has Object among its ancestors" do
      # CS_CONST1 is defined in Object
      ConstantSpecs::ContainerA::ChildA.const_defined?(:CS_CONST1).should be_true
    end

    it "returns false if the constant is defined in the receiver's superclass and the inherit flag is false" do
      ConstantSpecs::ContainerA::ChildA.const_defined?(:CS_CONST4, false).should be_false
    end

    it "returns true if the constant is defined in the receiver's superclass and the inherit flag is true" do
      ConstantSpecs::ContainerA::ChildA.const_defined?(:CS_CONST4, true).should be_true
    end
  end

  it "returns true if the given String names a constant defined in the receiver" do
    ConstantSpecs.const_defined?("CS_CONST2").should == true
    ConstantSpecs.const_defined?("ModuleA").should == true
    ConstantSpecs.const_defined?("ClassA").should == true
    ConstantSpecs::ContainerA.const_defined?("ChildA").should == true
  end

  ruby_version_is ""..."1.9" do
    it "returns false if the constant is not defined in the receiver" do
      ConstantSpecs::ContainerA::ChildA.const_defined?(:CS_CONST4).should == false
      ConstantSpecs::ParentA.const_defined?(:CS_CONST12).should == false
    end
  end

  ruby_version_is "1.9" do
    it "returns false if the constant is not defined in the receiver, its superclass, or any included modules" do
      # The following constant isn't defined at all.
      ConstantSpecs::ContainerA::ChildA.const_defined?(:CS_CONST4726).should be_false
      # DETATCHED_CONSTANT is defined in ConstantSpecs::Detatched, which isn't
      # included by or inherited from ParentA
      ConstantSpecs::ParentA.const_defined?(:DETATCHED_CONSTANT).should be_false
    end
  end

  it "does not call #const_missing if the constant is not defined in the receiver" do
    ConstantSpecs::ClassA.should_not_receive(:const_missing)
    ConstantSpecs::ClassA.const_defined?(:CS_CONSTX).should == false
  end

  it "calls #to_str to convert the given name to a String" do
    name = mock("ClassA")
    name.should_receive(:to_str).and_return("ClassA")
    ConstantSpecs.const_defined?(name).should == true
  end

  it "special cases Object and checks it's included Modules" do
    Object.const_defined?(:CS_CONST10).should be_true
  end

  it "raises a NameError if the name does not start with a capital letter" do
    lambda { ConstantSpecs.const_defined? "name" }.should raise_error(NameError)
  end

  it "raises a NameError if the name starts with a non-alphabetic character" do
    lambda { ConstantSpecs.const_defined? "__CONSTX__" }.should raise_error(NameError)
    lambda { ConstantSpecs.const_defined? "@Name" }.should raise_error(NameError)
    lambda { ConstantSpecs.const_defined? "!Name" }.should raise_error(NameError)
    lambda { ConstantSpecs.const_defined? "::Name" }.should raise_error(NameError)
  end

  it "raises a NameError if the name contains non-alphabetic characters except '_'" do
    ConstantSpecs.const_defined?("CS_CONSTX").should == false
    lambda { ConstantSpecs.const_defined? "Name=" }.should raise_error(NameError)
    lambda { ConstantSpecs.const_defined? "Name?" }.should raise_error(NameError)
  end

  it "raises a TypeError if conversion to a String by calling #to_str fails" do
    name = mock('123')
    lambda { ConstantSpecs.const_defined? name }.should raise_error(TypeError)

    name.should_receive(:to_str).and_return(123)
    lambda { ConstantSpecs.const_defined? name }.should raise_error(TypeError)
  end
end
