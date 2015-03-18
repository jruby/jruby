require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/dup_clone', __FILE__)

describe "Kernel#clone" do
  it_behaves_like :kernel_dup_clone, :clone

  before :each do
    ScratchPad.clear
    @obj = KernelSpecs::Duplicate.new 1, :a
  end

  it "calls #initialize_copy on the new instance" do
    clone = @obj.clone
    ScratchPad.recorded.should_not == @obj.object_id
    ScratchPad.recorded.should == clone.object_id
  end

  it "copies frozen state from the original" do
    o2 = @obj.clone
    @obj.freeze
    o3 = @obj.clone

    o2.frozen?.should == false
    o3.frozen?.should == true
  end

  it "copies instance variables" do
    clone = @obj.clone
    clone.one.should == 1
    clone.two.should == :a
  end

  it "copies singleton methods" do
    def @obj.special() :the_one end
    clone = @obj.clone
    clone.special.should == :the_one
  end

  it "copies modules included in the singleton class" do
    class << @obj
      include KernelSpecs::DuplicateM
    end

    clone = @obj.clone
    clone.repr.should == "KernelSpecs::Duplicate"
  end

  it "copies constants defined in the singleton class" do
    class << @obj
      CLONE = :clone
    end

    clone = @obj.clone
    class << clone
      CLONE.should == :clone
    end
  end

  it "raises TypeError when called on nil" do
    lambda {
      nil.clone
    }.should raise_error(TypeError)
  end
end
