require File.expand_path('../../../shared/kernel/method_missing', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Kernel#method_missing" do
    it_behaves_like :method_missing, nil, Kernel
  end
end

describe "Kernel#method_missing" do
  it_behaves_like :method_missing_defined_module, nil, KernelSpecs::ModuleMM
end

describe "Kernel#method_missing" do
  it_behaves_like :method_missing_module, nil, KernelSpecs::ModuleNoMM
end

describe "Kernel#method_missing" do
  it_behaves_like :method_missing_defined_class, nil, KernelSpecs::ClassMM
end

describe "Kernel#method_missing" do
  it_behaves_like :method_missing_class, nil, KernelSpecs::ClassNoMM
end

describe "Kernel#method_missing" do
  it_behaves_like :method_missing_defined_instance, nil, KernelSpecs::ClassMM
end

describe "Kernel#method_missing" do
  it_behaves_like :method_missing_instance, nil, KernelSpecs::ClassNoMM
end
