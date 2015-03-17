require 'complex'
require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../../../fixtures/math/common', __FILE__)
require File.expand_path('../../../../shared/math/atanh', __FILE__)
require File.expand_path('../shared/atanh', __FILE__)

describe "Math#atanh" do
  it_behaves_like :math_atanh_base, :atanh, IncludesMath.new
  it_behaves_like :complex_math_atanh_complex, :atanh, IncludesMath.new

  it_behaves_like :math_atanh_private, :atanh, IncludesMath.new
end

ruby_version_is ""..."1.9" do
  describe "Math#atanh!" do
    it_behaves_like :math_atanh_base, :atanh!, IncludesMath.new
    it_behaves_like :math_atanh_no_complex, :atanh!, IncludesMath.new
    it_behaves_like :complex_math_atanh_no_complex, :atanh!, IncludesMath.new

    it_behaves_like :math_atanh_private, :atanh!, IncludesMath.new
  end
end

describe "Math.atanh" do
  it_behaves_like :math_atanh_base, :atanh, CMath
  it_behaves_like :complex_math_atanh_complex, :atanh, CMath
end

ruby_version_is ""..."1.9" do
  describe "Math.atanh!" do
    it_behaves_like :math_atanh_base, :atanh!, CMath
    it_behaves_like :math_atanh_no_complex, :atanh!, CMath
    it_behaves_like :complex_math_atanh_no_complex, :atanh!, CMath
  end
end
