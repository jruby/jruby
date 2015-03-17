require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../../../shared/process/exec', __FILE__)

describe "Kernel#exec" do
  it "is a private method" do
    Kernel.should have_private_instance_method(:exec)
  end

  it_behaves_like :process_exec, :exec, KernelSpecs::Method.new
end

describe "Kernel.exec" do
  it_behaves_like :process_exec, :exec, Kernel
end
