require File.expand_path('../../../../spec_helper', __FILE__)
require 'complex'
require File.expand_path('../shared/cos', __FILE__)

describe "Math#cos" do
  it_behaves_like :complex_math_cos, :_, IncludesMath.new

  it "is a private instance method" do
    IncludesMath.should have_private_instance_method(:cos)
  end
end

ruby_version_is ""..."1.9" do
  describe "Math#cos!" do
    it_behaves_like :complex_math_cos_bang, :_, IncludesMath.new

    it "is a private instance method" do
      IncludesMath.should have_private_instance_method(:cos!)
    end
  end
end

describe "Math.cos" do
  it_behaves_like :complex_math_cos, :_, CMath
end

ruby_version_is ""..."1.9" do
  describe "Math.cos!" do
    it_behaves_like :complex_math_cos_bang, :_, CMath
  end
end
