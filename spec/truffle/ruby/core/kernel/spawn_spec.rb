require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../../../shared/process/spawn', __FILE__)

ruby_version_is "1.9" do
  describe "Kernel#spawn" do
    it "is a private method" do
      Kernel.should have_private_instance_method(:spawn)
    end

    it_behaves_like :process_spawn, :spawn, KernelSpecs::Method.new
  end

  describe "Kernel.spawn" do
    it_behaves_like :process_spawn, :spawn, Kernel
  end
end
