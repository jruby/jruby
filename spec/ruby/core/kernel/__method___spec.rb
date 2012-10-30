require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../fixtures/__method__', __FILE__)

# Can not, must not use a shared spec because #send influences
# __method__ on 1.8.7

describe "Kernel.__method__" do
  ruby_version_is '1.8.7' do
    it "returns the current method, even when aliased" do
      KernelSpecs::MethodTest.new.f.should == :f
    end

    it "returns the original name when aliased method" do
      KernelSpecs::MethodTest.new.g.should == :f
    end

    it "returns the caller from blocks too" do
      KernelSpecs::MethodTest.new.in_block.should == [:in_block, :in_block]
    end

    it "returns the caller from define_method too" do
      KernelSpecs::MethodTest.new.dm.should == :dm
    end

    it "returns the caller from block inside define_method too" do
      KernelSpecs::MethodTest.new.dm_block.should == [:dm_block, :dm_block]
    end

    it "returns method name even from eval" do
      KernelSpecs::MethodTest.new.from_eval.should == :from_eval
    end

    # crashes hard on 1.8.7-p174
    ruby_bug "unknown", "1.8.7.248" do
      it "returns nil when not called from a method" do
        __method__.should == nil
      end
    end
  end
end
