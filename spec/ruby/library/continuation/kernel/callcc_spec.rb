require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../../../fixtures/kernel/callcc', __FILE__)
require File.expand_path('../../../../shared/kernel/callcc', __FILE__)

with_feature :continuation_library do
  require 'continuation'

  describe "Kernel#callcc" do
    it_behaves_like :kernel_instance_callcc, :callcc

    it_behaves_like :kernel_callcc, :callcc, KernelSpecs::Method.new
  end

  describe "Kernel.callcc" do
    it_behaves_like :kernel_callcc, :callcc, Kernel
  end
end
