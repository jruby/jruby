require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/complex/arg', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Complex#angle" do
    it_behaves_like(:complex_arg, :angle)
  end
end
