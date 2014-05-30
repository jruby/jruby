require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/quo', __FILE__)

ruby_version_is "1.9" do
  describe "Float#quo" do
    it_behaves_like :float_quo, :quo
  end
end
