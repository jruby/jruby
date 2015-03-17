require File.expand_path('../../../../spec_helper', __FILE__)
require 'complex'
require File.expand_path('../shared/log', __FILE__)

describe "Math#log" do
  it_behaves_like :complex_math_log, :_, IncludesMath.new

  it "is a private instance method" do
    IncludesMath.should have_private_instance_method(:log)
  end
end

ruby_version_is ""..."1.9" do
  describe "Math#log!" do
    it_behaves_like :complex_math_log_bang, :_, IncludesMath.new

    it "is a private instance method" do
      IncludesMath.should have_private_instance_method(:log!)
    end
  end
end

describe "Math.log" do
  it_behaves_like :complex_math_log, :_, CMath
end

ruby_version_is ""..."1.9" do
  describe "Math.log!" do
    it_behaves_like :complex_math_log_bang, :_, CMath
  end
end
