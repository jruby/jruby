require File.expand_path('../../../../spec_helper', __FILE__)
require 'complex'
require File.expand_path('../shared/acosh', __FILE__)

describe "Math#acosh" do
  it_behaves_like :complex_math_acosh, :_, IncludesMath.new

  it "is a private instance method" do
    IncludesMath.should have_private_instance_method(:acosh)
  end
end

ruby_version_is ""..."1.9" do
  describe "Math#acosh!" do
    it_behaves_like :complex_math_acosh_bang, :_, IncludesMath.new

    it "is a private instance method" do
      IncludesMath.should have_private_instance_method(:acosh!)
    end
  end
end

describe "Math.acosh" do
  it_behaves_like :complex_math_acosh, :_, CMath
end

ruby_version_is ""..."1.9" do
  describe "Math.acosh!" do
    it_behaves_like :complex_math_acosh_bang, :_, CMath
  end
end
