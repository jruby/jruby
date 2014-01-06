require File.expand_path('../../../shared/kernel/method_missing', __FILE__)

ruby_version_is "1.9" do
  describe "BasicObject#method_missing" do
    it_behaves_like :method_missing, nil, BasicObject
  end

  describe "BasicObject#method_missing" do
    it_behaves_like :method_missing_class, nil, BasicObject
  end

  describe "BasicObject#method_missing" do
    it_behaves_like :method_missing_instance, nil, BasicObject
  end
end
