require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/complex/image', __FILE__)

require 'complex'

ruby_version_is ""..."2.2" do
  describe "Complex#image" do
    it_behaves_like(:complex_image, :image)
  end
end
