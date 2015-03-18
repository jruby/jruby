require File.expand_path('../../../shared/complex/image', __FILE__)

ruby_version_is ""..."1.9" do

  require 'complex'

  describe "Complex#imag" do
    it_behaves_like(:complex_image, :imag)
  end
end
