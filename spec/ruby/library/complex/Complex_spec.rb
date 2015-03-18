require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/complex/Complex', __FILE__)

ruby_version_is ""..."1.9" do
  require 'complex'

  describe "Complex()" do
    it_behaves_like :kernel_Complex, :Complex
  end
end
