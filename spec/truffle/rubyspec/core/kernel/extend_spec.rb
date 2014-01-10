require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

module KernelSpecs::M
  def self.extend_object(o)
    ScratchPad << "extend_object"
    super
  end

  def self.extended(o)
    ScratchPad << "extended"
    super
  end

  def self.append_features(o)
    ScratchPad << "append_features"
    super
  end
end

describe "Kernel#extend" do
  before(:each) do
    ScratchPad.record []
  end

  it "calls extend_object on argument" do
    o = mock('o')
    o.extend KernelSpecs::M
    ScratchPad.recorded.include?("extend_object").should == true
  end

  it "does not calls append_features on arguments metaclass" do
    o = mock('o')
    o.extend KernelSpecs::M
    ScratchPad.recorded.include?("append_features").should == false
  end

  it "calls extended on argument" do
    o = mock('o')
    o.extend KernelSpecs::M
    ScratchPad.recorded.include?("extended").should == true
  end

  it "makes the class a kind_of? the argument" do
    class C
      extend KernelSpecs::M
    end
    (C.kind_of? KernelSpecs::M).should == true
  end

  ruby_version_is ""..."1.9" do
    it "raises a TypeError if self is frozen" do
      module KernelSpecs::Mod; end
      o = mock('o')
      o.freeze
      lambda { o.extend KernelSpecs::Mod }.should raise_error(TypeError)
    end
  end

  ruby_version_is "1.9" do
    it "raises a RuntimeError if self is frozen" do
      module KernelSpecs::Mod; end
      o = mock('o')
      o.freeze
      lambda { o.extend KernelSpecs::Mod }.should raise_error(RuntimeError)
    end
  end
end

describe "Kernel#extend" do
  it "needs to be reviewed for spec completeness"
end
