require File.expand_path('../../../../spec_helper', __FILE__)
require 'complex'
require File.expand_path('../shared/tanh', __FILE__)

describe "Math#tanh" do
  it_behaves_like :complex_math_tanh, :_, IncludesMath.new

  it "is a private instance method" do
    IncludesMath.should have_private_instance_method(:tanh)
  end
end

ruby_version_is ""..."1.9" do
  describe "Math#tanh!" do
    it_behaves_like :complex_math_tanh_bang, :_, IncludesMath.new

    it "is a private instance method" do
      IncludesMath.should have_private_instance_method(:tanh!)
    end
  end
end

describe "Math.tanh" do
  it_behaves_like :complex_math_tanh, :_, CMath
end

ruby_version_is ""..."1.9" do
  describe "Math.tanh!" do
    it_behaves_like :complex_math_tanh_bang, :_, CMath
  end
end
