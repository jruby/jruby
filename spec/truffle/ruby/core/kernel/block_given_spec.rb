require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe :kernel_block_given, :shared => true do
  it "returns true if and only if a block is supplied" do
    @object.accept_block {}.should_not == false
    @object.accept_block_as_argument {}.should_not == false
    @object.accept_block.should_not == true
    @object.accept_block_as_argument.should_not == true
  end

  # Clarify: Based on http://www.ruby-forum.com/topic/137822 it appears
  # that Matz wanted this to be true in 1.9.
  it "returns false when a method defined by define_method is called with a block" do
    @object.defined_block {}.should == false
  end
end

describe "Kernel#block_given?" do
  it_behaves_like :kernel_block_given, :block_given?, KernelSpecs::BlockGiven

  it "returns false outside of a method" do
    block_given?.should == false
  end

  it "is a private method" do
    Kernel.should have_private_instance_method(:block_given?)
  end
end

describe "Kernel.block_given?" do
  it_behaves_like :kernel_block_given, :block_given?, KernelSpecs::KernelBlockGiven
end

describe "self.send(:block_given?)" do
  ruby_version_is ""..."1.9" do
    it "always returns false" do
      KernelSpecs::SelfBlockGiven.accept_block {}.should == false
      KernelSpecs::SelfBlockGiven.accept_block_as_argument {}.should == false
      KernelSpecs::SelfBlockGiven.accept_block.should == false
      KernelSpecs::SelfBlockGiven.accept_block_as_argument.should == false
      KernelSpecs::SelfBlockGiven.defined_block {}.should == false
    end
  end

  ruby_version_is "1.9" do
    it_behaves_like :kernel_block_given, :block_given?, KernelSpecs::SelfBlockGiven
  end
end

describe "Kernel.block_given?" do
  it "needs to be reviewed for spec completeness"
end
