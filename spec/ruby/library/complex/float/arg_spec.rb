require File.expand_path('../../../../shared/complex/float/arg', __FILE__)

ruby_version_is ""..."1.9" do
  require 'complex'

  describe "Float#arg" do
    it_behaves_like :float_arg, :arg
  end
end
