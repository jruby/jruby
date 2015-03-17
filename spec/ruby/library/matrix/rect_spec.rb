require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/rectangular', __FILE__)

ruby_version_is "1.9" do
  describe "Matrix#rect" do
    it_behaves_like(:matrix_rectangular, :rect)
  end
end
