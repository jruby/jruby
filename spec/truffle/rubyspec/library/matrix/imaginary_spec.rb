require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/imaginary', __FILE__)

ruby_version_is "1.9" do
  describe "Matrix#imaginary" do
    it_behaves_like(:matrix_imaginary, :imaginary)
  end
end
