require File.expand_path('../../../shared/complex/abs', __FILE__)

ruby_version_is ""..."1.9" do

  require 'complex'

  describe "Complex#abs" do
    it_behaves_like(:complex_abs, :abs)
  end
end
