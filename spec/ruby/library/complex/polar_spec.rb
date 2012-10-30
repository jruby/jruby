require File.expand_path('../../../shared/complex/polar', __FILE__)

ruby_version_is ""..."1.9" do
  require 'complex'

  describe "Complex.polar" do
    it_behaves_like(:complex_polar_class, :polar)
  end

  describe "Complex#polar" do
    it_behaves_like(:complex_polar, :polar)
  end
end
