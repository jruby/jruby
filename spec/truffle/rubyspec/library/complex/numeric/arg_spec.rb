require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../../../shared/complex/numeric/arg', __FILE__)

ruby_version_is ""..."1.9" do

  require 'complex'
  require 'rational'

  describe "Numeric#arg" do
    it_behaves_like :numeric_arg, :arg
  end
end
