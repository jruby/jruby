require File.expand_path('../../../shared/kernel/method_missing', __FILE__)

describe "BasicObject#method_missing" do
  it "is a private method" do
    BasicObject.should have_private_instance_method(:method_missing)
  end
end

describe "BasicObject#method_missing" do
  it_behaves_like :method_missing_class, nil, BasicObject
end

describe "BasicObject#method_missing" do
  it_behaves_like :method_missing_instance, nil, BasicObject
end
